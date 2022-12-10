package io.featurehub.dacha2.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.enriched.model.EnrichedFeatures
import io.featurehub.enricher.FeatureEnricher
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.pubsub.PubSubFactory
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PubsubDachaCloudEvents @Inject constructor(
  pubSubFactory: PubSubFactory,
  eventListener: CloudEventReceiverRegistry,
  featureEnricher: FeatureEnricher,
  cloudEventPublisher: CloudEventPublisher) {
  @ConfigKey("cloudevents.mr-dacha2.pubsub.topic-name")
  var topicName: String? = "featurehub-mr-dacha2"
  @ConfigKey("cloudevents.enricher.pubsub.subscription-name")
  var enricherSubscriptionName: String? = "enricher-updates-sub"
  @ConfigKey("cloudevents.mr-dacha2.pubsub.subscription-prefix")
  var subscriptionPrefix: String? = "featurehub-dacha2-listener"
  @ConfigKey("cloudevents.enricher.channel-name")
  private var enricherChannelName: String? = "featurehub-enriched-events"

  private val log: Logger = LoggerFactory.getLogger(PubsubDachaCloudEvents::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    pubSubFactory.makeUniqueSubscriber(topicName!!, subscriptionPrefix!!) { msg ->
      try {
        eventListener.process(msg)
      } catch (e: Exception) {
        log.error("dacha2 pubsub: Unable to process incoming event")
      }
      true //
    }

    if (featureEnricher.isEnabled()) {
      log.info("dacha2: pubsub enriching feature requests")
      pubSubFactory.makeSubscriber(enricherSubscriptionName!!) {
        featureEnricher.enrich(it)
      }
      val publisher = pubSubFactory.makePublisher(enricherChannelName!!)
      cloudEventPublisher.registerForPublishing(
        EnrichedFeatures.CLOUD_EVENT_TYPE,
        featureEnricher.metric(), true, publisher::publish)

      log.info("pubsub: dacha enricher enabled (listen & publish)")

    }

    log.info("dacha2: pubsub listening to {}", topicName)
  }
}
