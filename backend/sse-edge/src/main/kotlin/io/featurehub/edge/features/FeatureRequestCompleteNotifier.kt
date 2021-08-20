package io.featurehub.edge.features

interface FeatureRequestCompleteNotifier {
  fun complete(key: FeatureRequesterSource)
}
