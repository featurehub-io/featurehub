package io.featurehub.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.function.Supplier;

public interface FeatureHubConfig {
  /**
   * What is the fully deconstructed URL for the server?
   */
  String getRealtimeUrl();

  String apiKey();

  String baseUrl();

  // start the service
  void init();

  /**
   * The API Key indicates this is going to be server based evaluation
   */
  boolean isServerEvaluation();

  /**
   * returns a new context using the default edge provider and repository
   *
   * @return a new context
   */
  ClientContext newContext();

  /**
   * Allows you to create a new context for the user.
   *
   * @param repository - this repository is for this call only, it is not remembered, you should set the repository
   *                   on repository() to make it the default.
   *
   * @param edgeService - this edgeService is for this call only, it is not remembered, you should set it on
   *                    edgeService() to make it the default
   * @return a new context
   */
  ClientContext newContext(FeatureRepositoryContext repository, Supplier<EdgeService> edgeService);

  static boolean sdkKeyIsClientSideEvaluated(String sdkKey) {
    return sdkKey.contains("*");
  }

  void setRepository(FeatureRepositoryContext repository);
  FeatureRepositoryContext getRepository();

  void setEdgeService(Supplier<EdgeService> edgeService);
  Supplier<EdgeService> getEdgeService();

  /**
   * Allows you to specify a readyness listener to trigger every time the repository goes from
   * being in any way not reaay, to ready.
   * @param readynessListener
   */
  void addReadynessListener(ReadynessListener readynessListener);

  /**
   * Allows you to specify an analytics collector
   *
   * @param collector
   */
  void addAnalyticCollector(AnalyticsCollector collector);

  /**
   * Allows you to register a value interceptor
   * @param allowLockOverride
   * @param interceptor
   */
  void registerValueInterceptor(boolean allowLockOverride, FeatureValueInterceptor interceptor);

  /**
   * Allows you to query the state of the repository's readyness - such as in a heartbeat API
   * @return
   */
  Readyness getReadyness();

  /**
   * Allows you to override how your config will be deserialized when "getJson" is called.
   *
   * @param jsonConfigObjectMapper - a Jackson ObjectMapper
   */
  void setJsonConfigObjectMapper(ObjectMapper jsonConfigObjectMapper);
}
