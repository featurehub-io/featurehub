package io.featurehub.health

import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

class LoadBalancerFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(LoadBalancerResource::class.java)

    return true
  }
}
