package io.featurehub.messaging.service

import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.events.CloudEventPublisher
import io.featurehub.messaging.MessagingConfig
import io.featurehub.messaging.converter.FeatureMessagingConverter
import io.featurehub.messaging.converter.FeatureMessagingParameter
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.utils.ExecutorSupplier
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ExecutorService

class FeatureMessagingCloudEventPublisherImpl @Inject constructor(
  private val featureMessagingConverter: FeatureMessagingConverter,
  private val cloudEventPublisher: CloudEventPublisher,
  executorSupplier: ExecutorSupplier
) : FeatureMessagingCloudEventPublisher {
  private val executor: ExecutorService

  init {
    DeclaredConfigResolver.resolve(this)
    executor = executorSupplier.executorService(MessagingConfig.threadPoolSize())
  }

  private val log = LoggerFactory.getLogger(FeatureMessagingCloudEventPublisherImpl::class.java)
  override fun publishFeatureMessagingUpdate(featureMessagingParameter: FeatureMessagingParameter) {
    if (!MessagingConfig.isEnabled())
      return

    log.trace("publishing feature messaging update for {}", featureMessagingParameter)
    executor.submit {
      try {
        val featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(featureMessagingParameter)
        val event = CloudEventBuilder.v1().newBuilder()
        event.withSubject(FeatureMessagingUpdate.CLOUD_EVENT_SUBJECT)
        event.withId(UUID.randomUUID().toString())
        event.withType(FeatureMessagingUpdate.CLOUD_EVENT_TYPE)
        event.withSource(URI("http://mr"))
        event.withTime(OffsetDateTime.now())
        cloudEventPublisher.publish(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, featureMessagingUpdate, event)
      } catch (e: Exception) {
        log.error("Failed to publish messaging update for feature {}", featureMessagingParameter.featureValue, e)
      }
    }
  }
}
