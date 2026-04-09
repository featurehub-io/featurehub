package io.featurehub.events.kafka

import io.featurehub.events.DefaultEventingConnection
import io.featurehub.events.EventingConnection
import io.featurehub.health.HealthSource
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KafkaEventFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (!isEnabled()) return false

    log.info("Event transport detected as Kafka")

    context.register(object : AbstractBinder() {
      override fun configure() {
        bind(KafkaFactoryImpl::class.java)
          .to(KafkaFactory::class.java)
          .to(HealthSource::class.java)
          .`in`(Singleton::class.java)
        bind(DefaultEventingConnection::class.java).to(EventingConnection::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.starter(KafkaDynamicPublisher::class.java, context)
    LifecycleListeners.wrap(KafkaConfiguredSource::class.java, context)

    return true
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(KafkaEventFeature::class.java)

    fun isEnabled() =
      FallbackPropertyConfig.getConfig("cloudevents.kafka.bootstrap.servers") != null &&
        FallbackPropertyConfig.getConfig("cloudevents.kafka.enabled") != "false"
  }
}
