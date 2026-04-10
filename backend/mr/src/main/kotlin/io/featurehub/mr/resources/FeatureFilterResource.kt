package io.featurehub.mr.resources

import io.featurehub.db.api.FeatureFilterApi
import io.featurehub.mr.api.FeatureFilterServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.CreateFeatureFilter
import io.featurehub.mr.model.FeatureFilter
import io.featurehub.mr.model.SearchFeatureFilterResult
import io.featurehub.mr.utils.PortfolioFeaturePermissionUtils
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.UUID

class FeatureFilterResource @Inject constructor(
  private val authManager: AuthManagerService,
  private val featureFilterApi: FeatureFilterApi,
  private val portfolioFeaturePermissionUtils: PortfolioFeaturePermissionUtils
) : FeatureFilterServiceDelegate {

  override fun createFeatureFilter(
    id: UUID,
    createFeatureFilter: CreateFeatureFilter,
    securityContext: SecurityContext
  ): FeatureFilter {
    val person = authManager.from(securityContext)
    portfolioFeaturePermissionUtils.requireFeatureWriteAccessInPortfolio(id, person)
    return try {
      featureFilterApi.create(id, person, createFeatureFilter)
    } catch (e: FeatureFilterApi.DuplicateNameException) {
      log.warn("Attempt to create duplicate feature filter '{}' in portfolio {}", createFeatureFilter.name, id)
      throw WebApplicationException(Response.Status.CONFLICT)
    }
  }

  override fun updateFeatureFilter(
    id: UUID,
    featureFilter: FeatureFilter,
    securityContext: SecurityContext
  ): FeatureFilter {
    val person = authManager.from(securityContext)
    portfolioFeaturePermissionUtils.requireFeatureWriteAccessInPortfolio(id, person)
    return try {
      featureFilterApi.update(id, person, featureFilter)
    } catch (e: FeatureFilterApi.OptimisticLockingException) {
      throw WebApplicationException(Response.Status.CONFLICT)
    } catch (e: FeatureFilterApi.FilterNotFoundException) {
      throw NotFoundException()
    }
  }

  override fun deleteFeatureFilter(
    id: UUID,
    featureFilter: FeatureFilter,
    securityContext: SecurityContext
  ): FeatureFilter {
    val person = authManager.from(securityContext)
    portfolioFeaturePermissionUtils.requireFeatureWriteAccessInPortfolio(id, person)
    return try {
      featureFilterApi.delete(id, person, featureFilter)
    } catch (e: FeatureFilterApi.OptimisticLockingException) {
      throw WebApplicationException(Response.Status.CONFLICT)
    } catch (e: FeatureFilterApi.FilterNotFoundException) {
      throw NotFoundException()
    }
  }

  override fun findFeatureFilters(
    id: UUID,
    holder: FeatureFilterServiceDelegate.FindFeatureFiltersHolder,
    securityContext: SecurityContext
  ): SearchFeatureFilterResult {
    val person = authManager.from(securityContext)
    portfolioFeaturePermissionUtils.requireFeatureReadAccessInPortfolio(id, person)
    return featureFilterApi.find(
      id, holder.filter, holder.max, holder.page, holder.sortOrder, holder.includeDetails ?: false
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(FeatureFilterResource::class.java)
  }
}
