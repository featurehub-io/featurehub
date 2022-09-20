package io.featurehub.events

import io.cloudevents.core.builder.CloudEventBuilder
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class CloudEventChannelMetric(val failures: Counter, val perf: Histogram)

interface CloudEventsTelemetryWriter {
  fun publish(subject: String, builder: CloudEventBuilder, metrics: CloudEventChannelMetric,
              broadcastWriter: (event: CloudEventBuilder) -> Unit)
}

internal class CloudEventsTextMapSetter : TextMapSetter<CloudEventBuilder> {
  override fun set(carrier: CloudEventBuilder?, key: String, value: String) {
    carrier?.withContextAttribute(key, value)
  }
}

class CloudEventsTelemetryWriterImpl @Inject constructor(private val openTelemetry: OpenTelemetry,
                                                         private val tracer: Tracer
) : CloudEventsTelemetryWriter {
  private val log: Logger = LoggerFactory.getLogger(CloudEventsTelemetryWriterImpl::class.java)
  private val telemetrySetter = CloudEventsTextMapSetter()

  override fun publish(subject: String, builder: CloudEventBuilder, metrics: CloudEventChannelMetric,
                       broadcastWriter: (event: CloudEventBuilder) -> Unit) {
    val span = tracer.spanBuilder(subject)
      .setSpanKind(SpanKind.PRODUCER)
      .startSpan()

    val timer = metrics.perf.startTimer()
    try {
      span.makeCurrent().use {
        try {
          openTelemetry.propagators.textMapPropagator.inject(Context.current(), builder, telemetrySetter)

          broadcastWriter(builder)

          span.setStatus(StatusCode.OK)
        } catch (e: Exception) {
          log.error("Failed to send message", e)
          span.setStatus(StatusCode.ERROR, e.message ?: "failed to send message")
        } finally {
          span.end()
        }
      }
    } catch (e: Exception) {
      log.error("Unable to publish {}", subject, e)
    } finally {
      timer.observeDuration()
    }
  }
}

