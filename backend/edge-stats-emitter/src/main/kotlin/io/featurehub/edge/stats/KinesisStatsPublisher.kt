package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.events.kinesis.KinesisCloudEventsPublisher
import io.featurehub.events.kinesis.KinesisFactory
import jakarta.inject.Inject
import java.util.*

class KinesisStatsPublisher @Inject constructor(kinesisFactory: KinesisFactory): CloudEventStatPublisher {
  @ConfigKey("cloudevents.stats.kinesis.stream-name")
  var streamName: String? = "featurehub-stats"

  private var publisher: KinesisCloudEventsPublisher

  init {
    DeclaredConfigResolver.resolve(this)

    publisher = kinesisFactory.makePublisher(streamName!!)
  }

  override fun encodeAsJson(): Boolean = true  // only binary mode for kinesis, so no point in gzipping and mime encoding

  override fun publish(event: CloudEvent) {
    publisher.publish(event, UUID.randomUUID().toString())
  }
}
