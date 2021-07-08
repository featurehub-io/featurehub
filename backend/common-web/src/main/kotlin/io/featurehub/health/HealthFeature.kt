package io.featurehub.health

import org.glassfish.jersey.internal.inject.AbstractBinder
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

class HealthFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {

    context.register(object : AbstractBinder() {
      override fun configure() {
        bind(ApplicationHealthSource::class.java).`in`(Singleton::class.java)
      }
    })

    context.register(FeatureHubHealthResource::class.java)

    return true;
  }
}
