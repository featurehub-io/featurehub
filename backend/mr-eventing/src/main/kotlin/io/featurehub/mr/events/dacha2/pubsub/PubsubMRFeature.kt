package io.featurehub.mr.events.dacha2.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.enriched.model.EnricherPing
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.mr.events.common.CacheMetrics
import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

class PubsubMRFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (!GoogleEventFeature.isEnabled()) return false

    LifecycleListeners.starter(PubsubCloudEventsEdgeChannel::class.java, context)
    LifecycleListeners.starter(PubsubCloudEventsDachaChannel::class.java, context)

    return true
  }
}

@LifecyclePriority(priority = 12)
class PubsubCloudEventsEdgeChannel @Inject constructor(pubSubFactory: PubSubFactory, cloudEventsPublisher: CloudEventPublisher): LifecycleListener {
  @ConfigKey("cloudevents.mr-edge.pubsub.topic-name")
  private var edgeChannelName: String? = "featurehub-mr-edge"

  init {
    DeclaredConfigResolver.resolve(this)
    val publisher = pubSubFactory.makePublisher(edgeChannelName!!)

    cloudEventsPublisher.registerForPublishing(
      PublishFeatureValues.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(CacheMetrics.featureFailureCounter, CacheMetrics.featureGram),
      true, publisher::publish)
  }
}

@LifecyclePriority(priority = 12)
class PubsubCloudEventsDachaChannel @Inject constructor(pubSubFactory: PubSubFactory, cloudEventsPublisher: CloudEventPublisher): LifecycleListener {
  @ConfigKey("cloudevents.mr-dacha2.pubsub.topic-name")
  private var dachaChannelName: String? = "featurehub-mr-dacha2"

  init {
    DeclaredConfigResolver.resolve(this)
    val publisher = pubSubFactory.makePublisher(dachaChannelName!!)

    cloudEventsPublisher.registerForPublishing(
      EnricherPing.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(CacheMetrics.featureFailureCounter, CacheMetrics.featureGram), true, publisher::publish)

    cloudEventsPublisher.registerForPublishing(
      PublishFeatureValues.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(CacheMetrics.featureFailureCounter, CacheMetrics.featureGram),
      true, publisher::publish)

    cloudEventsPublisher.registerForPublishing(
      PublishServiceAccount.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(CacheMetrics.serviceAccountCounter, CacheMetrics.serviceAccountsGram),
      true, publisher::publish)

    cloudEventsPublisher.registerForPublishing(
      PublishEnvironment.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(CacheMetrics.environmentCounter, CacheMetrics.environmentGram),
      true, publisher::publish)
  }
}
