package io.featurehub.mr.resources

import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.RolloutStrategyApi
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.mr.api.RolloutStrategyServiceDelegate
import io.featurehub.mr.model.*
import io.featurehub.mr.utils.ApplicationUtils
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.*

class RolloutStrategyResource @Inject constructor(
  private val applicationUtils: ApplicationUtils,
  private val rolloutStrategyApi: RolloutStrategyApi, private val validator: RolloutStrategyValidator
) : RolloutStrategyServiceDelegate {
  override fun createRolloutStrategy(
    appId: UUID, rolloutStrategy: RolloutStrategy,
    holder: RolloutStrategyServiceDelegate.CreateRolloutStrategyHolder,
    securityContext: SecurityContext
  ): RolloutStrategyInfo {
    val person = applicationUtils.featureCreatorCheck(securityContext, appId).current
    cleanStrategy(rolloutStrategy)
    val strategy: RolloutStrategyInfo?
    strategy = try {
      rolloutStrategyApi.createStrategy(
        appId, rolloutStrategy, person,
        Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged)
      )
    } catch (e: RolloutStrategyApi.DuplicateNameException) {
      throw WebApplicationException("Duplicate name", 409)
    }
    if (strategy == null) {
      throw ForbiddenException()
    }
    return strategy
  }

  override fun deleteRolloutStrategy(
    appId: UUID, strategyIdOrName: String, holder: RolloutStrategyServiceDelegate.DeleteRolloutStrategyHolder,
    securityContext: SecurityContext
  ): RolloutStrategyInfo {
    val person = applicationUtils.featureCreatorCheck(securityContext, appId).current
    return rolloutStrategyApi.archiveStrategy(
      appId, strategyIdOrName, person,
      Opts().add(
        FillOpts.SimplePeople,
        holder.includeWhoChanged
      )
    ) ?: throw NotFoundException()
  }

  override fun getRolloutStrategy(
    appId: UUID, strategyIdOrName: String, holder: RolloutStrategyServiceDelegate.GetRolloutStrategyHolder,
    securityContext: SecurityContext
  ): RolloutStrategyInfo {
    applicationUtils.featureReadCheck(securityContext, appId)
    return rolloutStrategyApi.getStrategy(
      appId, strategyIdOrName, Opts().add(
        FillOpts.SimplePeople,
        holder.includeWhoChanged
      )
    ) ?: throw NotFoundException()
  }

  override fun listApplicationRolloutStrategies(
    appId: UUID,
    holder: RolloutStrategyServiceDelegate.ListApplicationRolloutStrategiesHolder,
    securityContext: SecurityContext
  ): List<RolloutStrategyInfo> {
    applicationUtils.featureReadCheck(securityContext, appId)
    return rolloutStrategyApi.listStrategies(
      appId,
      true == holder.includeArchived,
      Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged)
    )
      ?: throw NotFoundException()
  }

  override fun updateRolloutStrategy(
    appId: UUID, strategyIdOrName: String, rolloutStrategy: RolloutStrategy,
    holder: RolloutStrategyServiceDelegate.UpdateRolloutStrategyHolder,
    securityContext: SecurityContext
  ): RolloutStrategyInfo {
    val person = applicationUtils.featureCreatorCheck(securityContext, appId).current

    cleanStrategy(rolloutStrategy)

    return try {
      rolloutStrategyApi.updateStrategy(
        appId, rolloutStrategy, person,
        Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged)
      ) ?: throw ForbiddenException()
    } catch (e: RolloutStrategyApi.DuplicateNameException) {
      throw WebApplicationException("Duplicate name", 409)
    }
  }

  // we always clear the ids as we don't need them on saving, they are only used in the UI for validation tracking
  private fun cleanStrategy(rs: RolloutStrategy) {
    rs.attributes!!.forEach { attr: RolloutStrategyAttribute -> attr.id = null }
  }

  override fun validate(
    appId: UUID, req: RolloutStrategyValidationRequest,
    securityContext: SecurityContext
  ): RolloutStrategyValidationResponse {
    val validationFailure = validator.validateStrategies(null, req.customStrategies!!, req.sharedStrategies!!)
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

  companion object {
    private val log = LoggerFactory.getLogger(RolloutStrategyResource::class.java)
  }
}
