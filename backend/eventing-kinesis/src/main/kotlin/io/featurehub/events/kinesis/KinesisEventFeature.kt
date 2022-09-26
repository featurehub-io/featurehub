package io.featurehub.events.kinesis

import io.featurehub.health.HealthSource
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class KinesisEventFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (!isEnabled()) return false

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(KinesisFactoryImpl::class.java).to(KinesisFactory::class.java).to(HealthSource::class.java).`in`(Singleton::class.java)
      }
    })

    return true
  }

  companion object {
    fun isEnabled() = FallbackPropertyConfig.getConfig("cloudevents.kinesis.enabled")?.lowercase() == "true"
  }
}
