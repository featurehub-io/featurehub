package io.featurehub.client.interceptor;

import io.featurehub.client.FeatureValueInterceptor;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenTracingValueInterceptor implements FeatureValueInterceptor {
  private static final Logger log = LoggerFactory.getLogger(OpenTracingValueInterceptor.class);
  public static final String FEATUREHUB_OPENTRACING_ENABLED = "featurehub.opentracing-enabled";
  public static final String FEATUREHUB_OPENTRACING_BAGGAGE_PREFIX = "fhub.";

  private final boolean enabled;
  private Tracer tracer;

  public OpenTracingValueInterceptor() {
    enabled = System.getProperties().containsKey(FEATUREHUB_OPENTRACING_ENABLED);
  }

  public void setTracer(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public ValueMatch getValue(String key) {
    if (enabled) {
      Tracer tracer = this.tracer == null ? (GlobalTracer.isRegistered() ? GlobalTracer.get() : null) : this.tracer;

      if (tracer != null) {
        final Span span = tracer.activeSpan();

        if (span != null) {
          String val = span.getBaggageItem(FEATUREHUB_OPENTRACING_BAGGAGE_PREFIX + key.toLowerCase());

          return new ValueMatch(val != null, val);
        }
      } else {
        log.error("OpenTracing enabled for feature hub, but no global tracer or tracer specified.");
      }
    }

    return null;
  }
}
