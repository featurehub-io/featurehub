package io.featurehub.client;

import java.util.List;
import java.util.Map;

public interface EdgeService {
  /**
   * The context attributes have changed, notify edge service in case it uses them.
   *
   * @param attributes
   */
  void contextChange(Map<String, List<String>> attributes);

  /**
   * are we doing client side evaluation?
   * @return
   */
  boolean isClientEvaluation();

  /**
   * Shut down this service
   */
  void close();
}
