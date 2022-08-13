package io.featurehub.dacha

import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount

interface CacheUpdateListener {

  fun updateServiceAccount(sa: PublishServiceAccount)

  fun updateEnvironment(e: PublishEnvironment)
  fun updateFeatureValue(fv: PublishFeatureValue)

}
