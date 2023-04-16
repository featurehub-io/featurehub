package io.featurehub.mr.events.common

import io.featurehub.mr.events.common.converter.FeatureMessagingParameter

interface FeatureMessagingCloudEventPublisher {
  fun publishFeatureMessagingUpdate(featureMessagingParameter: FeatureMessagingParameter)
}
