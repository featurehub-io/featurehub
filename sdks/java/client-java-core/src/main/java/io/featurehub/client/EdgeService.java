package io.featurehub.client;

import java.util.List;
import java.util.Map;

public interface EdgeService {
  /**
   * called only when the new attribute header has changed
   *
   * @param newHeader
   */
  void contextChange(String newHeader);

  /**
   * are we doing client side evaluation?
   * @return
   */
  boolean isClientEvaluation();

  /**
   * Shut down this service
   */
  void close();

  FeatureHubConfig getConfig();

  boolean isRequiresReplacementOnHeaderChange();

  void poll();
}
