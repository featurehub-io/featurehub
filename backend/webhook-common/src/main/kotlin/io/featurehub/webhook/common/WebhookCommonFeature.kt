package io.featurehub.webhook.common

import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.metrics.MetricsCollector
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

// BE CAREFUL ADDING STUFF HERE, this artifact is directly included into the MR api. May need
// to split this into just an API and a "common" artifact it is extends out.
class WebhookCommonFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {

    return true
  }

  companion object {
    val channelMetric: CloudEventChannelMetric

    init {
      channelMetric = CloudEventChannelMetric(
        MetricsCollector.counter("webhooks_features_failure", "Attempts to publish webhook that have failed"),
        MetricsCollector.histogram("webhooks_features", "Tracking publishing webhook statuses")
      )
    }
  }
}
