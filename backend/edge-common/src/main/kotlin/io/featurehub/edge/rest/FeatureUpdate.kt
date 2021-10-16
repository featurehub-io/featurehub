package io.featurehub.edge.rest

import com.fasterxml.jackson.core.JsonProcessingException
import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.edge.KeyParts
import io.featurehub.edge.stats.StatRecorder
import io.featurehub.edge.utils.UpdateMapper
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.mr.model.DachaPermissionResponse
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RoleType
import io.featurehub.sse.model.FeatureStateUpdate
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import io.prometheus.client.Histogram
import jakarta.inject.Inject
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
    val apiKeyService: DachaApiKeyService = dachaClientRegistry.getApiKeyService(key.cacheName) ?: return null
    return try {
      apiKeyService.getApiKeyPermissions(
        key.environmentId, key.serviceKey, featureKey
      )
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
        statRecorder?.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK)
        response.resume(Response.status(Response.Status.FORBIDDEN).build())
        return
      }
      if (true == featureStateUpdate.lock) {
        if (!perms.roles.contains(RoleType.LOCK)) {
          statRecorder?.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK)
          response.resume(Response.status(Response.Status.FORBIDDEN).build())
          return
        }
      } else if (false == featureStateUpdate.lock) {
        if (!perms.roles.contains(RoleType.UNLOCK)) {
          statRecorder?.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK)
          response.resume(Response.status(Response.Status.FORBIDDEN).build())
          return
        }
      }
      if (featureStateUpdate.value != null) {
        featureStateUpdate.updateValue = true
      }

      // nothing to do?
      if (featureStateUpdate.lock == null && (featureStateUpdate.updateValue == null || false == featureStateUpdate.updateValue)) {
        statRecorder?.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK)
        response.resume(Response.status(Response.Status.BAD_REQUEST).build())
        return
      }
      if (true == featureStateUpdate.updateValue) {
        if (!perms.roles.contains(RoleType.CHANGE_VALUE)) {
          statRecorder?.recordHit(key, EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.TESTSDK)
          response.resume(Response.status(Response.Status.FORBIDDEN).build())
          return
        } else if (true == perms.feature.value.locked && false != featureStateUpdate.lock) {
          // its locked, and you are trying to change its value and not unlocking it at the same time, that makes no
          // sense
          statRecorder?.recordHit(key, EdgeHitResultType.UPDATE_NONSENSE, EdgeHitSourceType.TESTSDK)
          response.resume(Response.status(Response.Status.PRECONDITION_FAILED).build())
          return
        }
      }
      val upd = StreamedFeatureUpdate()
        .apiKey(apiKey)
        .environmentId(envId)
        .updatingValue(featureStateUpdate.updateValue!!)
        .lock(featureStateUpdate.lock)
        .featureKey(featureKey)

      // now update our internal value we will be sending, and also check
      // if aren't actually changing anything
      val value = perms.feature.value
      val lockChanging = upd.lock != null && upd.lock != value.locked
      var valueNotActuallyChanging = false

      if (true == featureStateUpdate.updateValue) {
        valueNotActuallyChanging = if (featureStateUpdate.value != null) {
          val newValue = featureStateUpdate.value.toString()
          when (perms.feature.feature.valueType!!) {
            FeatureValueType.BOOLEAN -> {
              // must be true or false in some case
              if (!newValue.equals("true", ignoreCase = true) && !newValue.equals("false", ignoreCase = true)) {
                statRecorder?.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK)
                response.resume(Response.status(Response.Status.BAD_REQUEST).build())
                return
              }

              upd.valueBoolean(newValue.toBoolean())
              upd.valueBoolean == value.valueBoolean
            }
            FeatureValueType.STRING -> {
              upd.valueString(newValue)
              upd.valueString == value.valueString
            }
            FeatureValueType.JSON -> {
              try {
                updateMapper.mapper.readTree(newValue)
              } catch (jpe: JsonProcessingException) {
                statRecorder?.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK)
                response.resume(Response.status(Response.Status.BAD_REQUEST).build())
                return
              }
              upd.valueString(newValue)
              upd.valueString == value.valueJson
            }
            FeatureValueType.NUMBER -> try {
              upd.valueNumber(BigDecimal(newValue))
              upd.valueNumber == value.valueNumber
            } catch (e: Exception) {
              statRecorder?.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK)
              response.resume(Response.status(Response.Status.BAD_REQUEST).build())
              return
            }
          }
        } else {
          when (perms.feature.feature.valueType!!) {
            FeatureValueType.BOOLEAN -> {
              // a null boolean is not valid
              statRecorder?.recordHit(key, EdgeHitResultType.UPDATE_NONSENSE, EdgeHitSourceType.TESTSDK)
              response.resume(Response.status(Response.Status.PRECONDITION_FAILED).build())
              return
            }
            FeatureValueType.STRING -> value.valueString == null
            FeatureValueType.NUMBER -> value.valueNumber == null
            FeatureValueType.JSON -> value.valueJson == null
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
      featureUpdatePublisher.publishFeatureChangeRequest(upd, namedCache)
      statRecorder?.recordHit(key, EdgeHitResultType.SUCCESS, EdgeHitSourceType.TESTSDK)
      response.resume(Response.ok().build())
    } catch (e: Exception) {
      log.error("Failed to process request: {}/{}/{}/{} : {}", namedCache, envId, apiKey, featureKey,
        featureStateUpdate, e)
      statRecorder?.recordHit(key, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.TESTSDK)
      response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build())
    }
  }

  companion object {
    val testSpeedHistogram: Histogram = Histogram.build("edge_conn_length_test", "The length of " +
    "time that the connection is open for Testing clients").register()
  }
}
