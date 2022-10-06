package io.featurehub.edge.events.nats

import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.edge.StreamingFeatureController
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.publish.ChannelConstants
import io.featurehub.publish.ChannelNames
import io.featurehub.publish.NATSSource
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.Subscription
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class is used when we are using Dacha1
 */
class NatsOriginalListener @Inject constructor(private val natsSource: NATSSource, private val controller: StreamingFeatureController) {
  private val subscription: Subscription
  private val dispatcher: Dispatcher = natsSource.connection.createDispatcher()
  private val log: Logger = LoggerFactory.getLogger(NatsOriginalListener::class.java)

  init {
    subscription = dispatcher.subscribe(ChannelNames.featureValueChannel(ChannelConstants.DEFAULT_CACHE_NAME), this::process)

    log.info("listening for pre 1.5.9-Dacha1 style feature updates")
  }

  @PreDestroy
  fun close() {
    natsSource.connection.closeDispatcher(dispatcher)
  }

  fun process(msg: Message) {
    val fv = CacheJsonMapper.readFromZipBytes(msg.getData(), PublishFeatureValue::class.java)
    controller.updateFeatures(PublishFeatureValues().addFeaturesItem(fv))
  }
}
