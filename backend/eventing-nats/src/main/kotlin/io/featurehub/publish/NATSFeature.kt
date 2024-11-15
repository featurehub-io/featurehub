package io.featurehub.publish

import io.featurehub.events.EventingConnection
import io.featurehub.events.nats.NATSConfiguredSource
import io.featurehub.events.nats.NATSDynamicPublisher
import io.featurehub.health.HealthSource
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class NATSFeature : Feature {
  companion object {
    fun isNatsConfigured() =
      FallbackPropertyConfig.getConfig("nats.urls") != null && FallbackPropertyConfig.getConfig("nats.enabled") != "false"
  }

  override fun configure(context: FeatureContext): Boolean {
    if (!isNatsConfigured()) return false

    context.register(object : AbstractBinder() {
      override fun configure() {
        bind(NATSConnectionSource::class.java).to(NATSSource::class.java).to(EventingConnection::class.java).`in`(Singleton::class.java)
        bind(NATSHealthSource::class.java).to(HealthSource::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.starter(NATSDynamicPublisher::class.java, context)
    LifecycleListeners.wrap(NATSConfiguredSource::class.java, context)

    return true
  }
}
