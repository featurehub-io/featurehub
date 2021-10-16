package io.featurehub.edge.features

import io.featurehub.edge.KeyParts
import io.featurehub.mr.model.DachaKeyDetailsResponse

interface FeatureRequester {
  val details: DachaKeyDetailsResponse?
  val key: KeyParts

  fun add(notifier: FeatureRequestCompleteNotifier)
}
