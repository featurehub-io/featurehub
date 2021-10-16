package io.featurehub.edge.permission

import com.fasterxml.jackson.core.JsonProcessingException
import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.edge.KeyParts
import io.featurehub.edge.rest.FeatureUpdatePublisher
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.mr.model.DachaPermissionResponse
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PermissionPublishDelivery @Inject constructor(
  private val natsSource: NATSSource,
) : FeatureUpdatePublisher {
  private val log: Logger = LoggerFactory.getLogger(PermissionPublishDelivery::class.java)

  override fun publishFeatureChangeRequest(featureUpdate: StreamedFeatureUpdate, namedCache: String) {
    // oh for asyncapi being actually useful
    val subject = "/$namedCache/feature-updates"

    try {
      natsSource
        .connection
        .publish(subject, CacheJsonMapper.mapper.writeValueAsBytes(featureUpdate))
    } catch (e: JsonProcessingException) {
      log.error("Unable to send feature-update message to server")
    }
  }
}
