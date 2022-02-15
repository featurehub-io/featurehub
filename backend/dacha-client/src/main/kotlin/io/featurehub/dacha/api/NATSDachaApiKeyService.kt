package io.featurehub.dacha.api

import io.featurehub.dacha.model.DachaKeyDetailsResponse
import io.featurehub.dacha.model.DachaPermissionResponse
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.model.DachaKeyDetailsRequest
import io.featurehub.mr.model.DachaNATSRequest
import io.featurehub.mr.model.DachaNATSResponse
import io.featurehub.mr.model.DachaPermissionRequest
import io.featurehub.publish.ChannelConstants
import io.featurehub.publish.ChannelNames
import io.featurehub.publish.NATSSource
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

class NATSDachaApiKeyService constructor(private val nats: NATSSource, val cacheName: String, private val connectionTimeout: Long): DachaApiKeyService {
  private val log: Logger = LoggerFactory.getLogger(NATSDachaApiKeyService::class.java)
  private val subjectName: String = ChannelNames.cache(cacheName, ChannelConstants.EDGE_CACHE_CHANNEL)

  override fun getApiKeyDetails(eId: UUID, serviceAccountKey: String, excludeRetired: Boolean?): DachaKeyDetailsResponse {
    try {
      val msg = nats.connection.request(
          subjectName,
          CacheJsonMapper.writeAsZipBytes(
            DachaNATSRequest().featuresRequest(
              DachaKeyDetailsRequest().serviceAccountKey(
                serviceAccountKey
              ).eId(eId).excludeRetired(excludeRetired)
            )
          ),
          Duration.of(connectionTimeout, ChronoUnit.MILLIS)
      ) ?: throw WebApplicationException(Response.status(412).entity("Dacha not ready").build())

      val response = CacheJsonMapper.readFromZipBytes(msg.data, DachaNATSResponse::class.java)

      if (response.status != 200 || response.featuresResponse == null) {
        throw WebApplicationException(Response.status(response.status).build())
      }

      return response.featuresResponse!!
    } catch (we: WebApplicationException) {
      throw we
    } catch (e : Exception) {
      log.error("Unable to request key {} : {}", eId, serviceAccountKey, e)
      throw InternalServerErrorException()
    }
  }

  override fun getApiKeyPermissions(eId: UUID, serviceAccountKey: String, featureKey: String): DachaPermissionResponse {
    try {
      val msg = nats.connection.request(
        subjectName,
        CacheJsonMapper.writeAsZipBytes(
          DachaNATSRequest().permissionRequest(
            DachaPermissionRequest().serviceAccountKey(
              serviceAccountKey
            ).eId(eId).featureKey(featureKey)
          )
        ),
        Duration.of(connectionTimeout, ChronoUnit.MILLIS)
      )

      val response = CacheJsonMapper.readFromZipBytes(msg.data, DachaNATSResponse::class.java)

      if (response.status != 200 || response.permissionResponse == null) {
        throw WebApplicationException(Response.status(response.status).build())
      }

      return response.permissionResponse!!
    } catch (e : Exception) {
      log.error("Unable to request key {} : {}", eId, serviceAccountKey, e)
      throw InternalServerErrorException()
    }
  }

}
