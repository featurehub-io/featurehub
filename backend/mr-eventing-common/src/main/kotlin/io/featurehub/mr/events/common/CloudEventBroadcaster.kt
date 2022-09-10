package io.featurehub.mr.events.common

import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.jersey.config.CacheJsonMapper
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

interface CloudEventBroadcasterWriter {
  fun encodePureJson(): Boolean
  fun publish(event: CloudEvent)
}

internal class CloudEventsTextMapSetter : TextMapSetter<CloudEventBuilder> {
  override fun set(carrier: CloudEventBuilder?, key: String, value: String) {
    carrier?.withContextAttribute(key, value)
  }
}

class CloudEventBroadcaster @Inject constructor(
  private val openTelemetry: OpenTelemetry,
  private val tracer: Tracer,
  private val broadcastWriter: CloudEventBroadcasterWriter
) : CacheBroadcast {
  private val log: Logger = LoggerFactory.getLogger(CloudEventBroadcaster::class.java)

  private val telemetrySetter = CloudEventsTextMapSetter()

  private var environmentSubject: String? = "featurehub/environment"
  private var serviceAccountSubject: String? = "featurehub/service-account"
  private var featureSubject: String? = "featurehub/feature"

  init {
    DeclaredConfigResolver.resolve(this)
  }

  private fun action(action: PublishAction): String {
    return when (action) {
      PublishAction.CREATE -> "create"
      PublishAction.UPDATE -> "update"
      PublishAction.DELETE -> "delete"
      PublishAction.EMPTY -> "empty"
    }
  }

  fun publish(
    cacheName: String,
    subject: String,
    data: Any,
    id: String?,
    type: String,
    action: PublishAction,
    metrics: CacheMetric
  ) {
    try {
      val event = CloudEventBuilder()
      event.withSubject(subject)
      event.withId(id ?: "000")
      event.withType(type)
      event.withContextAttribute("cache-name", cacheName)
      event.withContextAttribute("publish-action", action(action))

      val body: ByteArray
      if (broadcastWriter.encodePureJson()) {
        body = CacheJsonMapper.mapper.writeValueAsBytes(data)
        event.withData("application/json", body)
      } else {
        body = CacheJsonMapper.writeAsZipBytes(data)
        event.withData("application/json+gzip", body)
      }

      event.withTime(OffsetDateTime.now())

      val span = tracer.spanBuilder(subject)
        .setSpanKind(SpanKind.PRODUCER)
        .startSpan()

      metrics.counter.inc(body.size.toDouble())

      val timer = metrics.perf.startTimer()
      try {
        span.makeCurrent().use {
          try {
            openTelemetry.propagators.textMapPropagator.inject(Context.current(), event, telemetrySetter)

            broadcastWriter.publish(event.build())

            span.setStatus(StatusCode.OK)
          } catch (e: Exception) {
            log.error("Failed to send message", e)
            span.setStatus(StatusCode.ERROR, e.message ?: "failed to send message")
          } finally {
            span.end()
          }
        }
      } finally {
        timer.observeDuration()
      }
    } catch (e: Exception) {
      metrics.failures.inc()
      log.error("Unable to publish {}", subject, e)
    }
  }

  override fun publishEnvironment(cacheName: String, eci: PublishEnvironment) {
    publish(cacheName, environmentSubject!!, eci, eci.environment.id.toString(), "publish-environment-v1",
      eci.action, CacheMetrics.environments)
  }

  override fun publishServiceAccount(cacheName: String, saci: PublishServiceAccount) {
    publish(
      cacheName,
      serviceAccountSubject!!,
      saci,
      saci.serviceAccount?.id.toString(),
      "publish-service-account-v1",
      saci.action,
      CacheMetrics.services
    )
  }

  override fun publishFeature(cacheName: String, feature: PublishFeatureValue) {
    publish(
      cacheName, featureSubject!!, feature,
      "${feature.environmentId}/${feature.feature.feature.key}", "publish-feature-v1",
      feature.action, CacheMetrics.features
    )
  }
}
