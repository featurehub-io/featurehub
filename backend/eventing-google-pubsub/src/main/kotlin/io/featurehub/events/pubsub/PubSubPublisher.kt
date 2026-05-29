package io.featurehub.events.pubsub

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.cloud.pubsub.v1.Publisher
import com.google.common.util.concurrent.MoreExecutors
import com.google.pubsub.v1.TopicName
import io.cloudevents.CloudEvent
import io.cloudevents.gpubsub.PubsubMessageFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

interface PubSubPublisher {
  fun publish(msg: CloudEvent)
  fun shutdown()
}

class PubSubPublisherImpl(
  projectId: String,
  topicName: String,
  enricher: PubSubLocalEnricher,
) : PubSubPublisher {
  private val log: Logger = LoggerFactory.getLogger(PubSubPublisher::class.java)
  private val publisher: Publisher
  private val publisherName: TopicName
  private val failureReceiver = PubSubStatusReporter(topicName)

  init {
    publisherName = TopicName.of(projectId, topicName)
    val newBuilder = Publisher.newBuilder(publisherName)
    enricher.enrichPublisherClient(newBuilder)
    publisher = newBuilder.build()
  }

  override fun publish(msg: CloudEvent) {
    val cloudMessage = PubsubMessageFactory.createWriter().writeBinary(msg)

    if (log.isTraceEnabled) {
      log.trace("pubsub: publishing on {} : {} {} - {}", publisherName, msg.type, msg.subject, cloudMessage.attributesMap)
    }
    val published = publisher.publish(
      cloudMessage
    )

    // ensure we log failures
    ApiFutures.addCallback(published, failureReceiver, MoreExecutors.directExecutor())
  }

  override fun shutdown() {
    try {
      publisher.shutdown()
      publisher.awaitTermination(10, TimeUnit.SECONDS)
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      log.warn("pubsub: interrupted while waiting for publisher {} to terminate", publisherName)
    }
  }

  inner class PubSubStatusReporter(private val topicName: String) : ApiFutureCallback<String> {
    private val log: Logger = LoggerFactory.getLogger(PubSubStatusReporter::class.java)

    override fun onFailure(t: Throwable) {
      log.error("failed to publish on topic {}", topicName, t)
    }

    override fun onSuccess(result: String?) {
      log.trace("publish successful on topic {}: {}", topicName, result)
    }
  }
}
