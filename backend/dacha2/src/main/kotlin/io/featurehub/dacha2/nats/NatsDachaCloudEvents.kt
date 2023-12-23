package io.featurehub.dacha2.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.enricher.FeatureEnricher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.nats.NatsListener
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.publish.NATSFeature
import io.featurehub.publish.NATSSource
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class NatsDachaEventsListener : Feature {
  companion object {
    fun isEnabled() = NATSFeature.isNatsConfigured()
  }

  override fun configure(context: FeatureContext): Boolean {
    LifecycleListeners.starter(Dacha2NatsListener::class.java, context)
    LifecycleListeners.shutdown(Dacha2NatsListener::class.java, context)

    return true
  }
}

@LifecyclePriority(priority = 5)
class Dacha2NatsListener @Inject constructor(
  private val natsSource: NATSSource,
  private val eventListener: CloudEventReceiverRegistry,
  private val featureEnricher: FeatureEnricher
  ) : LifecycleStarted, LifecycleShutdown {
  @ConfigKey("cloudevents.mr-dacha2.nats.channel-name")
  private var dachaChannelName: String? = "featurehub/mr-dacha2-channel"

  private val log: Logger = LoggerFactory.getLogger(Dacha2NatsListener::class.java)
  private var dachaListener: NatsListener? = null
  private var enricherListener: NatsListener? = null

  override fun shutdown() {
    dachaListener?.close()
    enricherListener?.close()
  }

  override fun started() {
    DeclaredConfigResolver.resolve(this)

    dachaListener = natsSource.createTopicListener(dachaChannelName!!, eventListener::process)

    if (featureEnricher.isEnabled()) {
      enricherListener = natsSource.createQueueListener(dachaChannelName!!, "enricher", featureEnricher::enrich)

      log.info("nats: dacha enricher enabled (listen)")
    }

    log.info("nats: cloud event listener started and listening to {}", dachaChannelName)
  }
}
