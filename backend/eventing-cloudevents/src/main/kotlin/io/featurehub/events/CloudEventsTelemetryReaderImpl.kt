package io.featurehub.events

import io.cloudevents.CloudEvent
import io.featurehub.metrics.MetricsCollector
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

internal class CloudEventTextMapGetter : TextMapGetter<CloudEvent> {
  override fun keys(carrier: CloudEvent): MutableIterable<String> =
    listOf(carrier.attributeNames, carrier.extensionNames).flatten().toMutableSet()

  override fun get(carrier: CloudEvent?, key: String): String? {
    carrier?.let {
      if (it.specVersion.allAttributes.contains(key)) {
        return carrier.getAttribute(key)?.toString()
      } else if (it.extensionNames.contains(key)) {
        return carrier.getExtension(key)?.toString()
      }
    }

    return null
  }
}

interface CloudEventsTelemetryReader {
  /*
   * This one is not used
   */
  fun receive(subject: String, event: CloudEvent, metrics: CloudEventChannelMetric, process: (event: CloudEvent) -> Unit)
  /* this is primarily used */
  fun receive(event: CloudEvent, process: (event: CloudEvent) -> Unit)
}

class CloudEventsTelemetryReaderImpl @Inject constructor(private val openTelemetry: OpenTelemetry,
                                                         private val tracer: Tracer
) : CloudEventsTelemetryReader {
  private val telemetryGetter = CloudEventTextMapGetter()

  private val failedEvent = MetricsCollector.counter("cloudevent_failures", "General cloud event failures")
  private val log: Logger = LoggerFactory.getLogger(CloudEventsTelemetryReaderImpl::class.java)

  override fun receive(subject: String, event: CloudEvent, metrics: CloudEventChannelMetric, process: (event: CloudEvent) -> Unit) {
    val extractedContext =
      openTelemetry.propagators.textMapPropagator.extract(Context.current(), event, telemetryGetter)

    extractedContext.makeCurrent().use { scope ->
      val span = tracer.spanBuilder(subject)
        .setSpanKind(SpanKind.CONSUMER)
        .startSpan()

      setMDCFromBaggage()

      try {
        process(event)
      } catch (e: Exception) {
        metrics.failures.inc()
        throw e
      } finally {
        MDC.clear()
        span.end()
      }
    }
  }

  private fun setMDCFromBaggage() {
    val baggage = Baggage.fromContext(Context.current())

    if (baggage.size() > 0) {
      baggage.forEach { k, v -> MDC.put(k, v.value) }
    }

  }

  override fun receive(event: CloudEvent, process: (event: CloudEvent) -> Unit) {
    val extractedContext =
      openTelemetry.propagators.textMapPropagator.extract(Context.current(), event, telemetryGetter)

    extractedContext.makeCurrent().use {
      val span = tracer.spanBuilder(event.subject ?: "generic-cloud-event")
        .setSpanKind(SpanKind.CONSUMER)
        .startSpan()

      setMDCFromBaggage()

      try {
        process(event)
      } catch (e: Exception) {
        failedEvent.inc()
        log.error("failed to process event {}", event, e)
      } finally {
        MDC.clear()
        span.end()
      }
    }
  }
}
