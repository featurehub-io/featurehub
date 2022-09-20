package io.featurehub.events.pubsub

import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.cloud.pubsub.v1.SubscriberInterface
import com.google.protobuf.Message
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import io.cloudevents.CloudEvent
import io.cloudevents.gpubsub.PubsubMessageFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory


interface PubSubSubscriber {
  fun start()
  fun stop()
  fun isActive(): Boolean
  fun subscriptionName(): ProjectSubscriptionName
}

class PubSubscriberMessageReceiver(
  private val message: (msg: CloudEvent) -> Boolean,
) : MessageReceiver {
  private val log: Logger = LoggerFactory.getLogger(PubSubscriberMessageReceiver::class.java)

  override fun receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
    log.debug("pubsub: received message id: {} => {}", message.messageId, message.attributesMap)

    PubsubMessageFactory.createReader(message).toEvent()?.let {
      if (message(it)) {
        consumer.ack()
      } else {
        consumer.nack()
      }
    } ?: failed(message, consumer)
  }

  private fun failed(message: PubsubMessage, consumer: AckReplyConsumer) {
    log.error("received a message we could not decode {}", message)
    consumer.nack()
  }
}

class PubSubSubscriberImpl(
  projectId: String, subscriptionId: String,
  receiver: MessageReceiver,
  enricher: PubSubLocalEnricher,
) : PubSubSubscriber {
  private val log: Logger = LoggerFactory.getLogger(PubSubSubscriber::class.java)
  private var subscriber: SubscriberInterface?
  private val subName: ProjectSubscriptionName

  init {
    subName = ProjectSubscriptionName.of(projectId, subscriptionId)

    log.info("attempting to listen to {}", subName)

    val newBuilder = Subscriber.newBuilder(subName, receiver)

    enricher.enrichSubscriberClient(newBuilder)

    subscriber = newBuilder.build()
  }

  override fun start() {
    if (subscriber == null) {
      log.error("Attempting to start {} and it is not initialized", subName)
    } else {
      log.error("Starting {} and it is not initialized", subName)
      subscriber!!.startAsync()
    }
  }

  override fun stop() {
    if (subscriber == null) {
      log.error("Attempting to stop {} and it is not initialized", subName)
    } else {
      log.error("Stopping {} and it is not initialized", subName)
      subscriber?.stopAsync()
    }

    subscriber = null
  }

  override fun isActive(): Boolean {
    if (subscriber == null) {
      return false
    }

    val running = subscriber!!.isRunning
    if (!running) {
      log.warn("Subscriber {} should be running and is in {} state",
        subName, subscriber!!.state())

      try {
        start() // try and make it start again
      } catch (e: Exception) {
        log.warn("Subscriber {} could not start {}", subName, e.message) // could be already starting
      }
    }

    return running
  }

  override fun subscriptionName(): ProjectSubscriptionName {
    return subName
  }
}
