package io.featurehub.mr.dacha2.rest

import cd.connect.app.config.ConfigKey
import io.featurehub.dacha2.api.Dacha2Service
import io.featurehub.mr.events.dacha2.CacheApi
import io.featurehub.mr.model.Dacha2Environment
import io.featurehub.mr.model.Dacha2ServiceAccount
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import java.util.*

class Dacha2Resource @Inject constructor(private val cacheApi: CacheApi) : Dacha2Service {
  @ConfigKey("mr.dacha2.api-keys")
  var apiKeys: List<String> = emptyList()

  override fun getEnvironment(id: UUID, key: String?): Dacha2Environment {
    checkKey(key)

    val env = cacheApi.getEnvironment(id) ?: throw NotFoundException()

    return Dacha2Environment().env(env)
  }

  override fun getServiceAccount(id: String, key: String?): Dacha2ServiceAccount {
    checkKey(key)

    val serviceAccount = cacheApi.getServiceAccount(id) ?: throw NotFoundException()

    return Dacha2ServiceAccount().serviceAccount(serviceAccount)
  }

  private fun checkKey(key: String?) {
    // is this API protected by an API Key - typically only used when it is exposed on the public internet
    if (apiKeys.isNotEmpty() && key != null && !apiKeys.contains(key)) {
      throw ForbiddenException()
    }
  }
}
