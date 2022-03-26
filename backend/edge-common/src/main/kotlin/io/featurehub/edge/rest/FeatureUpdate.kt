package io.featurehub.edge.rest

import com.fasterxml.jackson.core.JsonProcessingException
import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.edge.KeyParts
import io.featurehub.edge.stats.StatRecorder
import io.featurehub.edge.utils.UpdateMapper
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.dacha.model.DachaPermissionResponse
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RoleType
import io.featurehub.sse.model.FeatureStateUpdate
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import io.prometheus.client.Histogram
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.core.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*

interface FeatureUpdate {
  fun updateFeature(
    response: AsyncResponse, namedCache: String, envId: UUID, apiKey: String, featureKey: String,
    featureStateUpdate: FeatureStateUpdate, statRecorder: StatRecorder?)
}

interface FeatureUpdatePublisher {
  fun publishFeatureChangeRequest(featureUpdate: StreamedFeatureUpdate, namedCache: String)
}

class FeatureUpdateProcessor @Inject constructor(private val updateMapper: UpdateMapper,
                                                 private val dachaClientRegistry: DachaClientServiceRegistry,
                                                 private val featureUpdatePublisher: FeatureUpdatePublisher
) : FeatureUpdate {
  private val log: Logger = LoggerFactory.getLogger(FeatureUpdateProcessor::class.java)

  override fun updateFeature(
    response: AsyncResponse,
    namedCache: String,
    envId: UUID,
    apiKey: String,
    featureKey: String,
    featureStateUpdate: FeatureStateUpdate,
    statRecorder: StatRecorder?
  ) {
    val timer = testSpeedHistogram.startTimer()

    try {
      updateFeatureProcess(response, namedCache, envId, apiKey, featureKey, featureStateUpdate, statRecorder)
    } finally {
      timer.observeDuration()
    }
  }

  private fun requestPermission(key: KeyParts, featureKey: String): DachaPermissionResponse? {
    val apiKeyService: DachaApiKeyService = dachaClientRegistry.getApiKeyService(key.cacheName)

    return try {
      apiKeyService.getApiKeyPermissions(
        key.environmentId, key.serviceKey, featureKey
      )
    } catch (wae: WebApplicationException) {
      throw wae
    } catch (ignored: Exception) {
      null
    }
  }

  private fun updateFeatureProcess(response: AsyncResponse,
                                   namedCache: String,
                                   envId: UUID,
                                   apiKey: String,
                                   featureKey: String,
                                   featureStateUpdate: FeatureStateUpdate,
                                   statRecorder: StatRecorder?) {
    val key = KeyParts(namedCache, envId, apiKey)

    try {
      val perms = requestPermission(key, featureKey)
      if (perms == null) {
        statRecorder?.recordHit(key, EdgeHitResultType.MISSED, EdgeHitSourceType.TESTSDK)
        response.resume(Response.status(Response.Status.NOT_FOUND).build())
        return
      }

      key.applicationId = perms.applicationId
      key.organisationId = perms.organizationId
      key.portfolioId = perms.portfolioId
      key.serviceKeyId = perms.serviceKeyId

      if (perms.roles.isEmpty() || (perms.roles.size == 1 && perms.roles[0] == RoleType.READ)) {
        log.trace("FeatureUpdate failed: No permission to update")
        statRecorder?.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK)
        response.resume(Response.status(Response.Status.FORBIDDEN).entity("no write roles").build())
        return
      }
      if (true == featureStateUpdate.lock) {
        if (!perms.roles.contains(RoleType.LOCK)) {
          log.trace("FeatureUpdate failed: Feature is locked")
          statRecorder?.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK)
          response.resume(Response.status(Response.Status.FORBIDDEN).entity("feature locked").build())
          return
        }
      } else if (false == featureStateUpdate.lock) {
        if (!perms.roles.contains(RoleType.UNLOCK)) {
          log.trace("FeatureUpdate failed: feature is locked and no permission to unlock")
          statRecorder?.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK)
          response.resume(Response.status(Response.Status.FORBIDDEN).entity("feature locked, no permission to unlock").build())
          return
        }
      }
      if (featureStateUpdate.value != null) {
        featureStateUpdate.updateValue = true
      }

      // nothing to do?
      if (featureStateUpdate.lock == null && (featureStateUpdate.updateValue == null || false == featureStateUpdate.updateValue)) {
        log.trace("FeatureUpdate failed: not asking to lock or update value, so nothing to do")
        statRecorder?.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK)
        response.resume(Response.status(Response.Status.BAD_REQUEST).entity("lock and updateValue null, nothing to do").build())
        return
      }
      if (true == featureStateUpdate.updateValue) {
        if (!perms.roles.contains(RoleType.CHANGE_VALUE)) {
          log.trace("FeatureUpdate failed: No permission to change value")
          statRecorder?.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK)
          response.resume(Response.status(Response.Status.FORBIDDEN).entity("API Key has no change_value permission").build())
          return
        } else if (true == perms.feature.value?.locked && false != featureStateUpdate.lock) {
          // its locked, and you are trying to change its value and not unlocking it at the same time, that makes no
          // sense
          log.trace("FeatureUpdate failed: Attempting to change locked value without unlocking")
          statRecorder?.recordHit(key, EdgeHitResultType.UPDATE_NONSENSE, EdgeHitSourceType.TESTSDK)
          response.resume(Response.status(Response.Status.PRECONDITION_FAILED).entity("it is locked, cannot change value without also unlocking").build())
          return
        }
      }
      val upd = StreamedFeatureUpdate()
        .apiKey(apiKey)
        .environmentId(envId)
        .updatingValue(featureStateUpdate.updateValue ?: false)
        .lock(featureStateUpdate.lock)
        .featureKey(featureKey)

      // now update our internal value we will be sending, and also check
      // if aren't actually changing anything
      val value = perms.feature.value
      val lockChanging = upd.lock != null && upd.lock != value?.locked
      var valueNotActuallyChanging = false

      if (true == featureStateUpdate.updateValue) {
        valueNotActuallyChanging = if (featureStateUpdate.value != null) {
          val newValue = featureStateUpdate.value.toString()
          when (perms.feature.feature.valueType) {
            FeatureValueType.BOOLEAN -> {
              // must be true or false in some case
              if (!newValue.equals("true", ignoreCase = true) && !newValue.equals("false", ignoreCase = true)) {
                log.trace("FeatureUpdate failed: Attempting to change flag value to non true/false")
                statRecorder?.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK)
                response.resume(Response.status(Response.Status.BAD_REQUEST).entity("flag value must be true or false").build())
                return
              }

              upd.valueBoolean(newValue.toBoolean())
              upd.valueBoolean == value?.value
            }
            FeatureValueType.STRING -> {
              upd.valueString(newValue)
              upd.valueString == value?.value
            }
            FeatureValueType.JSON -> {
              try {
                updateMapper.mapper.readTree(newValue)
              } catch (jpe: JsonProcessingException) {
                log.trace("FeatureUpdate failed: Attempting to update JSON value to non-json")
                statRecorder?.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK)
                response.resume(Response.status(Response.Status.BAD_REQUEST).entity("Attempting to update JSON value to non-json").build())
                return
              }
              upd.valueString(newValue)
              upd.valueString == value?.value
            }
            FeatureValueType.NUMBER -> try {
              upd.valueNumber(BigDecimal(newValue))
              upd.valueNumber == value?.value
            } catch (e: Exception) {
              log.trace("FeatureUpdate failed: Attempting to update NUMBER value with non-NUMBER")
              statRecorder?.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK)
              response.resume(Response.status(Response.Status.BAD_REQUEST).entity("Attempting to update NUMBER value with non-NUMBER").build())
              return
            }
            else -> {
              log.trace("FeatureUpdate failed: {} is unknown", perms.feature.feature.valueType)
              response.resume(Response.status(Response.Status.BAD_REQUEST).entity("unknown value type").build())
              return
            }
          }
        } else {
          when (perms.feature.feature.valueType) {
            FeatureValueType.BOOLEAN -> {
              log.trace("FeatureUpdate failed: Cannot set flag value to null")
              // a null boolean is not valid
              statRecorder?.recordHit(key, EdgeHitResultType.UPDATE_NONSENSE, EdgeHitSourceType.TESTSDK)
              response.resume(Response.status(Response.Status.PRECONDITION_FAILED).entity("cannot set flag value to null").build())
              return
            }
            FeatureValueType.STRING, FeatureValueType.NUMBER, FeatureValueType.JSON -> value?.value == null
            else -> value?.value == null
          }
        }
      }

      if (valueNotActuallyChanging && !lockChanging) {
        statRecorder?.recordHit(key, EdgeHitResultType.UPDATE_NO_CHANGE, EdgeHitSourceType.TESTSDK)
        response.resume(Response.status(Response.Status.ACCEPTED).build())
        return
      }

      if (valueNotActuallyChanging) {
        upd.updatingValue(false)
        upd.valueBoolean(null)
        upd.valueNumber(null)
        upd.valueString(null)
      }

      log.debug("publishing update on {} for {}", namedCache, upd)

      upd.applicationId = perms.applicationId
      upd.organizationId = perms.organizationId
      upd.serviceKeyId = perms.serviceKeyId
      upd.portfolioId = perms.portfolioId

      featureUpdatePublisher.publishFeatureChangeRequest(upd, namedCache)

      statRecorder?.recordHit(key, EdgeHitResultType.SUCCESS, EdgeHitSourceType.TESTSDK)

      response.resume(Response.ok().build())
    } catch (e: Exception) {
      log.error("Failed to process request: {}/{}/{}/{} : {}", namedCache, envId, apiKey, featureKey,
        featureStateUpdate, e)
      statRecorder?.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK)
      response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("something happened, not sure what").build())
    }
  }

  companion object {
    val testSpeedHistogram: Histogram = Histogram.build("edge_conn_length_test", "The length of " +
    "time that the connection is open for Testing clients").register()
  }
}
