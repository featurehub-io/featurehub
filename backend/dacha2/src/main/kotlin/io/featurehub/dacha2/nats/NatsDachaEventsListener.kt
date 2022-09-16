package io.featurehub.dacha2.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.nats.NatsMessageFactory
import io.featurehub.dacha2.Dacha2CloudEventListener
import io.featurehub.publish.NATSFeature
import io.featurehub.publish.NATSSource
import io.nats.client.Dispatcher
import io.nats.client.Message
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
    context.register(NATSFeature::class.java)

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(NatsListener::class.java).to(NatsListener::class.java).`in`(Immediate::class.java)
      }

    })
    return true
  }
}

class NatsListener @Inject constructor(natsSource: NATSSource, private val eventListener: Dacha2CloudEventListener) {
  private val dispatcher: Dispatcher

  @ConfigKey("dacha2.environment.nats-channel-name")
  private var environmentSubject: String? = "featurehub/environment"
  @ConfigKey("dacha2.service-account.nats-channel-name")
  private var serviceAccountSubject: String? = "featurehub/service-account"
  @ConfigKey("dacha2.features.nats-channel-name")
  private var featureSubject: String? = "featurehub/feature"

  private val log: Logger = LoggerFactory.getLogger(NatsListener::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    dispatcher = natsSource.connection.createDispatcher()
    dispatcher.subscribe(environmentSubject, this::processEvent)
    dispatcher.subscribe(serviceAccountSubject, this::processEvent)
    dispatcher.subscribe(featureSubject, this::processEvent)

    log.info("nats: cloud event listener started and listening to {}, {} and {}", environmentSubject, serviceAccountSubject, featureSubject)
  }

  private fun processEvent(msg: Message) {
    log.info("received message {}", msg.headers.toString())
    NatsMessageFactory.createReader(msg)?.let {
      try {
        eventListener.process(it.toEvent())
      } catch (e: Exception) {
        log.error("failed ot process incoming message", e)
      }
    } ?: log.error("Unable to parse incoming NATS message into an event")
  }

  @PreDestroy
  fun shutdown() {
    dispatcher.unsubscribe(environmentSubject)
    dispatcher.unsubscribe(serviceAccountSubject)
    dispatcher.unsubscribe(featureSubject)
  }
}
