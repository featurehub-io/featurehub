package io.featurehub.events.nats

import io.featurehub.events.EventingFeatureSource
import io.featurehub.publish.NATSFeature
import jakarta.ws.rs.core.Feature

class NATSEventingFeatureSource : EventingFeatureSource {
  override val featureSource: Class<out Feature>?
    get() = if (NATSFeature.isNatsConfigured()) NATSFeature::class.java else null
  override val enabled: Boolean
    get() = NATSFeature.isNatsConfigured()
}
