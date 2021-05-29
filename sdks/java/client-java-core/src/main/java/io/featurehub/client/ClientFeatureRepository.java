package io.featurehub.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.featurehub.sse.model.RolloutStrategy;
import io.featurehub.sse.model.SSEResultState;
import io.featurehub.strategies.matchers.MatcherRegistry;
import io.featurehub.strategies.percentage.PercentageMumurCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ClientFeatureRepository extends AbstractFeatureRepository
    implements FeatureRepositoryContext {
  private static final Logger log = LoggerFactory.getLogger(ClientFeatureRepository.class);
  // feature-key, feature-state
  private final Map<String, FeatureStateBase> features = new ConcurrentHashMap<>();
  private final ExecutorService executor;
  private final ObjectMapper mapper;
  private boolean hasReceivedInitialState = false;
  private final List<AnalyticsCollector> analyticsCollectors = new ArrayList<>();
  private Readyness readyness = Readyness.NotReady;
  private final List<ReadynessListener> readynessListeners = new ArrayList<>();
  private final List<FeatureValueInterceptorHolder> featureValueInterceptors = new ArrayList<>();
  private ObjectMapper jsonConfigObjectMapper;
  private final ApplyFeature applyFeature;
  private boolean serverEvaluation = false; // the client tells us, we pass it out to others

  private final TypeReference<List<io.featurehub.sse.model.FeatureState>> FEATURE_LIST_TYPEDEF =
      new TypeReference<List<io.featurehub.sse.model.FeatureState>>() {};

  public ClientFeatureRepository(ExecutorService executor, ApplyFeature applyFeature) {
    mapper = initializeMapper();

    jsonConfigObjectMapper = mapper;

    this.executor = executor;

    this.applyFeature =
        applyFeature == null
            ? new ApplyFeature(new PercentageMumurCalculator(), new MatcherRegistry())
            : applyFeature;
  }

  public ClientFeatureRepository(int threadPoolSize) {
    this(getExecutor(threadPoolSize), null);
  }

  public ClientFeatureRepository() {
    this(1);
  }

  public ClientFeatureRepository(ExecutorService executor) {
    this(executor == null ? getExecutor(1) : executor, null);
  }

  protected ObjectMapper initializeMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    return mapper;
  }

  protected static ExecutorService getExecutor(int threadPoolSize) {
    return Executors.newFixedThreadPool(threadPoolSize);
  }

  public void setJsonConfigObjectMapper(ObjectMapper jsonConfigObjectMapper) {
    this.jsonConfigObjectMapper = jsonConfigObjectMapper;
  }

  @Override
  public boolean exists(String key) {
    return features.containsKey(key);
  }

  @Override
  public boolean isServerEvaluation() {
    return serverEvaluation;
  }

  public Readyness getReadyness() {
    return readyness;
  }

  @Override
  public FeatureRepository addAnalyticCollector(AnalyticsCollector collector) {
    analyticsCollectors.add(collector);
    return this;
  }

  @Override
  public FeatureRepository registerValueInterceptor(
      boolean allowFeatureOverride, FeatureValueInterceptor interceptor) {
    featureValueInterceptors.add(
        new FeatureValueInterceptorHolder(allowFeatureOverride, interceptor));

    return this;
  }

  @Override
  public void notify(SSEResultState state, String data) {
    log.trace("received state {} data {}", state, data);
    if (state == null) {
      log.warn("Unexpected null state");
    } else {
      try {
        switch (state) {
          case ACK:
          case BYE:
            break;
          case DELETE_FEATURE:
            deleteFeature(mapper.readValue(data, io.featurehub.sse.model.FeatureState.class));
            break;
          case FEATURE:
            featureUpdate(mapper.readValue(data, io.featurehub.sse.model.FeatureState.class));
            break;
          case FEATURES:
            List<io.featurehub.sse.model.FeatureState> features =
                mapper.readValue(data, FEATURE_LIST_TYPEDEF);
            notify(features);
            break;
          case FAILURE:
            readyness = Readyness.Failed;
            broadcastReadyness();
            break;
        }
      } catch (Exception e) {
        log.error("Unable to process data `{}` for state `{}`", data, state, e);
      }
    }
  }

  @Override
  public void notify(List<io.featurehub.sse.model.FeatureState> states, boolean force) {
    states.forEach(s -> featureUpdate(s, force));

    if (!hasReceivedInitialState) {
      checkForInvalidFeatures();
      hasReceivedInitialState = true;
      readyness = Readyness.Ready;
      broadcastReadyness();
    } else if (readyness != Readyness.Ready) {
      readyness = Readyness.Ready;
      broadcastReadyness();
    }
  }

  @Override
  public List<FeatureValueInterceptorHolder> getFeatureValueInterceptors() {
    return featureValueInterceptors;
  }

  @Override
  public Applied applyFeature(
      List<RolloutStrategy> strategies, String key, String featureValueId, ClientContext cac) {
    return applyFeature.applyFeature(strategies, key, featureValueId, cac);
  }

  @Override
  public void execute(Runnable command) {
    executor.execute(command);
  }

  @Override
  public ObjectMapper getJsonObjectMapper() {
    return jsonConfigObjectMapper;
  }

  @Override
  public void setServerEvaluation(boolean val) {
    this.serverEvaluation = val;
  }

  @Override
  public void notReady() {
    readyness = Readyness.NotReady;
    broadcastReadyness();
  }

  @Override
  public void close() {
    log.info("featurehub repository closing");
    features.clear();

    readyness = Readyness.NotReady;
    readynessListeners.stream().forEach(rl -> rl.notify(readyness));

    executor.shutdownNow();

    log.info("featurehub repository closed");
  }

  @Override
  public void notify(List<io.featurehub.sse.model.FeatureState> states) {
    notify(states, false);
  }

  @Override
  public FeatureRepository addReadynessListener(ReadynessListener rl) {
    this.readynessListeners.add(rl);

    if (!executor.isShutdown()) {
      // let it know what the current state is
      executor.execute(() -> rl.notify(readyness));
    }

    return this;
  }

  private void broadcastReadyness() {
    if (!executor.isShutdown()) {
      readynessListeners.forEach((rl) -> executor.execute(() -> rl.notify(readyness)));
    }
  }

  private void deleteFeature(io.featurehub.sse.model.FeatureState readValue) {
    readValue.setValue(null);
    featureUpdate(readValue);
  }

  private void checkForInvalidFeatures() {
    String invalidKeys =
        features.values().stream()
            .filter(v -> v.getKey() == null)
            .map(FeatureState::getKey)
            .collect(Collectors.joining(", "));
    if (invalidKeys.length() > 0) {
      log.error("FeatureHub error: application is requesting use of invalid keys: {}", invalidKeys);
    }
  }

  @Override
  public FeatureState getFeatureState(String key) {
    return features.computeIfAbsent(
        key,
        key1 -> {
          if (hasReceivedInitialState) {
            log.error(
                "FeatureHub error: application requesting use of invalid key after initialization: `{}`",
                key1);
          }

          return new FeatureStateBase(null, this, key);
        });
  }

  @Override
  public FeatureRepository logAnalyticsEvent(
      String action, Map<String, String> other, ClientContext ctx) {
    // take a snapshot of the current state of the features
    List<FeatureState> featureStateAtCurrentTime =
        features.values().stream()
            .map(f -> ctx == null ? f : f.withContext(ctx))
            .filter(FeatureState::isSet)
            .map(f -> ((FeatureStateBase)f).analyticsCopy())
            .collect(Collectors.toList());

    executor.execute(
        () ->
            analyticsCollectors.forEach(
                (c) -> c.logEvent(action, other, featureStateAtCurrentTime)));

    return this;
  }

  private void featureUpdate(io.featurehub.sse.model.FeatureState featureState) {
    featureUpdate(featureState, false);
  }

  private void featureUpdate(io.featurehub.sse.model.FeatureState featureState, boolean force) {
    FeatureStateBase holder = features.get(featureState.getKey());
    if (holder == null || holder.getKey() == null) {
      holder = new FeatureStateBase(holder, this, featureState.getKey());

      features.put(featureState.getKey(), holder);
    } else if (!force && holder._featureState != null) {
      if (holder._featureState.getVersion() > featureState.getVersion()
          || (holder._featureState.getVersion().equals(featureState.getVersion())
              && !FeatureStateUtils.changed(
                  holder._featureState.getValue(), featureState.getValue()))) {
        // if the old version is newer, or they are the same version and the value hasn't changed.
        // it can change with server side evaluation based on user data
        return;
      }
    }

    holder.setFeatureState(featureState);
  }
}
