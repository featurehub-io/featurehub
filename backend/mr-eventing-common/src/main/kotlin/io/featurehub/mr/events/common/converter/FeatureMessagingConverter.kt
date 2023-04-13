package io.featurehub.mr.events.common.converter

import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.mr.model.RolloutStrategy

interface FeatureMessagingConverter {
  fun toFeatureMessagingUpdate(
    featureValue: DbFeatureValue,
    lockUpdate: SingleFeatureValueUpdate<Boolean>?,
    defaultValueUpdate: SingleFeatureValueUpdate<String>?,
    retiredUpdate: SingleFeatureValueUpdate<Boolean>?,
    strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>?
  ): FeatureMessagingUpdate

}
