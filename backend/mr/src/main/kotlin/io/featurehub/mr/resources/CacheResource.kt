package io.featurehub.mr.resources

import io.featurehub.db.api.CacheRefresherApi
import io.featurehub.mr.api.CacheServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.CacheRefreshRequest
import io.featurehub.mr.model.CacheRefreshResponse
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.core.SecurityContext

class CacheResource @Inject constructor(private val authManager: AuthManagerService, private val cacheRefresherApi: CacheRefresherApi) : CacheServiceDelegate {
  override fun cacheRefresh(
    cacheRefreshRequest: CacheRefreshRequest,
    securityContext: SecurityContext
  ): CacheRefreshResponse {
    // in open source the organisation admin is the superuser. This API must never be exposed in a multi-tenant
    // deployment, as refreshEntireDatabase() spans every organisation.
    if (!authManager.isOrgAdmin(authManager.from(securityContext))) {
      throw ForbiddenException()
    }

    cacheRefreshRequest.portfolioId?.let {
      cacheRefresherApi.refreshPortfolios(it)
    }

    cacheRefreshRequest.applicationId?.let {
      cacheRefresherApi.refreshApplications(it)
    }

    if (cacheRefreshRequest.allTheThings == true) {
      cacheRefresherApi.refreshEntireDatabase()
    }

    return CacheRefreshResponse()
  }
}
