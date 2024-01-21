package io.featurehub.enricher.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.enriched.model.EnrichedFeatures
import io.featurehub.enricher.FeatureEnricher
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.nats.NatsListener
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class NatsEnricherBase {
  @ConfigKey("cloudevents.enricher.channel-name")
  protected var enricherChannelName: String? = "featurehub/enriched-events"

  @ConfigKey("cloudevents.enricher.queue-name")
  protected var enricherQueueName: String? = "enrich-listener"

  init {
    DeclaredConfigResolver.resolve(this)
  }
}

/**
 * We include this one here, because Dacha1 and Dacha2 support NATS. Used by a service that publishes
 */
@LifecyclePriority(priority = 10)
class NatsEnricherPublisher @Inject constructor(
  natsSource: NATSSource,
  featureEnricher: FeatureEnricher,
  cloudEventsPublisher: CloudEventPublisherRegistry
) : NatsEnricherBase(), LifecycleListener {
  init {
    val publisher = natsSource.createPublisher(enricherChannelName!!)

    cloudEventsPublisher.registerForPublishing(
      EnrichedFeatures.CLOUD_EVENT_TYPE,
      featureEnricher.metric(), true, publisher::publish
    )
  }
}

/**
 * used by a service that listens to the enriched stream
 */
@LifecyclePriority(priority = 10)
class NatsEnricherListener @Inject constructor(
  natsSource: NATSSource,
  private val eventListener: CloudEventReceiverRegistry
) : NatsEnricherBase(), LifecycleShutdown {
  private val log: Logger = LoggerFactory.getLogger(NatsEnricherListener::class.java)
  private val enricherListener: NatsListener

  init {
    enricherListener =
      natsSource.createQueueListener(enricherChannelName!!, enricherQueueName!!, eventListener::process)

    log.info("nats: dacha enricher enabled (listen)")
  }

  override fun shutdown() {
    enricherListener.close()
  }
}

