package io.featurehub.db.api

import io.featurehub.mr.model.PortfolioRolloutStrategy
import io.featurehub.mr.model.PortfolioRolloutStrategyList
import io.featurehub.mr.model.CreatePortfolioRolloutStrategy
import io.featurehub.mr.model.SortOrder
import io.featurehub.mr.model.UpdatePortfolioRolloutStrategy
import java.util.*

interface PortfolioRolloutStrategyApi {
    @Throws(DuplicateNameException::class)
    fun createStrategy(
        appId: UUID,
        rolloutStrategy: CreatePortfolioRolloutStrategy,
        person: UUID,
        opts: Opts
    ): PortfolioRolloutStrategy?

    @Throws(DuplicateNameException::class)
    fun updateStrategy(
        appId: UUID,
        strategyId: UUID,
        update: UpdatePortfolioRolloutStrategy,
        person: UUID,
        opts: Opts
    ): PortfolioRolloutStrategy?

    fun listStrategies(
      appId: UUID,
      page: Int,
      max: Int,
      filter: String?,
      includeArchived: Boolean,
      sortOrder: SortOrder?,
      opts: Opts
    ): PortfolioRolloutStrategyList
    fun getStrategy(appId: UUID, strategyId: UUID, opts: Opts): PortfolioRolloutStrategy?
    fun archiveStrategy(appId: UUID, strategyId: UUID, person: UUID): Boolean

    class DuplicateNameException : Exception()
}
