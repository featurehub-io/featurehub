package io.featurehub.mr.resources

import io.featurehub.db.api.CacheRefresherApi
import io.featurehub.mr.api.CacheServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.CacheRefreshRequest
import io.featurehub.mr.model.CacheRefreshResponse
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.core.SecurityContext
import java.util.*

class CacheResource @Inject constructor(private val authManager: AuthManagerService, private val cacheRefresherApi: CacheRefresherApi) : CacheServiceDelegate {
  override fun cacheRefresh(
    cacheRefreshRequest: CacheRefreshRequest,
    securityContext: SecurityContext
  ): CacheRefreshResponse {
    if (!authManager.isOrgAdmin(authManager.from(securityContext))) {
      throw ForbiddenException()
    }

    cacheRefreshRequest.portfolioId?.let {
      cacheRefresherApi.refreshPortfolios(it)
    }

    cacheRefreshRequest.applicationId?.let {
      cacheRefresherApi.refreshApplications(it)
    }

    cacheRefreshRequest.allTheThings?.let {
      cacheRefresherApi.refreshEntireDatabase()
    }

    return CacheRefreshResponse()
  }
}
