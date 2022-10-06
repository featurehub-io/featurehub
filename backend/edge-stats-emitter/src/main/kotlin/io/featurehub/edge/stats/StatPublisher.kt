package io.featurehub.edge.stats

import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventsTelemetryWriter
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.sse.stats.model.EdgeStatsBundle
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface StatPublisher {
  fun publish(cacheName: String, bundle: EdgeStatsBundle)
}

interface CloudEventStatPublisher {
  fun encodeAsJson(): Boolean
  fun publish(event: CloudEvent)
}

class StatPublisherImpl @Inject constructor(private val publisher: CloudEventStatPublisher,
                                            private val telemetryWriter: CloudEventsTelemetryWriter) : StatPublisher {
  private val log: Logger = LoggerFactory.getLogger(StatPublisherImpl::class.java)

  companion object {
    private val prometheusPublishSuccessCounter = ConcurrentHashMap<String, Counter>()
    private val prometheusPublishFailedCounter = ConcurrentHashMap<String, Counter>()
    private val prometheusHistogram = ConcurrentHashMap<String, Histogram>()
  }

  override fun publish(cacheName: String, bundle: EdgeStatsBundle) {
    val event = CloudEventBuilder.v1().newBuilder()
    event.withSubject(EdgeStatsBundle.CLOUD_EVENT_SUBJECT)
    event.withId(UUID.randomUUID().toString())
    event.withType(EdgeStatsBundle.CLOUD_EVENT_TYPE)
    event.withSource(URI("http://dacha2"))
    event.withContextAttribute("cachename", cacheName)
    event.withTime(OffsetDateTime.now())

    try {
      bundle.timestamp(OffsetDateTime.now())

      if (log.isTraceEnabled) {
        log.trace("stat: Publishing /{}/ : {}", cacheName, CacheJsonMapper.mapper.writeValueAsString(bundle))
      }

      CacheJsonMapper.toEventData(event, bundle, !publisher.encodeAsJson())

      val failed = prometheusPublishFailedCounter.computeIfAbsent(cacheName) {
        Counter.build(
          String.format("edge_stat_failed_%s", cacheName.replace("-", "_")),
          String.format("Edge Stats Failed publishing to channel %s", cacheName)
        ).register()
      }
      val histogram = prometheusHistogram.computeIfAbsent(cacheName) {
        Histogram.build(
          String.format("edge_stat_published_%s", cacheName.replace("-", "_")),
          String.format("Edge Stats Published %s", cacheName)
        ).register()
      }

      telemetryWriter.publish(EdgeStatsBundle.CLOUD_EVENT_SUBJECT, event,
        CloudEventChannelMetric(failed, histogram)
      ) { builder ->
        publisher.publish(event.build())
      }
    } catch (e : Exception) {
      log.error("Failed to publish stat event", e)
    }
  }
}
