package io.featurehub.db.listener

import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import java.util.*
import java.util.function.Function

interface FeatureUpdateBySDKApi {
    @Throws(RolloutStrategyValidator.InvalidStrategyCombination::class)
    fun updateFeatureFromTestSdk(
        sdkUrl: String,
        envId: UUID,
        featureKey: String,
        updatingValue: Boolean,
        updatingLock: Boolean,
        buildFeatureValue: Function<FeatureValueType, FeatureValue>
    )
}
