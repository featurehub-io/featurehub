package io.featurehub.mr.events.common.listeners

import io.featurehub.db.listener.FeatureUpdateBySDKApi

interface EdgeUpdateListenerSource {
  fun createListener(featureUpdateBySDKApi: FeatureUpdateBySDKApi): EdgeUpdateListener
}
