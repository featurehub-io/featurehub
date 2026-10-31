package io.featurehub.health

import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

// the purpose of this resource/feature is to ensure that / responds with a 200
// often a load balancer (ingres) will check that / is returning a 200 before allowing
// traffic through.
class LoadBalancerFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(LoadBalancerResource::class.java)

    return true
  }
}
