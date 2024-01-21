package io.featurehub.dacha2.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.enricher.FeatureEnricher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@LifecyclePriority(priority = 12)
class PubsubDachaCloudEvents @Inject constructor(
  pubSubFactory: PubSubFactory,
  eventListener: CloudEventReceiverRegistry,
  featureEnricher: FeatureEnricher) : LifecycleListener {
  @ConfigKey("cloudevents.mr-dacha2.pubsub.topic-name")
  var topicName: String? = "featurehub-mr-dacha2"
  @ConfigKey("cloudevents.enricher.pubsub.subscription-name")
  var enricherSubscriptionName: String? = "enricher-updates-sub"
  @ConfigKey("cloudevents.mr-dacha2.pubsub.subscription-prefix")
  var subscriptionPrefix: String? = "featurehub-dacha2-listener"

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

      log.info("pubsub: dacha enricher enabled (listen)")
    }

    log.info("dacha2: pubsub listening to {}", topicName)
  }
}
