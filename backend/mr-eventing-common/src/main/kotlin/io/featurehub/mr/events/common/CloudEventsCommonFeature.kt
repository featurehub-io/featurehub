package io.featurehub.mr.events.common

import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class CloudEventsCommonFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
      }

    })
    return true
  }
}
