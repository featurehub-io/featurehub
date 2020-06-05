package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.SortOrder;

import java.util.List;

/**
 * This exposes the functionality of the Database version of the Portfolio API. It assumes
 * that
 */
public interface PortfolioApi {
  class DuplicatePortfolioException extends RuntimeException {}
  /**
   *
   * @param filter - optional, a filter that limits the portfolios being returned
   * @param organizationId - if provided, search for portfolios under this one, if null then at the top
   * @param ordering - for sorting the results
   * @param opts - for determining how deep we fill the objects returned
   *
   * @return zero or more org units matching
   */
  @NotNull List<Portfolio> findPortfolios(String filter, String organizationId, SortOrder ordering, Opts opts, Person currentPerson);

  Portfolio createPortfolio(Portfolio portfolio, Opts opts, Person createdBy) throws DuplicatePortfolioException;
  Portfolio getPortfolio(String id, Opts opts, Person currentPerson);
  Portfolio updatePortfolio(Portfolio portfolio, Opts opts) throws DuplicatePortfolioException, OptimisticLockingException;
  void deletePortfolio(String id);
}
