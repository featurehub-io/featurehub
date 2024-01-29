package io.featurehub.enricher

import io.featurehub.lifecycle.LifecycleListeners
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

class EnrichmentProcessingFeature : Feature {
  override fun configure(ctx: FeatureContext): Boolean {
    // we always bind this, clients should just not do anything if it is disabled (no binding of listeners, etc)
    LifecycleListeners.starter(FeatureEnricherProcessor::class.java, ctx)

    return true
  }
}
