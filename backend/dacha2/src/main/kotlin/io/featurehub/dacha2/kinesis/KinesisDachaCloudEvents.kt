package io.featurehub.dacha2.kinesis

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.enriched.model.EnrichedFeatures
import io.featurehub.enricher.FeatureEnricher
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.kinesis.KinesisFactory
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

@LifecyclePriority(priority = 12)
class KinesisDachaCloudEvents @Inject constructor(
  kinesisFactory: KinesisFactory,
  eventListener: CloudEventReceiverRegistry,
  featureEnricher: FeatureEnricher,
  cloudEventPublisher: CloudEventPublisher) : LifecycleListener {
  @ConfigKey("cloudevents.inbound.kinesis.mr-features-name")
  var topicName: String? = "featurehub-mr-dacha2"
  @ConfigKey("cloudevents.enricher.channel-name")
  private var enricherChannelName: String? = "featurehub-enriched-events"
  @ConfigKey("cloudevents.enricher.kinesis.randomise")
  private var randomise: Boolean? = true

  private val log: Logger = LoggerFactory.getLogger(KinesisDachaCloudEvents::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    kinesisFactory.makeSubscriber("dacha2-" + UUID.randomUUID().toString(), topicName!!) {event ->
      eventListener.process(event)
    }

    if (featureEnricher.isEnabled()) {
      kinesisFactory.makeSubscriber("enricher", topicName!!, featureEnricher::enrich)

      val publisher = kinesisFactory.makePublisher(enricherChannelName!!)

      cloudEventPublisher.registerForPublishing(
        EnrichedFeatures.CLOUD_EVENT_TYPE,
        featureEnricher.metric(), false) { msg ->
        publisher.publish(msg, publishKey())
      }

      log.info("pubsub: dacha enricher enabled (listen & publish)")

    }
  }

  private fun publishKey() = if (randomise!!) UUID.randomUUID().toString() else "dacha2-enricher"

}
