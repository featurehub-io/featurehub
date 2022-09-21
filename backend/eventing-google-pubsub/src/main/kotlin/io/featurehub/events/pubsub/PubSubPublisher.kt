package io.featurehub.events.pubsub

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.cloud.pubsub.v1.Publisher
import com.google.cloud.pubsub.v1.PublisherInterface
import com.google.common.util.concurrent.MoreExecutors
import com.google.pubsub.v1.TopicName
import io.cloudevents.CloudEvent
import io.cloudevents.gpubsub.PubsubMessageFactory
import io.featurehub.jersey.config.CacheJsonMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface PubSubPublisher {
  fun publish(msg: CloudEvent)
}

class PubSubPublisherImpl(
  projectId: String,
  topicName: String,
  enricher: PubSubLocalEnricher,
) : PubSubPublisher {
  private val log: Logger = LoggerFactory.getLogger(PubSubPublisher::class.java)
  private val publisher: PublisherInterface
  private val publisherName: TopicName
  private val failureReceiver = PubSubStatusReporter(topicName)

  init {
    publisherName = TopicName.of(projectId, topicName)
    val newBuilder = Publisher.newBuilder(publisherName)
    enricher.enrichPublisherClient(newBuilder)
    publisher = newBuilder.build()
  }

  override fun publish(msg: CloudEvent) {
    if (log.isTraceEnabled) {
      log.trace("pubsub: publishing on {} : {}", publisherName, msg.toString())
    }

    val published = publisher.publish(
      PubsubMessageFactory.createWriter().writeBinary(msg)
    )

    // ensure we log failures
    ApiFutures.addCallback(published, failureReceiver, MoreExecutors.directExecutor())
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
