package io.featurehub.publish

import io.featurehub.health.HealthSource
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NATSFeature : Feature {
  companion object {
    fun isNatsConfigured() =
      FallbackPropertyConfig.getConfig("nats.urls") != null && FallbackPropertyConfig.getConfig("nats.enabled") != "false"
  }

  override fun configure(context: FeatureContext): Boolean {
    if (!isNatsConfigured()) return false

    context.register(object : AbstractBinder() {
      override fun configure() {
        bind(NATSConnectionSource::class.java).to(NATSSource::class.java).`in`(Singleton::class.java)
        bind(NATSHealthSource::class.java).to(HealthSource::class.java).`in`(Singleton::class.java)
//        bind(NATSDynamicPublisher::class.java).to(NATSDynamicPublisher::class.java).`in`(Immediate::class.java)
      }
    })

    return true
  }
}
