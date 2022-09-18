package io.featurehub.mr.events.common

import io.cloudevents.core.builder.CloudEventBuilder
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

class CloudEventCacheBroadcaster @Inject constructor(
  private val edgeChannel: CloudEventsEdgeChannel,
  private val dachaChannel: CloudEventsDachaChannel,
  private val cloudEventsTelemetryWriter: CloudEventsTelemetryWriter,
) : CacheBroadcast {
  private val log: Logger = LoggerFactory.getLogger(CloudEventCacheBroadcaster::class.java)

  private val featureChannels: Array<CloudEventChannel> =
    if (dachaChannel.dacha2Enabled()) arrayOf(dachaChannel, edgeChannel) else arrayOf(edgeChannel)

  fun publish(
    cacheName: String,
    subject: String,
    data: Any,
    id: String?,
    type: String,
    metrics: CacheMetric,
    vararg channels: CloudEventChannel
  ) {
    try {
      val event = CloudEventBuilder.v1().newBuilder()
      event.withSubject(subject)
      event.withId(id ?: "000")
      event.withType(type)
      event.withSource(URI("http://management-service"))
      event.withContextAttribute("cachename", cacheName)
      event.withTime(OffsetDateTime.now())

      // write any pure json channels
      val pureJsonChannels = channels.filter { it.encodePureJson() }
      if (pureJsonChannels.isNotEmpty()) {
        CacheJsonMapper.toEventData(event, data, false)

        for (cloudEventChannel in pureJsonChannels) {
          publishToChannel(
            subject,
            event,
            metrics,
            cloudEventChannel)
        }
      }

      // now write any zipped channels
      val jsonZipChannels = channels.filter { !it.encodePureJson() }
      if (jsonZipChannels.isNotEmpty()) {
        CacheJsonMapper.toEventData(event, data, true)

        for (cloudEventChannel in jsonZipChannels) {
          publishToChannel(
            subject,
            event,
            metrics,
            cloudEventChannel)
        }
      }
    } catch (e: Exception) {
      log.error("failed", e)
    }
  }

  private fun publishToChannel(
      subject: String,
      event: CloudEventBuilder,
      metrics: CacheMetric,
      channel: CloudEventChannel
  ) {
    cloudEventsTelemetryWriter.publish(
      subject, event,
      CloudEventChannelMetric(metrics.counter, metrics.failures, metrics.perf)
    ) { evt ->
      channel.publishEvent(evt.build())
    }
  }

  override fun publishEnvironment(cacheName: String, eci: PublishEnvironment) {
    if (dachaChannel.dacha2Enabled()) {
      publish(cacheName, PublishEnvironment.CLOUD_EVENT_SUBJECT, eci, eci.environment.id.toString(),
        PublishEnvironment.CLOUD_EVENT_TYPE,
        CacheMetrics.environments, dachaChannel)
    }
  }

  override fun publishServiceAccount(cacheName: String, saci: PublishServiceAccount) {
    if (dachaChannel.dacha2Enabled()) {
      publish(
        cacheName,
        PublishServiceAccount.CLOUD_EVENT_SUBJECT,
        saci,
        saci.serviceAccount?.id.toString(),
        PublishServiceAccount.CLOUD_EVENT_TYPE,
        CacheMetrics.services,
        dachaChannel
      )
    }
  }

  override fun publishFeatures(cacheName: String, features: PublishFeatureValues) {
    if (features.features.isNotEmpty()) {
      // all updates are the same type for the same environment, not that it really matters
      val firstFeature = features.features[0]

      publish(
        cacheName, PublishFeatureValues.CLOUD_EVENT_SUBJECT, features,
        "${firstFeature.environmentId}/${firstFeature.feature.feature.key}", PublishFeatureValues.CLOUD_EVENT_TYPE,
        CacheMetrics.features, *featureChannels
      )
    }
  }
}
