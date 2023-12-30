package io.featurehub.messaging.service

import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.api.SingleNullableFeatureValueUpdate
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.events.DynamicCloudEventDestination
import io.featurehub.mr.model.RolloutStrategy

data class FeatureMessagingParameter(
  val featureValue: DbFeatureValue,
  val lockUpdate: SingleFeatureValueUpdate<Boolean>,
  val defaultValueUpdate: SingleNullableFeatureValueUpdate<String?>,
  val retiredUpdate: SingleFeatureValueUpdate<Boolean>,
  val strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>,
  val versionUpdate: SingleNullableFeatureValueUpdate<Long>
)

interface FeatureMessagingCloudEventPublisher {
  /**
   * This is set up by the initializer
   */
  fun setHooks(hooks: List<DynamicCloudEventDestination>)

  val isEnabled: Boolean

  fun publish(featureMessagingParameter: FeatureMessagingParameter)
}
