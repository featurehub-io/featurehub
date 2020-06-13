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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ClientFeatureRepository {
  private static final Logger log = LoggerFactory.getLogger(ClientFeatureRepository.class);
  // feature-key, feature-state
  private final Map<String, FeatureStateBaseHolder> features = new ConcurrentHashMap<>();
  private final Executor executor;
  private final ObjectMapper mapper;
  private final TypeReference<List<FeatureState>> FEATURE_LIST_TYPEDEF = new TypeReference<>() {
  };
  private boolean hasReceivedInitialState = false;
  private List<AnalyticsCollector> analyticsCollectors = new ArrayList<>();
  private Readyness readyness = Readyness.NotReady;
  private List<ReadynessListener> readynessListeners = new ArrayList<>();

  public ClientFeatureRepository(int threadPoolSize) {
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    executor = Executors.newFixedThreadPool(threadPoolSize);
  }

  public void addAnalyticCollector(AnalyticsCollector collector) {
    analyticsCollectors.add(collector);
  }

  public void notify(SSEResultState state, String data) {
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
            features.forEach(this::featureUpdate);
            if (!hasReceivedInitialState) {
              checkForInvalidFeatures();
              hasReceivedInitialState = true;
              readyness = Readyness.Ready;
              broadcastReadyness();
            }
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

  public void addReadynessListener(ReadynessListener rl) {
    this.readynessListeners.add(rl);

    // let it know what the current state is
    executor.execute(() -> rl.notify(readyness));
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
    String invalidKeys = features.values().stream().filter(v -> v.getKey() == null).map(FeatureStateHolder::getKey).collect(Collectors.joining(", "));
    if (invalidKeys.length() > 0) {
      log.error("FeatureHub error: application is requesting use of invalid keys: {}", invalidKeys);
    }
  }

  public FeatureStateHolder getFeatureState(String key) {
    return features.computeIfAbsent(key, key1 -> {
      if (hasReceivedInitialState) {
        log.error("FeatureHub error: application requesting use of invalid key after initialization: `{}`", key1);
      }

      return new FeatureStatePlaceHolder(executor);
    });
  }

  public void logAnalyticsEvent(String action, Map<String, String> other) {
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

  }

  private void featureUpdate(FeatureState featureState) {
    FeatureStateBaseHolder holder = features.get(featureState.getKey());
    if (holder == null || holder.getKey() == null) {
      switch (featureState.getType()) {
        case BOOLEAN:
          holder = new FeatureStateBooleanHolder(holder, executor);
          break;
        case NUMBER:
          holder = new FeatureStateNumberHolder(holder, executor);
          break;
        case STRING:
          holder = new FeatureStateStringHolder(holder, executor);
          break;
        case JSON:
          holder = new FeatureStateJsonHolder(holder, executor, mapper);
          break;
      }

      features.put(featureState.getKey(), holder);
    }

    holder.setFeatureState(featureState);
  }
}
