package io.featurehub.edge.permission

import com.fasterxml.jackson.core.JsonProcessingException
import io.featurehub.dacha.api.CacheJsonMapper
import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.edge.KeyParts
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.mr.model.DachaPermissionResponse
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PermissionPublishDelivery @Inject constructor(
  private val natsSource: NATSSource,
  private val dachaClientRegistry: DachaClientServiceRegistry
) : PermissionPublisher {
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

  override fun requestPermission(key: KeyParts, featureKey: String?): DachaPermissionResponse? {
    val apiKeyService: DachaApiKeyService = dachaClientRegistry.getApiKeyService(key.cacheName) ?: return null
    return try {
      apiKeyService.getApiKeyPermissions(
        key.environmentId, key.serviceKey, featureKey
      )
    } catch (ignored: Exception) {
      null
    }
  }
}
