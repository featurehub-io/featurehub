package io.featurehub.edge.features

interface FeatureRequester {
  fun add(notifier: FeatureRequestCompleteNotifier)
}
