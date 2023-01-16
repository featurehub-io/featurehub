package io.featurehub.mr.events.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.enriched.model.EnricherPing
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.nats.NatsCloudEventsPublisher
import io.featurehub.mr.events.common.CacheMetrics
import io.featurehub.mr.events.common.Dacha2Config
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject


/**
 * This broadcasts events specifically for the consumption of Edge (which is just feature updates)
 */
class NatsCloudEventsPublishers @Inject constructor(nats: NATSSource, cloudEventsPublisher: CloudEventPublisher) {
  @ConfigKey("cloudevents.mr-edge.nats.channel-name")
  private var edgeChannelName: String? = "featurehub/mr-edge-channel"
  @ConfigKey("cloudevents.mr-dacha2.nats.channel-name")
  private var dachaChannelName: String? = "featurehub/mr-dacha2-channel"
  // only NATS is available for dacha1 anyway
  @ConfigKey("dacha1.inbound.nats.channel-name")
  var dacha1CloudEventsChannel: String? = "featurehub-dacha1-cloudevents";

  init {
    DeclaredConfigResolver.resolve(this)

    publishFeaturesToEdge(nats, cloudEventsPublisher)
    publishEventsToDacha2(nats, cloudEventsPublisher)
    publishEventsToDacha1(nats, cloudEventsPublisher)
  }

  fun publishFeaturesToEdge(nats: NATSSource, cloudEventsPublisher: CloudEventPublisher) {
    val natsChannel = nats.createPublisher(edgeChannelName!!)

    cloudEventsPublisher.registerForPublishing(PublishFeatureValues.CLOUD_EVENT_SUBJECT,
      CloudEventChannelMetric(CacheMetrics.featureFailureCounter, CacheMetrics.featureGram), true) { msg ->
      natsChannel.publish(msg)
    }
  }

  fun publishEventsToDacha1(nats: NATSSource, cloudEventsPublisher: CloudEventPublisher) {
    if (!Dacha2Config.isDacha2Enabled()) {
      // this ia a QUEUE not a TOPIC
      val natsChannel = nats.createPublisher(dacha1CloudEventsChannel!!)
      publishEnricherPing(cloudEventsPublisher, natsChannel)
    }
  }

  fun publishEventsToDacha2(nats: NATSSource, cloudEventsPublisher: CloudEventPublisher) {
    if (Dacha2Config.isDacha2Enabled()) {
      val natsChannel = nats.createPublisher(dachaChannelName!!)

      publishEnricherPing(cloudEventsPublisher, natsChannel)

      cloudEventsPublisher.registerForPublishing(
        PublishFeatureValues.CLOUD_EVENT_TYPE,
        CloudEventChannelMetric(CacheMetrics.featureFailureCounter, CacheMetrics.featureGram),
        true, natsChannel::publish)

      cloudEventsPublisher.registerForPublishing(
        PublishServiceAccount.CLOUD_EVENT_TYPE,
        CloudEventChannelMetric(CacheMetrics.serviceAccountCounter, CacheMetrics.serviceAccountsGram),
        true, natsChannel::publish)

      cloudEventsPublisher.registerForPublishing(
        PublishEnvironment.CLOUD_EVENT_TYPE,
        CloudEventChannelMetric(CacheMetrics.environmentCounter, CacheMetrics.environmentGram),
        true, natsChannel::publish)
    }
  }

  private fun publishEnricherPing(
    cloudEventsPublisher: CloudEventPublisher,
    natsChannel: NatsCloudEventsPublisher
  ) {
    cloudEventsPublisher.registerForPublishing(
      EnricherPing.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(CacheMetrics.featureFailureCounter, CacheMetrics.featureGram), false, natsChannel::publish
    )
  }
}


