package io.featurehub.mr.resources

import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PortfolioRolloutStrategyApi
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.mr.api.PortfolioRolloutStrategyServiceDelegate
import io.featurehub.mr.model.CreatePortfolioRolloutStrategy
import io.featurehub.mr.model.CustomRolloutStrategyViolation
import io.featurehub.mr.model.PortfolioRolloutStrategy
import io.featurehub.mr.model.PortfolioRolloutStrategyList
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyValidationRequest
import io.featurehub.mr.model.RolloutStrategyValidationResponse
import io.featurehub.mr.model.RolloutStrategyViolation
import io.featurehub.mr.model.UpdatePortfolioRolloutStrategy
import io.featurehub.mr.utils.PortfolioUtils
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import java.util.*

class PortfolioRolloutStrategyResource @Inject
  constructor(private val portfolioRolloutStrategyApi: PortfolioRolloutStrategyApi,
              private val validator: RolloutStrategyValidator,
              private val portfolioUtils: PortfolioUtils) :
  PortfolioRolloutStrategyServiceDelegate {
  override fun createPortfolioStrategy(
    portfolioId: UUID,
    createPortfolioRolloutStrategy: CreatePortfolioRolloutStrategy,
    holder: PortfolioRolloutStrategyServiceDelegate.CreatePortfolioStrategyHolder,
    securityContext: SecurityContext
  ): PortfolioRolloutStrategy {
    val personId = portfolioUtils.portfolioStrategyCreateOrEdit(securityContext, portfolioId)

    try {
      return portfolioRolloutStrategyApi.createStrategy(portfolioId, createPortfolioRolloutStrategy, personId,
        Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged)
      ) ?: throw NotFoundException()
    } catch (e: PortfolioRolloutStrategyApi.DuplicateNameException) {
      throw WebApplicationException("Duplicate name", 409)
    }
  }

  override fun deletePortfolioStrategy(
    portfolioId: UUID,
    portfolioStrategyId: UUID,
    holder: PortfolioRolloutStrategyServiceDelegate.DeletePortfolioStrategyHolder,
    securityContext: SecurityContext
  ) {
    val personId = portfolioUtils.portfolioStrategyDelete(securityContext, portfolioId)
    if (!portfolioRolloutStrategyApi.archiveStrategy(portfolioId, portfolioStrategyId, personId)) {
      throw NotFoundException("No such strategy to delete")
    }
  }

  override fun getPortfolioStrategy(
    portfolioId: UUID,
    portfolioStrategyId: UUID,
    holder: PortfolioRolloutStrategyServiceDelegate.GetPortfolioStrategyHolder,
    securityContext: SecurityContext
  ): PortfolioRolloutStrategy {
    portfolioUtils.portfolioStrategyRead(securityContext, portfolioId)

    return portfolioRolloutStrategyApi.getStrategy(portfolioId, portfolioStrategyId,
      Opts().add(
        FillOpts.SimplePeople,
        holder.includeWhoChanged
      ).add(FillOpts.Usage, holder.includeUsage)) ?: throw NotFoundException()
  }

  override fun listPortfolioStrategies(
    portfolioId: UUID,
    holder: PortfolioRolloutStrategyServiceDelegate.ListPortfolioStrategiesHolder,
    securityContext: SecurityContext
  ): PortfolioRolloutStrategyList {
    portfolioUtils.portfolioStrategyRead(securityContext, portfolioId)
    return portfolioRolloutStrategyApi.listStrategies(
      portfolioId,
      holder.page ?: 0, holder.max ?: 20, holder.filter,
      true == holder.includeArchived,
      holder.sortOrder,
      Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged).add(FillOpts.Usage, holder.includeUsage)
    )
  }

  override fun portfolioStrategyValidate(
    portfolioId: UUID,
    req: RolloutStrategyValidationRequest,
    securityContext: SecurityContext
  ): RolloutStrategyValidationResponse {
    val validationFailure = validator.validateStrategies(null, req.customStrategies, req.sharedStrategies)
    return RolloutStrategyValidationResponse()
      .customStategyViolations(
        validationFailure.customStrategyViolations.entries
          .map { (key, value): Map.Entry<RolloutStrategy, Set<RolloutStrategyViolation>> ->
            CustomRolloutStrategyViolation().strategy(
              key
            ).violations(value.toList())
          })
      .violations(validationFailure.collectionViolationType.toList())
  }

  override fun updatePortfolioStrategy(
    portfolioId: UUID,
    portfolioStrategyId: UUID,
    updatePortfolioRolloutStrategy: UpdatePortfolioRolloutStrategy,
    holder: PortfolioRolloutStrategyServiceDelegate.UpdatePortfolioStrategyHolder,
    securityContext: SecurityContext
  ): PortfolioRolloutStrategy {
    val person = portfolioUtils.portfolioStrategyCreateOrEdit(securityContext, portfolioId)

    return try {
      portfolioRolloutStrategyApi.updateStrategy(
        portfolioId, portfolioStrategyId, updatePortfolioRolloutStrategy, person,
        Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged)
      ) ?: throw ForbiddenException()
    } catch (e: PortfolioRolloutStrategyApi.DuplicateNameException) {
      throw WebApplicationException("Duplicate name", 409)
    }
  }

}
