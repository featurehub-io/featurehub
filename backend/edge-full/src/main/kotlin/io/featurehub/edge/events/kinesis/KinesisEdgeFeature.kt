package io.featurehub.edge.events.kinesis

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.edge.events.CloudEventsEdgePublisher
import io.featurehub.edge.events.EdgeSubscriber
import io.featurehub.events.kinesis.KinesisCloudEventsPublisher
import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.kinesis.KinesisFactory
import jakarta.inject.Inject
import jakarta.inject.Singleton
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
        bind(KinesisFeatureUpdatePublisher::class.java).to(CloudEventsEdgePublisher::class.java).`in`(Singleton::class.java)
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

class KinesisFeatureUpdatePublisher @Inject constructor(kinesisFactory: KinesisFactory) : CloudEventsEdgePublisher {
  @ConfigKey("cloudevents.edge-mr.kinesis.stream-name")
  private val updateStreamName: String = "featurehub-edge-updates"
  private var publisher: KinesisCloudEventsPublisher

  init {
    DeclaredConfigResolver.resolve(this)

    publisher = kinesisFactory.makePublisher(updateStreamName)
  }

  override fun encodeAsJson() = false

  override fun publish(event: CloudEvent) {
    publisher.publish(event, UUID.randomUUID().toString()) // scatter gun them across the listeners
  }
}
