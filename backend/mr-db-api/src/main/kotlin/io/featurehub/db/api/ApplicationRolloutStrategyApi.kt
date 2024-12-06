package io.featurehub.db.api

import io.featurehub.mr.model.ApplicationRolloutStrategy
import io.featurehub.mr.model.ApplicationRolloutStrategyList
import io.featurehub.mr.model.CreateApplicationRolloutStrategy
import io.featurehub.mr.model.UpdateApplicationRolloutStrategy
import java.util.*

interface ApplicationRolloutStrategyApi {
    @Throws(DuplicateNameException::class)
    fun createStrategy(
        appId: UUID,
        rolloutStrategy: CreateApplicationRolloutStrategy,
        person: UUID,
        opts: Opts
    ): ApplicationRolloutStrategy?

    @Throws(DuplicateNameException::class)
    fun updateStrategy(
        appId: UUID,
        strategyId: UUID,
        rolloutStrategy: UpdateApplicationRolloutStrategy,
        person: UUID,
        opts: Opts
    ): ApplicationRolloutStrategy?

    fun listStrategies(appId: UUID, page: Int, max: Int, filter: String?, includeArchived: Boolean, opts: Opts): ApplicationRolloutStrategyList
    fun getStrategy(appId: UUID, strategyId: UUID, opts: Opts): ApplicationRolloutStrategy?
    fun archiveStrategy(appId: UUID, strategyId: UUID, person: UUID): Boolean

    class DuplicateNameException : Exception()
}
