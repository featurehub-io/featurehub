package io.featurehub.events

import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

class FakeExternalEventingFeature : Feature {
  override fun configure(context: FeatureContext): Boolean = true
}

/**
 * Registered via META-INF/services in tests only, stands in for NATS/Kafka/PubSub/Kinesis.
 */
class FakeEventingFeatureSource : EventingFeatureSource {
  override val featureSource: Class<out Feature>?
    get() = if (enabledFlag) FakeExternalEventingFeature::class.java else null
  override val enabled: Boolean
    get() = enabledFlag

  companion object {
    @JvmStatic
    var enabledFlag = false
  }
}
