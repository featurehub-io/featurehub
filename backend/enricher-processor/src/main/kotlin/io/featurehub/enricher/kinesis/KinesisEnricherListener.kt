package io.featurehub.enricher.kinesis

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.enriched.model.EnrichedFeatures
import io.featurehub.enricher.FeatureEnricher
import io.featurehub.enricher.nats.NatsEnricherBase
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.kinesis.KinesisFactory
import io.featurehub.events.nats.NatsListener
import io.featurehub.publish.NATSSource
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

open class KinesisEnricherBase {
  @ConfigKey("cloudevents.enricher.channel-name")
  protected var enricherChannelName: String? = "featurehub-enriched-events"

  @ConfigKey("cloudevents.enricher.kinesis.queue-name")
  protected var queueName: String? = "enricher-queue"

  @ConfigKey("cloudevents.enricher.kinesis.randomise")
  protected var randomise: Boolean? = true

  init {
    DeclaredConfigResolver.resolve(this)
  }
}

// used for example in Edge
class KinesisEnricherListener @Inject constructor(
  kinesisFactory: KinesisFactory,
  eventListener: CloudEventReceiverRegistry
) : KinesisEnricherBase() {

  init {
    DeclaredConfigResolver.resolve(this)

    kinesisFactory.makeSubscriber(queueName!!, enricherChannelName!!, eventListener::process)
  }
}

// only used by Dacha2
class KinesisEnricherPublisher @Inject constructor(
  kinesisFactory: KinesisFactory,
  featureEnricher: FeatureEnricher,
  cloudEventsPublisher: CloudEventPublisher
) : KinesisEnricherBase() {
  init {
    val publisher = kinesisFactory.makePublisher(enricherChannelName!!)

    cloudEventsPublisher.registerForPublishing(
      EnrichedFeatures.CLOUD_EVENT_TYPE,
      featureEnricher.metric(), false
    ) { msg ->
      publisher.publish(msg, publishKey())
    }
  }

  private fun publishKey() = if (randomise!!) UUID.randomUUID().toString() else "enricher-client"
}
