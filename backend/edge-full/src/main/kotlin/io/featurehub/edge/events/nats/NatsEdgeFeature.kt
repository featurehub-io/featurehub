package io.featurehub.edge.events.nats

import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.publish.NATSFeature
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class NatsEdgeFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
//    if (!NATSFeature.isNatsConfigured()) return false
    context.register(NATSFeature::class.java)

//    LifecycleListeners.starter(NatsFeatureUpdatePublisher::class.java, context)
    if (isDacha2Enabled()) {
//      LifecycleListeners.wrap(NatsFeatureListener::class.java, context)
    }
    if (isDacha1Enabled()) {
      LifecycleListeners.wrap(NatsOriginalListener::class.java, context)
    }

    return true
  }

  fun isDacha1Enabled(): Boolean =
    FallbackPropertyConfig.getConfig("dacha1.enabled") != "false"

  fun isDacha2Enabled(): Boolean =
    FallbackPropertyConfig.getConfig("dacha2.enabled") == "true"

}
