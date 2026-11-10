package io.featurehub.edge.ofrep

import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

/**
 * Registers the OFREP resource and its service. Registered by
 * [io.featurehub.edge.EdgeCommonFeature], so every edge deployment
 * (edge-rest, edge-full, party-server, party-server-ish) serves OFREP.
 */
class OfrepFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(OfrepResource::class.java)

    context.register(
      object : AbstractBinder() {
        override fun configure() {
          bind(OfrepEvaluationService::class.java)
            .to(OfrepEvaluationService::class.java)
            .`in`(Singleton::class.java)
        }
      }
    )

    return true
  }
}
