package io.featurehub.edge.events.nats

import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.publish.NATSFeature
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

class NatsEdgeFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (!NATSFeature.isNatsConfigured() || !isDacha1Enabled() ) return false
    LifecycleListeners.wrap(NatsOriginalListener::class.java, context)

    return true
  }

  fun isDacha1Enabled(): Boolean =
    FallbackPropertyConfig.getConfig("dacha1.enabled") != "false"
}
