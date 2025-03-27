package io.featurehub.mr.utils

import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.Opts
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.Person
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class ApplicationUtils @Inject constructor(
  private val authManager: AuthManagerService,
  private val applicationApi: ApplicationApi
) {
  @JvmOverloads
  fun check(securityContext: SecurityContext, id: UUID, opts: Opts = Opts.empty()): ApplicationPermissionCheck {
    val current = authManager.from(securityContext)

    return check(current, id, opts)
  }

  @Throws(WebApplicationException::class)
  fun check(current: Person, id: UUID, opts: Opts): ApplicationPermissionCheck {
    val app = applicationApi.getApplication(id, opts) ?: throw NotFoundException()

    if (isSuperuserOrPortfolioAdmin(current.id!!.id, app.portfolioId!!)) {
      return ApplicationPermissionCheck(current, app)
    } else {
      throw ForbiddenException()
    }
  }

  @Throws(WebApplicationException::class)
  fun featureCreatorCheck(
    securityContext: SecurityContext,
    appId: UUID
  ): ApplicationPermissionCheck {
    val current = authManager.from(securityContext)

    if (!applicationApi.personIsFeatureCreator(appId, current.id!!.id)) {
      log.warn(
        "Attempt by person {} to edit features in application {}", current.id!!
          .id, appId
      )

      return check(current, appId, Opts.empty())
    } else {
      return ApplicationPermissionCheck(current, Application().id(appId))
    }
  }

  /**
   * This just checks to see if a person has the Editor/Delete permission and if not, throws an exception. A portfolio
   * admin or admin will always have it.
   *
   * @param securityContext
   * @param appId
   */
  fun featureEditorCheck(securityContext: SecurityContext, appId: UUID) {
    val current = authManager.from(securityContext)

    if (!applicationApi.personIsFeatureEditor(appId, current.id!!.id)) {
      log.warn(
        "Attempt by person {} to edt features in application {}", current.id!!
          .id, appId
      )

      check(current, appId, Opts.empty())
    }
  }

  fun applicationStrategyReadCheck(securityContext: SecurityContext, id: UUID): UUID {
    val current = authManager.from(securityContext)
    val personId = current.id!!.id

    if (isSuperuserOrPortfolioAdmin(personId, id) || applicationApi.personIsFeatureReader(id, personId) || applicationApi.personApplicationRoles(id, personId).isEmpty()) {
      return personId
    }

    throw ForbiddenException()
  }

  private fun appStrategyCheck(securityContext: SecurityContext, id: UUID, roles: Set<ApplicationRoleType>): UUID {
    val current = authManager.from(securityContext).id!!.id

    if (isSuperuserOrPortfolioAdmin(current, id) || applicationApi.personApplicationRoles(id, current).any { roles.contains(it) }) {
      return current
    }

    throw ForbiddenException()
  }

  private fun isSuperuserOrPortfolioAdmin(current: UUID, portfolioId: UUID): Boolean {
    return authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(portfolioId, current, null)
  }

  fun applicationStrategyCreate(securityContext: SecurityContext, id: UUID): UUID {
    return appStrategyCheck(securityContext, id, setOf(ApplicationRoleType.APP_STRATEGY_CREATE))
  }

  fun applicationStrategyEdit(securityContext: SecurityContext, id: UUID): UUID {
    return appStrategyCheck(securityContext, id, setOf(ApplicationRoleType.APP_STRATEGY_EDIT, ApplicationRoleType.APP_STRATEGY_EDIT_AND_DELETE))
  }

  fun applicationStrategyDelete(securityContext: SecurityContext, id: UUID): UUID {
    return appStrategyCheck(securityContext, id, setOf(ApplicationRoleType.APP_STRATEGY_EDIT_AND_DELETE))
  }

  fun featureReadCheck(securityContext: SecurityContext, id: UUID): Person {
    val current = authManager.from(securityContext)

    if (!applicationApi.personIsFeatureReader(id, current.id!!.id)) {
      throw ForbiddenException()
    }

    return current
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(ApplicationUtils::class.java)
  }
}
