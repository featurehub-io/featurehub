package io.featurehub.client;

import io.opentracing.Span;

import static io.featurehub.client.interceptor.OpenTracingValueInterceptor.FEATUREHUB_OPENTRACING_BAGGAGE_PREFIX;

/**
 * A set of utilities allowing injection of feature states into the current OpenTracing span that
 * something down the line can pick up.
 */
public class OpenTracingFeatureStateInjector {
  /**
   * A simple method for keeping things consistent.
   *
   * @param key - the name of the feature
   * @param value - the value of the feature
   * @param span - the span into which to inject
   */
  public static void injectFeatureState(String key, String value, Span span) {
    span.setBaggageItem(FEATUREHUB_OPENTRACING_BAGGAGE_PREFIX + key, value);
  }
}
