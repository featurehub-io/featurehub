package io.featurehub.mr.events.common.converter

import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.mr.model.RolloutStrategy

data class FeatureMessagingParameter(
  val featureValue: DbFeatureValue,
  val lockUpdate: SingleFeatureValueUpdate<Boolean>?,
  val defaultValueUpdate: SingleFeatureValueUpdate<String>?,
  val retiredUpdate: SingleFeatureValueUpdate<Boolean>?,
  val strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>?
)

interface FeatureMessagingConverter {
  fun toFeatureMessagingUpdate(featureMessagingParameter: FeatureMessagingParameter): FeatureMessagingUpdate

}
