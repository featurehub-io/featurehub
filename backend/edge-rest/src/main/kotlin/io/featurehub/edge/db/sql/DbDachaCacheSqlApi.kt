package io.featurehub.edge.db.sql

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.dacha.api.DachaEnvironmentService
import io.featurehub.mr.model.DachaStructureResponse
import java.util.*

class DbDachaCacheSqlApi : DachaClientServiceRegistry {
  private val apiKeyService = DbDachaSqlApi()

  override fun getEnvironmentService(cache: String?): DachaEnvironmentService {
    return FakeDachaEnvironmentService() // is never called anyway
  }

  override fun getApiKeyService(cache: String): DachaApiKeyService =
    apiKeyService

  override fun registerApiKeyService(cache: String?, apiKeyService: DachaApiKeyService?) {
  }

  internal class FakeDachaEnvironmentService : DachaEnvironmentService {
    override fun getEnvironmentStructure(eId: UUID): DachaStructureResponse {
      return DachaStructureResponse()
    }
  }
}
