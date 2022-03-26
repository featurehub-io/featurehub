package io.featurehub.db.publish

import io.featurehub.db.listener.EdgeUpdateListener
import io.featurehub.db.listener.FeatureUpdateBySDKApi
import io.featurehub.db.listener.FeatureUpdateListener
import io.nats.client.Connection

interface EdgeUpdateListenerSource {
  fun createListener(namedCache: String, connection: Connection, featureUpdateBySDKApi: FeatureUpdateBySDKApi): EdgeUpdateListener
}

class EdgeUpdateListenerFactory : EdgeUpdateListenerSource {
  override fun createListener(
    namedCache: String,
    connection: Connection,
    featureUpdateBySDKApi: FeatureUpdateBySDKApi
  ): EdgeUpdateListener {
    return FeatureUpdateListener(namedCache, connection, featureUpdateBySDKApi)
  }
}
