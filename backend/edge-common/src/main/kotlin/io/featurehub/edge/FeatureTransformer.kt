package io.featurehub.edge

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.sse.model.FeatureState

interface FeatureTransformer {
  fun transform(features: List<CacheEnvironmentFeature>?, clientAttributes: ClientContext?, allowExtendedProperties: Boolean): List<FeatureState>
  fun transform(rf: CacheEnvironmentFeature, clientAttr: ClientContext?, allowExtendedProperties: Boolean): FeatureState
}
