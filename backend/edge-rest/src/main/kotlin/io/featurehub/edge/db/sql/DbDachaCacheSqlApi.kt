package io.featurehub.edge.db.sql

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.dacha.api.DachaEnvironmentService
import io.featurehub.dacha.model.DachaStructureResponse
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.db.publish.CommonCacheGenerator
import io.featurehub.db.publish.FeatureModelWalker
import io.featurehub.db.services.Conversions
import jakarta.inject.Inject
import java.util.*

class DbDachaCacheSqlApi @Inject constructor(cacheSourceFeatureGroup: CacheSourceFeatureGroupApi,
                                             commonCacheGenerator: CommonCacheGenerator,
                                             featureModelWalker: FeatureModelWalker, conversions: Conversions
) : DachaClientServiceRegistry {
  private val apiKeyService = DbDachaSqlApi(cacheSourceFeatureGroup, featureModelWalker, commonCacheGenerator, conversions)

  override fun getEnvironmentService(cache: String): DachaEnvironmentService {
    return FakeDachaEnvironmentService() // is never called anyway
  }

  override fun getApiKeyService(cache: String): DachaApiKeyService =
    apiKeyService

  override fun registerApiKeyService(cache: String, apiKeyService: DachaApiKeyService) {
  }

  internal class FakeDachaEnvironmentService : DachaEnvironmentService {
    override fun getEnvironmentStructure(eId: UUID): DachaStructureResponse {
      return DachaStructureResponse()
    }
  }
}
