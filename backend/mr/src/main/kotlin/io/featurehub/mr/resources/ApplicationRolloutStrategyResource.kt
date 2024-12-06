package io.featurehub.mr.resources

import io.featurehub.db.api.ApplicationRolloutStrategyApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.mr.api.ApplicationRolloutStrategyServiceDelegate
import io.featurehub.mr.model.*
import io.featurehub.mr.utils.ApplicationUtils
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.*

class ApplicationRolloutStrategyResource @Inject constructor(
  private val applicationUtils: ApplicationUtils,
  private val applicationRolloutStrategyApi: ApplicationRolloutStrategyApi, private val validator: RolloutStrategyValidator
) : ApplicationRolloutStrategyServiceDelegate {
  override fun createApplicationStrategy(
    appId: UUID,
    createRolloutStrategy: CreateApplicationRolloutStrategy,
    holder: ApplicationRolloutStrategyServiceDelegate.CreateApplicationStrategyHolder,
    securityContext: SecurityContext
  ): ApplicationRolloutStrategy {
    val person = applicationUtils.featureCreatorCheck(securityContext, appId).current
    try {
      return applicationRolloutStrategyApi.createStrategy(
        appId, createRolloutStrategy, person.id!!.id,
        Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged)
      ) ?: throw NotFoundException()
    } catch (e: ApplicationRolloutStrategyApi.DuplicateNameException) {
      throw WebApplicationException("Duplicate name", 409)
    }
  }

  override fun deleteApplicationStrategy(
    appId: UUID,
    appStrategyId: UUID,
    holder: ApplicationRolloutStrategyServiceDelegate.DeleteApplicationStrategyHolder,
    securityContext: SecurityContext
  ) {
    val person = applicationUtils.featureCreatorCheck(securityContext, appId).current

    if (!applicationRolloutStrategyApi.archiveStrategy(appId, appStrategyId, person.id!!.id)) {
      throw NotFoundException()
    }
  }

  override fun getApplicationStrategy(
    appId: UUID,
    appStrategyId: UUID,
    holder: ApplicationRolloutStrategyServiceDelegate.GetApplicationStrategyHolder,
    securityContext: SecurityContext
  ): ApplicationRolloutStrategy {
    applicationUtils.featureReadCheck(securityContext, appId)
    log.info("getApplicationStrategy: {}", holder)

    return applicationRolloutStrategyApi.getStrategy(
      appId, appStrategyId, Opts().add(
        FillOpts.SimplePeople,
        holder.includeWhoChanged
      ).add(FillOpts.Usage, holder.includeUsage)
    ) ?: throw NotFoundException()
  }

  override fun listApplicationStrategies(
    appId: UUID,
    holder: ApplicationRolloutStrategyServiceDelegate.ListApplicationStrategiesHolder,
    securityContext: SecurityContext
  ): ApplicationRolloutStrategyList {
    applicationUtils.featureReadCheck(securityContext, appId)
    return applicationRolloutStrategyApi.listStrategies(
      appId,
      holder.page ?: 0, holder.max ?: 20, holder.filter,
      true == holder.includeArchived,
      Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged).add(FillOpts.Usage, holder.includeUsage)
    )
  }

  override fun updateApplicationStrategy(
    appId: UUID,
    appStrategyId: UUID,
    updateApplicationRolloutStrategy: UpdateApplicationRolloutStrategy,
    holder: ApplicationRolloutStrategyServiceDelegate.UpdateApplicationStrategyHolder,
    securityContext: SecurityContext
  ): ApplicationRolloutStrategy {
    val person = applicationUtils.featureCreatorCheck(securityContext, appId).current

    return try {
      applicationRolloutStrategyApi.updateStrategy(
        appId, appStrategyId, updateApplicationRolloutStrategy, person.id!!.id,
        Opts().add(FillOpts.SimplePeople, holder.includeWhoChanged)
      ) ?: throw ForbiddenException()
    } catch (e: ApplicationRolloutStrategyApi.DuplicateNameException) {
      throw WebApplicationException("Duplicate name", 409)
    }
  }

  override fun validate(
    appId: UUID, req: RolloutStrategyValidationRequest,
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

  companion object {
    private val log = LoggerFactory.getLogger(ApplicationRolloutStrategyResource::class.java)
  }
}
