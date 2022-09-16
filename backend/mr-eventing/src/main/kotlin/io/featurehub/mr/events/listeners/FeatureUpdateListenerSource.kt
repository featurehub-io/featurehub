package io.featurehub.db.publish

import io.featurehub.db.listener.FeatureUpdateBySDKApi
import io.featurehub.mr.events.common.listeners.EdgeUpdateListener
import io.featurehub.mr.events.common.listeners.EdgeUpdateListenerSource
import io.featurehub.mr.events.service.FeatureUpdateListener


class EdgeUpdateListenerFactory : EdgeUpdateListenerSource {
  override fun createListener(
    featureUpdateBySDKApi: FeatureUpdateBySDKApi
  ): EdgeUpdateListener {
    return FeatureUpdateListener(featureUpdateBySDKApi)
  }
}
