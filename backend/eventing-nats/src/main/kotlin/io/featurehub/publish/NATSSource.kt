package io.featurehub.publish

import io.cloudevents.CloudEvent
import io.featurehub.events.nats.NatsCloudEventsPublisher
import io.featurehub.events.nats.NatsListener
import io.nats.client.Connection

interface NATSSource {
  val connection: Connection

  fun createTopicListener(subject: String, handler: (event: CloudEvent) -> Unit) : NatsListener
  fun createQueueListener(subject: String, queue: String, handler: (event: CloudEvent) -> Unit) : NatsListener

  fun createPublisher(subject: String): NatsCloudEventsPublisher

}
