package io.featurehub.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.SSEResultState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ClientFeatureRepository implements FeatureRepository {
  private static final Logger log = LoggerFactory.getLogger(ClientFeatureRepository.class);
  // feature-key, feature-state
  private final Map<String, FeatureStateBaseHolder> features = new ConcurrentHashMap<>();
  private final Executor executor;
  private final ObjectMapper mapper;
  private final TypeReference<List<FeatureState>> FEATURE_LIST_TYPEDEF
    = new TypeReference<List<FeatureState>>() {};
  private boolean hasReceivedInitialState = false;
  private final List<AnalyticsCollector> analyticsCollectors = new ArrayList<>();
  private Readyness readyness = Readyness.NotReady;
  private final List<ReadynessListener> readynessListeners = new ArrayList<>();
  private final List<FeatureValueInterceptorHolder> featureValueInterceptors = new ArrayList<>();
  private ObjectMapper jsonConfigObjectMapper;
  private final ClientContext clientContext;

  public ClientFeatureRepository(int threadPoolSize) {
    mapper = initializeMapper();
    jsonConfigObjectMapper = mapper;
    executor = getExecutor(threadPoolSize);
    this.clientContext = new ClientContextRepository(executor);
  }

  public ClientFeatureRepository() {
    this(1);
  }

  public ClientFeatureRepository(Executor executor) {
    mapper = initializeMapper();
    jsonConfigObjectMapper = mapper;
    this.executor = executor;
    this.clientContext = new ClientContextRepository(executor);
  }

  protected ObjectMapper initializeMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    return mapper;
  }

  protected Executor getExecutor(int threadPoolSize) {
    return Executors.newFixedThreadPool(threadPoolSize);
  }

  public void setJsonConfigObjectMapper(ObjectMapper jsonConfigObjectMapper) {
    this.jsonConfigObjectMapper = jsonConfigObjectMapper;
  }

  @Override
  public ClientContext clientContext() {
    return clientContext;
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
  public FeatureRepository registerValueInterceptor(boolean allowFeatureOverride, FeatureValueInterceptor interceptor) {
    featureValueInterceptors.add(new FeatureValueInterceptorHolder(allowFeatureOverride, interceptor));

    return this;
  }

  @Override
  public void notify(SSEResultState state, String data) {
    log.trace("received state {} data {}", state, data);
    if (state == null) {
      log.warn("Unexpected state {}", state);
    } else {
      try {
        switch (state) {
          case ACK:
            break;
          case BYE:
            break;
          case DELETE_FEATURE:
            deleteFeature(mapper.readValue(data, FeatureState.class));
            break;
          case FEATURE:
            featureUpdate(mapper.readValue(data, FeatureState.class));
            break;
          case FEATURES:
            List<FeatureState> features = mapper.readValue(data, FEATURE_LIST_TYPEDEF);
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
  public void notify(List<FeatureState> states, boolean force) {
    states.forEach(s -> featureUpdate(s, force));

    if (!hasReceivedInitialState) {
      checkForInvalidFeatures();
      hasReceivedInitialState = true;
      readyness = Readyness.Ready;
      broadcastReadyness();
    }
  }

  @Override
  public void notify(List<FeatureState> states) {
    notify(states, false);
  }


  @Override
  public FeatureRepository addReadynessListener(ReadynessListener rl) {
    this.readynessListeners.add(rl);

    // let it know what the current state is
    executor.execute(() -> rl.notify(readyness));

    return this;
  }

  private void broadcastReadyness() {
    readynessListeners.forEach((rl) -> {
      executor.execute(() -> rl.notify(readyness));
    });
  }


  private void deleteFeature(FeatureState readValue) {
    readValue.setValue(null);
    featureUpdate(readValue);
  }

  private void checkForInvalidFeatures() {
    String invalidKeys =
      features.values().stream().filter(v -> v.getKey() == null)
        .map(FeatureStateHolder::getKey).collect(Collectors.joining(", "));
    if (invalidKeys.length() > 0) {
      log.error("FeatureHub error: application is requesting use of invalid keys: {}", invalidKeys);
    }
  }

  @Override
  public FeatureStateHolder getFeatureState(String key) {
    return features.computeIfAbsent(key, key1 -> {
      if (hasReceivedInitialState) {
        log.error("FeatureHub error: application requesting use of invalid key after initialization: `{}`", key1);
      }

      return new FeatureStatePlaceHolder(executor, featureValueInterceptors, key, mapper);
    });
  }

  @Override
  public FeatureStateHolder getFeatureState(Feature feature) {
    return this.getFeatureState(feature.name());
  }

  @Override
  public FeatureStateHolder getFeatureState(String key, ClientContext ctx) {
    return null;
  }

  @Override
  public FeatureStateHolder getFeatureState(Feature feature, ClientContext ctx) {
    return null;
  }

  @Override
  public boolean getFlag(Feature feature) {
    return getFlag(feature.name());
  }

  @Override
  public boolean getFlag(String key, ClientContext ctx) {
    return false;
  }

  @Override
  public boolean getFlag(Feature feature, ClientContext ctx) {
    return false;
  }

  @Override
  public boolean getFlag(String key) {
    return getFeatureState(key).getBoolean() == Boolean.TRUE;
  }

  @Override
  public String getString(Feature feature) {
    return getString(feature.name());
  }

  @Override
  public String getString(String key, ClientContext ctx) {
    return null;
  }

  @Override
  public String getString(Feature feature, ClientContext ctx) {
    return null;
  }

  @Override
  public String getString(String key) {
    return getFeatureState(key).getString();
  }

  @Override
  public BigDecimal getNumber(String key) {
    return getFeatureState(key).getNumber();
  }

  @Override
  public BigDecimal getNumber(Feature feature) {
    return getNumber(feature.name());
  }

  @Override
  public BigDecimal getNumber(String key, ClientContext ctx) {
    return null;
  }

  @Override
  public BigDecimal getNumber(Feature feature, ClientContext ctx) {
    return null;
  }

  @Override
  public <T> T getJson(String key, Class<T> type) {
    return getFeatureState(key).getJson(type);
  }

  @Override
  public <T> T getJson(Feature feature, Class<T> type) {
    return getJson(feature.name(), type);
  }

  @Override
  public <T> T getJson(String key, Class<T> type, ClientContext ctx) {
    return null;
  }

  @Override
  public <T> T getJson(Feature feature, Class<T> type, ClientContext ctx) {
    return null;
  }

  @Override
  public String getRawJson(String key) {
    return getFeatureState(key).getRawJson();
  }

  @Override
  public String getRawJson(Feature feature) {
    return getRawJson(feature.name());
  }

  @Override
  public String getRawJson(String key, ClientContext ctx) {
    return null;
  }

  @Override
  public String getRawJson(Feature feature, ClientContext ctx) {
    return null;
  }

  @Override
  public boolean isSet(String key) {
    return getFeatureState(key).isSet();
  }

  @Override
  public boolean isSet(Feature feature) {
    return isSet(feature.name());
  }

  @Override
  public boolean isSet(String key, ClientContext ctx) {
    return false;
  }

  @Override
  public boolean isSet(Feature feature, ClientContext ctx) {
    return false;
  }

  @Override
  public boolean exists(String key) {
    return !(getFeatureState(key) instanceof FeatureStatePlaceHolder);
  }

  @Override
  public boolean exists(Feature feature) {
    return exists(feature.name());
  }

  @Override
  public boolean exists(String key, ClientContext ctx) {
    return false;
  }

  @Override
  public boolean exists(Feature feature, ClientContext ctx) {
    return false;
  }

  @Override
  public FeatureRepository logAnalyticsEvent(String action, Map<String, String> other) {
    // take a snapshot of the current state of the features
    List<FeatureStateHolder> featureStateAtCurrentTime = features.values().stream()
      .filter(FeatureStateBaseHolder::isSet)
      .map(FeatureStateBaseHolder::copy)
      .collect(Collectors.toList());

    executor.execute(() -> {
      analyticsCollectors.forEach((c) -> {
        c.logEvent(action, other, featureStateAtCurrentTime);
      });
    });

    return this;
  }

  private void featureUpdate(FeatureState featureState) {
    featureUpdate(featureState, false);
  }

  private void featureUpdate(FeatureState featureState, boolean force) {
    FeatureStateBaseHolder holder = features.get(featureState.getKey());
    if (holder == null || holder.getKey() == null) {
      switch (featureState.getType()) {
        case BOOLEAN:
          holder = new FeatureStateBooleanHolder(holder, executor, featureValueInterceptors, featureState.getKey());
          break;
        case NUMBER:
          holder = new FeatureStateNumberHolder(holder, executor, featureValueInterceptors, featureState.getKey());
          break;
        case STRING:
          holder = new FeatureStateStringHolder(holder, executor, featureValueInterceptors, featureState.getKey());
          break;
        case JSON:
          holder = new FeatureStateJsonHolder(holder, executor, jsonConfigObjectMapper, featureValueInterceptors,
            featureState.getKey());
          break;
      }

      features.put(featureState.getKey(), holder);
    } else if (!force && holder.featureState != null) {
      if (holder.featureState.getVersion() > featureState.getVersion() ||
        (
        holder.featureState.getVersion().equals(featureState.getVersion()) &&
        !FeatureStateUtils.changed(holder.featureState.getValue(), featureState.getValue()))) {
        // if the old version is newer, or they are the same version and the value hasn't changed.
        // it can change with server side evaluation based on user data
        return;
      }
    }


    holder.setFeatureState(featureState);
  }

}
