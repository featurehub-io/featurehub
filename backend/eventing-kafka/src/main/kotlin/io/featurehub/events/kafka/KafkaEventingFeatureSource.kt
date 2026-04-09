package io.featurehub.events.kafka

import io.featurehub.events.EventingFeatureSource
import jakarta.ws.rs.core.Feature

class KafkaEventingFeatureSource : EventingFeatureSource {
  override val featureSource: Class<out Feature>?
    get() = if (KafkaEventFeature.isEnabled()) KafkaEventFeature::class.java else null
  override val enabled: Boolean
    get() = KafkaEventFeature.isEnabled()
}
