package io.featurehub.enricher

import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.enriched.model.EnrichedFeatures
import io.featurehub.enriched.model.EnricherPing
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.CloudEventsTelemetryReader
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.metrics.MetricsCollector
import io.featurehub.rest.Info
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.validation.constraints.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

interface FeatureEnricher {
  fun isEnabled(): Boolean
  fun metric(): CloudEventChannelMetric
  fun processFeature(fv: PublishFeatureValue)
  fun enricherPing(event: CloudEvent, simpleEnricher: EnricherPing)
}

data class EnrichmentEnvironment(val features: Collection<CacheEnvironmentFeature>,
                                 val environment: PublishEnvironment)

interface FeatureEnrichmentCache {
  fun getEnrichableEnvironment(eId: UUID): EnrichmentEnvironment
  fun updateFeature(feature: PublishFeatureValue)
}

/**
 * Dacha1 needs FeatureEnricher as an API, Dacha2 does not, it just needs the class to be instantiated.
 */
@LifecyclePriority(LifecyclePriority.INTERNAL_PRIORITY_END)
class FeatureEnricherProcessor @Inject constructor(
  private val cache: FeatureEnrichmentCache,
  private val cloudEventPubisher: CloudEventPublisherRegistry,
  cloudReceiverRegistry: CloudEventReceiverRegistry
) : FeatureEnricher, LifecycleListener {
  private val log: Logger = LoggerFactory.getLogger(FeatureEnricherProcessor::class.java)

  val publishOnlyWhenEnvironmentNotEmpty = FallbackPropertyConfig.getConfig("enricher.ignore-when-empty", "true").lowercase() == "true"
  val publishFeatureMetric: CloudEventChannelMetric

  init {
    DeclaredConfigResolver.resolve(this)

    publishFeatureMetric = CloudEventChannelMetric(
      MetricsCollector.counter("enrich_publish_fail", "Enrichment Feature publishing failures"),
      MetricsCollector.histogram("enrich_publish", "Enrichment Feature publishing")
    )

    cloudReceiverRegistry.registry(if (Info.applicationName() == "party-server") "common" else "enricher").listen(PublishFeatureValues::class.java) { featureData, ce ->
      log.trace("enricher received CE of type {}", ce.type)
      enrichData(featureData, ce.time)
    }

    cloudReceiverRegistry.registry("enricher").listen(EnricherPing::class.java) { ping, ce ->
      log.trace("enricher received CE of type {}", ce.type)
      enricherPing(ce, ping)
    }
  }

  override fun isEnabled(): Boolean = EnricherConfig.enabled()

  override fun metric(): CloudEventChannelMetric = publishFeatureMetric

  /**
   * coming in from dacha1, NATS only
   */
  override fun processFeature(fv: PublishFeatureValue) {
    log.trace("dacha1 request to enrich new feature: {}", fv)
    enrichData(PublishFeatureValues().features(listOf(fv)), OffsetDateTime.now())
  }

  /**
   * internal testing only
   */
  fun enrich(event: CloudEvent): Boolean {
    log.trace("enricher received CE of type {}", event.type)
    if (isEnabled()) {
        if (event.type == PublishFeatureValues.CLOUD_EVENT_TYPE) {
          CacheJsonMapper.fromEventData(event, PublishFeatureValues::class.java)?.let { featureData ->

            enrichData(featureData, event.time)
          }
        }
    }

    return true
  }

  override fun enricherPing(event: CloudEvent, simpleEnricher: EnricherPing) {
    publishEnrichedStream(simpleEnricher.environment, simpleEnricher.cloudEventProcessor, listOf(), event.time)
  }

  private fun enrichData(featureData: PublishFeatureValues, time: OffsetDateTime?) {
    log.trace("enriching features {}", featureData)
    val envs = mutableMapOf<UUID, MutableList<PublishFeatureValue>>()
    featureData.features.forEach { envs.getOrPut(it.environmentId) { mutableListOf() }.add(it) }

    envs.keys.forEach { envId ->
      envs[envId]?.let { features ->
        features.forEach {
          cache.updateFeature(it) // ensure it is up to date or ignore or whatever
        }

        val updatedFeatures = features.map { it.feature.feature.key }

        log.trace("publishing enriched stream for env {} -> {}", envId, updatedFeatures)
        publishEnrichedStream(envId, null, updatedFeatures, time)
      }
    }
  }

  private fun publishEnrichedStream(
    envId: UUID,
    targetProcessor: String?,
    updatedFeatures: List<@NotNull String>,
    time: OffsetDateTime?
  ) {
    try {
      val env = cache.getEnrichableEnvironment(envId)

      if (env.environment.environment.webhookEnvironment?.isEmpty() == true && publishOnlyWhenEnvironmentNotEmpty) {
        log.trace("environment {} has no webhooks config, skipping", env.environment)
        return
      }

      val data = EnrichedFeatures()
        .targetEnrichmentDestination(targetProcessor)
        .featureKeys(updatedFeatures)
        .environment(env.environment.copy().featureValues(env.features.toList()))

      val event = CloudEventBuilder.v1().newBuilder()
      event.withSubject(EnrichedFeatures.CLOUD_EVENT_SUBJECT)
      event.withId(envId.toString() + "/" + System.currentTimeMillis().toString())
      event.withType(EnrichedFeatures.CLOUD_EVENT_TYPE)
      event.withSource(URI("http://enricher"))
      event.withTime(time)

      cloudEventPubisher.publish(EnrichedFeatures.CLOUD_EVENT_TYPE, data, event)
    } catch (e: Exception) {
      log.error("There was an error trying to find the environment for a publish feature value", e)
    }
  }
}
