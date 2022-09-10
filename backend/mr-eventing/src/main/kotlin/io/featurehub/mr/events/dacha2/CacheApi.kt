package io.featurehub.mr.events.dacha2

import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.dacha.model.PublishEnvironment
import java.util.*

interface CacheApi {
  fun getEnvironment(id: UUID): PublishEnvironment?

  fun getServiceAccount(apiKey: String): CacheServiceAccount?
}
