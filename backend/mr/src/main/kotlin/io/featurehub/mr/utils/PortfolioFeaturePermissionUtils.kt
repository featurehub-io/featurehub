package io.featurehub.mr.utils

import io.featurehub.db.api.ApplicationApi
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Person
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import java.util.*

/**
 * Security helper for portfolio-level feature filter operations.
 * Write access requires portfolio/org admin OR feature creator/editor in any application in the portfolio.
 * Read access requires any application level role in any application in the portfolio.
 */
class PortfolioFeaturePermissionUtils @Inject constructor(
  private val authManager: AuthManagerService,
  private val applicationApi: ApplicationApi
) {
  /** Throws ForbiddenException unless user is portfolio/org admin or feature creator/editor in any app. */
  fun requireFeatureWriteAccessInPortfolio(portfolioId: UUID, current: Person) {
    if (authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(portfolioId, current)) return
    val personId = current.id!!.id
    if (applicationApi.personIsFeatureCreatorInPortfolio(portfolioId, personId)) return
    throw ForbiddenException()
  }

  /** Throws ForbiddenException unless user has at least read access to any app in the portfolio. */
  fun requireFeatureReadAccessInPortfolio(portfolioId: UUID, current: Person) {
    if (authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(portfolioId, current)) return
    val personId = current.id!!.id
    if (applicationApi.personIsFeatureReaderInPortfolio(portfolioId, personId)) return
    throw ForbiddenException()
  }
}
