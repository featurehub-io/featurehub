package io.featurehub.db.messaging

import io.cloudevents.CloudEvent
import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.api.SingleNullableFeatureValueUpdate
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.mr.model.RolloutStrategy

data class FeatureMessagingParameter(
  val featureValue: DbFeatureValue,
  val lockUpdate: SingleFeatureValueUpdate<Boolean>,
  val defaultValueUpdate: SingleNullableFeatureValueUpdate<String?>,
  val retiredUpdate: SingleFeatureValueUpdate<Boolean>,
  val strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>,
  val versionUpdate: SingleNullableFeatureValueUpdate<Long>
)

interface FeatureMessagingPublisher {
  /**
   * Sets off the chain in motion of publishing a cloud event. This sets it all up, creates all the
   * data and forms a cloud event that it subsequently sends back to the caller, before proceeding to
   * send it to downstream systems.
   */
  fun publish(featureMessagingParameter: FeatureMessagingParameter)
}
