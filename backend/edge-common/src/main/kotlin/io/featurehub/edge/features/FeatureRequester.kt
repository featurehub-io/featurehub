package io.featurehub.edge.features

import io.featurehub.dacha.model.DachaKeyDetailsResponse
import io.featurehub.edge.KeyParts

interface FeatureRequester {
  val details: DachaKeyDetailsResponse?
  val failure: Exception?
  val key: KeyParts

  fun add(notifier: FeatureRequestCompleteNotifier)
}
