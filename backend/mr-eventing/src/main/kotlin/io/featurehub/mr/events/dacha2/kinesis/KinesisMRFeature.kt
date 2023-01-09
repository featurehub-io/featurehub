package io.featurehub.mr.events.dacha2.kinesis

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.enriched.model.EnricherPing
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.kinesis.*
import io.featurehub.mr.events.common.CacheMetrics
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
        bind(KinesisMROutboundStream::class.java).to(KinesisMROutboundStream::class.java).`in`(Immediate::class.java)
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

class KinesisMROutboundStream @Inject constructor(kinesisFactory: KinesisFactory, cloudEventsPublisher: CloudEventPublisher) {
  @ConfigKey("cloudevents.outbound.kinesis.stream-name")
  private var streamName: String? = "featurehub-mr-stream"
  @ConfigKey("cloudevents.outbound.kinesis.randomise-partition-key")
  private var randomisePartitionKey: Boolean? = false

  private var publisher: KinesisCloudEventsPublisher
  val uniqueAppKinesisPartitionId = UUID.randomUUID().toString()

  init {
    DeclaredConfigResolver.resolve(this)

    publisher = kinesisFactory.makePublisher(streamName!!)

    cloudEventsPublisher.registerForPublishing(
      EnricherPing.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(CacheMetrics.featureFailureCounter, CacheMetrics.featureGram), false, this::publishEvent)

    cloudEventsPublisher.registerForPublishing(
      PublishFeatureValues.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(CacheMetrics.featureFailureCounter, CacheMetrics.featureGram), false, this::publishEvent)

    cloudEventsPublisher.registerForPublishing(
      PublishServiceAccount.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(CacheMetrics.serviceAccountCounter, CacheMetrics.serviceAccountsGram), false, this::publishEvent)

    cloudEventsPublisher.registerForPublishing(
      PublishEnvironment.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(CacheMetrics.environmentCounter, CacheMetrics.environmentGram), false, this::publishEvent)
  }

  private fun partitionKey() : String {
    return if (randomisePartitionKey == true) {
      UUID.randomUUID().toString()
    } else {
      uniqueAppKinesisPartitionId
    }
  }

   private fun publishEvent(event: CloudEvent) {
     publisher.publish(event, partitionKey() )
  }
}
