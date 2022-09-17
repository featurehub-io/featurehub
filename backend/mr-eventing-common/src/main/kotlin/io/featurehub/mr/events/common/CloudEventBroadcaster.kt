package io.featurehub.mr.events.common

import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventsTelemetryWriter
import io.featurehub.jersey.config.CacheJsonMapper
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime

interface CloudEventBroadcasterWriter {
  fun encodePureJson(): Boolean
  fun publish(event: CloudEvent)
}


class CloudEventBroadcaster @Inject constructor(
  private val broadcastWriter: CloudEventBroadcasterWriter,
  private val cloudEventsTelemetryWriter: CloudEventsTelemetryWriter
) : CacheBroadcast {
  private val log: Logger = LoggerFactory.getLogger(CloudEventBroadcaster::class.java)


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
      event.withSource(URI("http://management-service"))
      event.withContextAttribute("cachename", cacheName)
      event.withContextAttribute("publishaction", action(action))

      val body: ByteArray
      if (broadcastWriter.encodePureJson()) {
        body = CacheJsonMapper.mapper.writeValueAsBytes(data)
        event.withData("application/json", body)
      } else {
        body = CacheJsonMapper.writeAsZipBytes(data)
        event.withData("application/json+gzip", body)
      }

      event.withTime(OffsetDateTime.now())

      cloudEventsTelemetryWriter.publish(
        subject, event,
        CloudEventChannelMetric(metrics.counter, metrics.failures, metrics.perf)
      ) { evt ->
        broadcastWriter.publish(evt.build())
      }
    } catch (e: Exception) {
      log.error("failed", e)
    }
  }

  override fun publishEnvironment(cacheName: String, eci: PublishEnvironment) {
    publish(cacheName, PublishEnvironment.CLOUD_EVENT_SUBJECT, eci, eci.environment.id.toString(),
      PublishEnvironment.CLOUD_EVENT_TYPE,
      eci.action, CacheMetrics.environments)
  }

  override fun publishServiceAccount(cacheName: String, saci: PublishServiceAccount) {
    publish(
      cacheName,
      PublishServiceAccount.CLOUD_EVENT_SUBJECT,
      saci,
      saci.serviceAccount?.id.toString(),
      PublishServiceAccount.CLOUD_EVENT_TYPE,
      saci.action,
      CacheMetrics.services
    )
  }

  override fun publishFeature(cacheName: String, features: PublishFeatureValues) {
    if (features.features.isNotEmpty()) {
      // all updates are the same type for the same environment, not that it really matters
      val firstFeature = features.features[0]

      publish(
        cacheName, PublishFeatureValues.CLOUD_EVENT_SUBJECT, features,
        "${firstFeature.environmentId}/${firstFeature.feature.feature.key}", PublishFeatureValues.CLOUD_EVENT_TYPE,
        firstFeature.action, CacheMetrics.features
      )
    }
  }
}
