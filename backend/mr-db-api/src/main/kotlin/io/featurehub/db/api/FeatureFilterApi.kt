package io.featurehub.db.api

import io.featurehub.mr.model.CreateFeatureFilter
import io.featurehub.mr.model.FeatureFilter
import io.featurehub.mr.model.SearchFeatureFilterResult
import io.featurehub.mr.model.SortOrder
import io.featurehub.mr.model.Person
import java.util.UUID

interface FeatureFilterApi {
  class DuplicateNameException : Exception()
  class OptimisticLockingException : Exception()
  class FilterNotFoundException : Exception()

  @Throws(DuplicateNameException::class)
  fun create(portfolioId: UUID, creator: Person, filter: CreateFeatureFilter): FeatureFilter

  @Throws(OptimisticLockingException::class, FilterNotFoundException::class)
  fun update(portfolioId: UUID, updater: Person, filter: FeatureFilter): FeatureFilter

  @Throws(OptimisticLockingException::class, FilterNotFoundException::class)
  fun delete(portfolioId: UUID, deleter: Person, filter: FeatureFilter): FeatureFilter

  /**
   * @param includeDetails when false, returns only id and name (for dropdowns).
   * When true, returns full FeatureFilter data including which features use each filter.
   */
  fun find(
    portfolioId: UUID,
    filter: String?,
    max: Int?,
    page: Int?,
    sortOrder: SortOrder?,
    includeDetails: Boolean
  ): SearchFeatureFilterResult
}
