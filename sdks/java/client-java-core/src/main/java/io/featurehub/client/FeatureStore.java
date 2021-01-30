package io.featurehub.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.RolloutStrategy;
import io.featurehub.sse.model.SSEResultState;

import java.util.List;

/**
 * This interface is only designed for use internally, but we won't hide it in case someone finds a
 * particular need elsewhere.
 */
public interface FeatureStore {
  /*
   * Any incoming state changes from a multi-varied set of possible data. This comes
   * from SSE.
   */
  void notify(SSEResultState state, String data);

  /**
   * Indicate the feature states have updated and if their versions have
   * updated or no versions exist, update the repository.
   *
   * @param states - the features
   */
  void notify(List<FeatureState> states);


  /**
   * Update the feature states and force them to be updated, ignoring their version numbers.
   * This still may not cause events to be triggered as event triggers are done on actual value changes.
   *
   * @param states - the list of feature states
   * @param force  - whether we should force the states to change
   */
  void notify(List<FeatureState> states, boolean force);

  List<FeatureValueInterceptorHolder> getFeatureValueInterceptors();

  Applied applyFeature(List<RolloutStrategy> strategies, String key, String featureValueId,
                       ClientContext cac);

  void execute(Runnable command);

  ObjectMapper getJsonObjectMapper();
}
