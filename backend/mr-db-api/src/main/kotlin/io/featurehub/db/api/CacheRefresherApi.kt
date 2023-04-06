package io.featurehub.db.api

import java.util.*

interface CacheRefresherApi {
  fun refreshPortfolios(portfolioIds: List<UUID>)
  fun refreshApplications(applicationIds: List<UUID>)
  fun refreshEntireDatabase()
}
