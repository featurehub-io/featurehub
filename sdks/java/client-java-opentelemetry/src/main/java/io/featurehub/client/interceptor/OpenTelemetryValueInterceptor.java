package io.featurehub.client.interceptor;

import io.featurehub.client.FeatureValueInterceptor;
import io.grpc.Context;

import javax.inject.Inject;

/**
 * OpenTelemetry is different in that it uses the gRPC Context object to store
 * context data.
 */
public class OpenTelemetryValueInterceptor implements FeatureValueInterceptor {
  public static final String FEATUREHUB_FEATURE_CONTEXT_PREFIX = "fhub.";

  @Inject
  public OpenTelemetryValueInterceptor() {
  }

  @Override
  public ValueMatch getValue(String key) {
    final Context.Key<String> val = Context.key(FEATUREHUB_FEATURE_CONTEXT_PREFIX + key.replace(":",
      "_"));

    return null;
  }
}
