package io.featurehub.mr.events.common.listeners

import io.featurehub.db.api.RolloutStrategyValidator.InvalidStrategyCombination
import io.featurehub.db.listener.FeatureUpdateBySDKApi
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface FeatureUpdateListener {
  fun processUpdate(update: StreamedFeatureUpdate)
}

open class FoundationFeatureUpdateListenerImpl @Inject constructor(
  private val featureUpdateBySDKApi: FeatureUpdateBySDKApi,
) : FeatureUpdateListener {
  override fun processUpdate(update: StreamedFeatureUpdate) {
    try {
      log.debug("received update {}", update)
      featureUpdateBySDKApi.updateFeatureFromTestSdk(
        update.apiKey, update.environmentId, update.featureKey,  update.updatingValue ?: false, update.lock != null
      ) { valueType: FeatureValueType? ->
        val fv = FeatureValue()
          .key(update.featureKey)
          .locked(update.lock != null && update.lock!!)

        update.updatingValue?.let { amUpdatingValue ->
          if (amUpdatingValue) {
            when (valueType) {
              FeatureValueType.BOOLEAN -> fv.valueBoolean(update.valueBoolean)
              FeatureValueType.STRING -> fv.valueString(update.valueString)
              FeatureValueType.NUMBER -> fv.valueNumber = update.valueNumber
              FeatureValueType.JSON -> fv.valueJson(update.valueString)
              else -> {
              }
            }
          }
        }

        fv
      }
    } catch (ignoreEx: InvalidStrategyCombination) {
      // ignore
    } catch (failed: Exception) {
      log.error("Failed to update {}", update, failed)
    }
  }
  companion object {
    private val log: Logger = LoggerFactory.getLogger(FoundationFeatureUpdateListenerImpl::class.java)
  }
}
