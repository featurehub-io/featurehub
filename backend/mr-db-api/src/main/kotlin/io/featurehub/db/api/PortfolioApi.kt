package io.featurehub.db.api

import io.featurehub.mr.model.CreatePortfolio
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.SortOrder
import java.util.*

/**
 * This exposes the functionality of the Database version of the Portfolio API. It assumes
 * that
 */
interface PortfolioApi {
    class DuplicatePortfolioException : RuntimeException()

    /**
     *
     * @param filter - optional, a filter that limits the portfolios being returned
     * @param ordering - for sorting the results
     * @param opts - for determining how deep we fill the objects returned
     *
     * @return zero or more org units matching
     */
    fun findPortfolios(filter: String?, ordering: SortOrder?, opts: Opts, currentPerson: UUID): List<Portfolio>

    @Throws(DuplicatePortfolioException::class)
    fun createPortfolio(portfolio: CreatePortfolio, opts: Opts, createdBy: UUID?): Portfolio?
    fun getPortfolio(id: UUID, opts: Opts, currentPerson: UUID): Portfolio?

    @Throws(DuplicatePortfolioException::class, OptimisticLockingException::class)
    fun updatePortfolio(portfolio: Portfolio, opts: Opts): Portfolio?
    fun deletePortfolio(id: UUID)
}
