package io.featurehub.publish

import io.featurehub.health.HealthSource
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class NATSFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(NATSConnectionSource::class.java).to(NATSSource::class.java).`in`(Singleton::class.java)
        bind(NATSHealthSource::class.java).to(HealthSource::class.java).`in`(Singleton::class.java)
      }

    })

    return true
  }
}
