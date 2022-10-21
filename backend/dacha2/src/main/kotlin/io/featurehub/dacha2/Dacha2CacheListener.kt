package io.featurehub.dacha2

import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount

interface Dacha2CacheListener {
  fun updateServiceAccount(serviceAccount: PublishServiceAccount)
  fun updateEnvironment(env: PublishEnvironment)
  fun updateFeature(feature: PublishFeatureValue)
}
