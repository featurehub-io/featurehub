package io.featurehub.mr.utils

import io.featurehub.db.api.GroupApi
import io.featurehub.db.services.Conversions
import io.featurehub.mr.auth.AuthManager
import io.featurehub.mr.model.PortfolioGroupRoleType
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.core.SecurityContext
import java.util.*

interface PortfolioUtils {
  fun formatPortfolioAdminGroupName(pfName: String?): String
  // return: personId or forbidden
  fun portfolioUserManager(user: SecurityContext, portfolioId: UUID): UUID
  fun portfolioStrategyCreateOrEdit(user: SecurityContext, portfolioId: UUID): UUID
  fun portfolioStrategyDelete(user: SecurityContext, portfolioId: UUID): UUID
  // do they have any role in any group in this portfolio, if so they can at least read the portfolio strategies
  fun portfolioStrategyRead(user: SecurityContext, portfolioId: UUID): UUID
}

class PortfolioUtilsImpl @Inject constructor(private val authManager: AuthManager, private val conversions: Conversions, private val groupApi: GroupApi): PortfolioUtils {
  private val portfolioAdminGroupSuffix = FallbackPropertyConfig.getConfig("portfolio.admin.group.suffix", "Administrators")

  companion object {
    private val portfolioStrategyEditor =
      setOf(PortfolioGroupRoleType.PORTFOLIO_STRATEGY_EDIT, PortfolioGroupRoleType.PORTFOLIO_STRATEGY_EDIT_AND_DELETE)
    private val portfolioStrategyDelete = setOf(PortfolioGroupRoleType.PORTFOLIO_STRATEGY_EDIT_AND_DELETE)
    private val portfolioGroupMemberManager = setOf(PortfolioGroupRoleType.GROUP_MEMBER_MANAGER)
  }

  override fun formatPortfolioAdminGroupName(pfName: String?): String {
    return String.format("%s %s", pfName, portfolioAdminGroupSuffix)
  }

  override fun portfolioUserManager(
    user: SecurityContext,
    portfolioId: UUID
  ): UUID {
    return portfolioGroupRole(user, portfolioId, portfolioGroupMemberManager)
  }

  override fun portfolioStrategyCreateOrEdit(
    user: SecurityContext,
    portfolioId: UUID
  ): UUID {
    return portfolioGroupRole(user, portfolioId, portfolioStrategyEditor)
  }

  override fun portfolioStrategyDelete(
    user: SecurityContext,
    portfolioId: UUID
  ): UUID {
    return portfolioGroupRole(user, portfolioId, portfolioStrategyDelete)
  }

  private fun portfolioGroupRole(user: SecurityContext, portfolioId: UUID, roles: Set<PortfolioGroupRoleType>): UUID {
    val personId = authManager.from(user).id!!.id

    if (conversions.personIsSuperAdmin(personId) ||
      conversions.isPersonMemberOfPortfolioAdminGroup(portfolioId, personId) ||
      groupApi.portfolioRoles(personId, portfolioId).intersect(roles).isNotEmpty()) return personId

    throw ForbiddenException()
  }

  override fun portfolioStrategyRead(
    user: SecurityContext,
    portfolioId: UUID
  ): UUID {
    val personId = authManager.from(user).id!!.id

    if (conversions.personIsSuperAdmin(personId) ||
      conversions.isPersonMemberOfPortfolioAdminGroup(portfolioId, personId) ||
        groupApi.isPersonMemberOfAnyPortfolioGroup(portfolioId, personId)) return personId

    throw ForbiddenException()
  }
}
