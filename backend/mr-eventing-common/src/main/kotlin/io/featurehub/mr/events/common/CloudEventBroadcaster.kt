package io.featurehub.mr.events.common

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.dacha.model.*
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventTelemetryWrapper
import io.featurehub.events.KnownEventSubjects
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
  private val cloudEventTelemetryWrapper: CloudEventTelemetryWrapper
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
    val event = CloudEventBuilder()
    event.withSubject(subject)
    event.withId(id ?: "000")
    event.withType(type)
    event.withSource(URI("http://management-service"))
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

    cloudEventTelemetryWrapper.publish(subject, event,
      CloudEventChannelMetric(metrics.counter, metrics.failures, metrics.perf)) { evt ->
        broadcastWriter.publish(evt.build())
    }
  }

  override fun publishEnvironment(cacheName: String, eci: PublishEnvironment) {
    publish(cacheName, KnownEventSubjects.Management.environmentUpdate, eci, eci.environment.id.toString(), "publish-environment-v1",
      eci.action, CacheMetrics.environments)
  }

  override fun publishServiceAccount(cacheName: String, saci: PublishServiceAccount) {
    publish(
      cacheName,
      KnownEventSubjects.Management.serviceAccountUpdate,
      saci,
      saci.serviceAccount?.id.toString(),
      "publish-service-account-v1",
      saci.action,
      CacheMetrics.services
    )
  }

  override fun publishFeature(cacheName: String, features: PublishFeatureValues) {
    if (features.features.isNotEmpty()) {
      // all updates are the same type for the same environment, not that it really matters
      val firstFeature = features.features[0]

      publish(
        cacheName, KnownEventSubjects.Management.featureUpdates, features,
        "${firstFeature.environmentId}/${firstFeature.feature.feature.key}", "publish-features-v1",
        firstFeature.action, CacheMetrics.features
      )
    }
  }
}
