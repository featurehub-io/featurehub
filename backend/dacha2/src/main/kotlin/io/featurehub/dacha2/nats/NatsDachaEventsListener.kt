package io.featurehub.dacha2.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.nats.NatsListener
import io.featurehub.publish.NATSFeature
import io.featurehub.publish.NATSSource
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NatsDachaEventsListener : Feature {
  companion object {
    fun isEnabled() = NATSFeature.isNatsConfigured()
  }

  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(Dacha2NatsListener::class.java).to(Dacha2NatsListener::class.java).`in`(Immediate::class.java)
      }

    })
    return true
  }
}

class Dacha2NatsListener @Inject constructor(natsSource: NATSSource, private val eventListener: CloudEventReceiverRegistry) {
  @ConfigKey("cloudevents.mr-dacha2.nats.channel-name")
  private var dachaChannelName: String? = "featurehub/mr-dacha2-channel"

  private val log: Logger = LoggerFactory.getLogger(Dacha2NatsListener::class.java)
  private val dachaListener: NatsListener

  init {
    DeclaredConfigResolver.resolve(this)

    dachaListener = natsSource.createTopicListener(dachaChannelName!!) {
      eventListener.process(it)
    }

    log.info("nats: cloud event listener started and listening to {}", dachaChannelName)
  }

  @PreDestroy
  fun shutdown() {
    dachaListener.close()
  }
}
