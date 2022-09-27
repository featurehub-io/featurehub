package io.featurehub.mr.events.dacha2.kinesis

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.events.kinesis.*
import io.featurehub.mr.events.common.CloudEventsDachaChannel
import io.featurehub.mr.events.common.CloudEventsEdgeChannel
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder
import java.util.*

class KinesisMRFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (!KinesisEventFeature.isEnabled()) return false

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(KinesisEdgeStream::class.java).to(CloudEventsEdgeChannel::class.java).`in`(Singleton::class.java)
        bind(KinesisDacha2Stream::class.java).to(CloudEventsDachaChannel::class.java).`in`(Singleton::class.java)
        bind(MrAppNameProvider::class.java).to(KinesisAppNameProvider::class.java).`in`(Singleton::class.java)
        bind(KinesisListenAllTheThings::class.java).to(KinesisListenAllTheThings::class.java).`in`(Immediate::class.java)
      }
    })

    return true
  }
}

class MrAppNameProvider : KinesisAppNameProvider {
  override fun name(): String = "management-repository"
}

class KinesisEdgeStream @Inject constructor(kinesisFactory: KinesisFactory) : CloudEventsEdgeChannel {
  @ConfigKey("cloudevents.mr-edge.kinesis.stream-name")
  private var streamName: String? = "featurehub-mr-edge"
  @ConfigKey("cloudevents.mr-edge.kinesis.randomise-partition-key")
  private var randomisePartitionKey: Boolean? = false

  private var publisher: KinesisCloudEventsPublisher
  val uniqueAppKinesisPartitionId = UUID.randomUUID().toString()

  init {
    DeclaredConfigResolver.resolve(this)

    publisher = kinesisFactory.makePublisher(streamName!!)
  }

  override fun encodePureJson(): Boolean = true

  private fun partitionKey() : String {
    return if (randomisePartitionKey == true) {
      UUID.randomUUID().toString()
    } else {
      uniqueAppKinesisPartitionId
    }
  }

  override fun publishEvent(event: CloudEvent) {
    publisher.publish(event, partitionKey() )
  }
}

class KinesisDacha2Stream @Inject constructor(kinesisFactory: KinesisFactory) : CloudEventsDachaChannel {
  @ConfigKey("cloudevents.mr-dacha2.kinesis.stream-name")
  private var streamName: String? = "featurehub-mr-dacha2"
  @ConfigKey("cloudevents.mr-dacha2.kinesis.randomise-partition-key")
  private var randomisePartitionKey: Boolean? = false

  private var publisher: KinesisCloudEventsPublisher
  val uniqueAppKinesisPartitionId = UUID.randomUUID().toString()

  init {
    DeclaredConfigResolver.resolve(this)

    publisher = kinesisFactory.makePublisher(streamName!!)
  }

  override fun dacha2Enabled(): Boolean = true

  override fun encodePureJson(): Boolean = true

  private fun partitionKey() : String {
    return if (randomisePartitionKey == true) {
      UUID.randomUUID().toString()
    } else {
      uniqueAppKinesisPartitionId
    }
  }

  override fun publishEvent(event: CloudEvent) {
    publisher.publish(event, partitionKey() )
  }
}
