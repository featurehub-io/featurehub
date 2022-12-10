package io.featurehub.edge.events.kinesis

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.edge.events.EdgeSubscriber
import io.featurehub.edge.events.StreamingEventPublisher
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.kinesis.KinesisFactory
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.webhook.common.WebhookCommonFeature
import io.featurehub.webhook.events.WebhookEnvironmentResult
import io.features.webhooks.features.WebhookFeature
import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder
import java.util.*

class KinesisEdgeFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (!KinesisEventFeature.isEnabled()) return false

    context.register(KinesisEventFeature::class.java)
    context.register(object: AbstractBinder() {
      override fun configure() {
        // start listening immediately on wiring finishing
        bind(KinesisFeaturesListener::class.java).to(KinesisFeaturesListener::class.java).`in`(Immediate::class.java)
        bind(KinesisFeatureUpdatePublisher::class.java).to(KinesisFeatureUpdatePublisher::class.java).`in`(Immediate::class.java)
      }
    })

    return true
  }
}

class KinesisFeaturesListener @Inject constructor(
  private val controller: EdgeSubscriber,
  kinesisFactory: KinesisFactory
) {
  @ConfigKey("cloudevents.mr-edge.kinesis.stream-name")
  private var edgeTopicName: String? = "featurehub-mr-edge"

  init {
    DeclaredConfigResolver.resolve(this)

    kinesisFactory.makeSubscriber("edge-" + UUID.randomUUID(), edgeTopicName!!) { event ->
      controller.process(event)
    }
  }
}

class KinesisFeatureUpdatePublisher @Inject constructor(kinesisFactory: KinesisFactory, cloudEventPublisher: CloudEventPublisher) {
  @ConfigKey("cloudevents.edge-mr.kinesis.stream-name")
  private val updateStreamName: String = "featurehub-edge-updates"
  @ConfigKey("cloudevents.edge-mr.kinesis.randomise")
  private var randomise: Boolean? = true

  init {
    DeclaredConfigResolver.resolve(this)

    val publisher = kinesisFactory.makePublisher(updateStreamName)

    cloudEventPublisher.registerForPublishing(
      StreamedFeatureUpdate.CLOUD_EVENT_TYPE,
      StreamingEventPublisher.channelMetric, false) { msg ->
      publisher.publish(msg, publishKey())
    }

    // if we are registered for webhooks, send the results back on the same channel
    if (WebhookFeature.enabled) {
      cloudEventPublisher.registerForPublishing(
        WebhookEnvironmentResult.CLOUD_EVENT_TYPE,
        WebhookCommonFeature.channelMetric, false) { msg ->
        publisher.publish(msg, publishKey())
      }
    }
  }

  private fun publishKey() = if (randomise!!) UUID.randomUUID().toString() else "edge-client"

}
