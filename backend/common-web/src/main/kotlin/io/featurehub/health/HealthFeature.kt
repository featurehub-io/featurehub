package io.featurehub.health

import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class HealthFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {

    context.register(object : AbstractBinder() {
      override fun configure() {
        bind(ApplicationHealthSource::class.java).to(HealthSource::class.java).`in`(Singleton::class.java)
      }
    })

    context.register(FeatureHubHealthResource::class.java)

    return true;
  }
}
