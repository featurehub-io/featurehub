package io.featurehub.enricher.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.enriched.model.EnrichedFeatures
import io.featurehub.enricher.FeatureEnricher
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@LifecyclePriority(priority = 10)
class PubsubEnricherListener @Inject constructor(
  pubSubFactory: PubSubFactory,
  eventListener: CloudEventReceiverRegistry
) : LifecycleListener {
  @ConfigKey("cloudevents.enricher.pubsub.enriched-subscription-name")
  private var enricherChannelName: String? = "featurehub-enriched-events"
  private val log: Logger = LoggerFactory.getLogger(PubsubEnricherListener::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    pubSubFactory.makeSubscriber(enricherChannelName!!) {
      try {
        eventListener.process(it)
      } catch (e: Exception) {
        log.error("failed to process enriched event", e)
      }

      true
    }
  }
}

/**
 * Pubsub is different to NATS or Kinesis because we cannot share one "channel/topic". In Pubsub, you cannot have all
 * subscribers share the same message, only one of the subscribers gets the message. So we need to specify the channels
 * we will use by giving them names, and then we dynamically look up the name pair.
 */
@LifecyclePriority(priority = 10)
class PubsubEnricherPublisher @Inject constructor(
  pubSubFactory: PubSubFactory,
  cloudEventPublisher: CloudEventPublisherRegistry,
  featureEnricher: FeatureEnricher
) : LifecycleListener {
  private val log: Logger = LoggerFactory.getLogger(PubsubEnricherPublisher::class.java)

  init {
    val publishers = pubSubFactory.makePublishersFromConfig(
      "cloudevents.enricher.pubsub.channels",
      "cloudevents.enricher.pubsub.channel",
      "featurehub-enriched-events"
      )

    cloudEventPublisher.registerForPublishing(
      EnrichedFeatures.CLOUD_EVENT_TYPE,
      featureEnricher.metric(), true
    ) { msg ->
      try {
        publishers.forEach { key, publisher ->
          publisher.publish(msg)
        }
      } catch (e: Exception) {
        log.error("Failed to publish on enricher", e)
      }
    }
  }
}
