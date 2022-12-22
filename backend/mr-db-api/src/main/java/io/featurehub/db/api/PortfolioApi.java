package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.SortOrder;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This exposes the functionality of the Database version of the Portfolio API. It assumes
 * that
 */
public interface PortfolioApi {
  class DuplicatePortfolioException extends RuntimeException {}
  /**
   *
   * @param filter - optional, a filter that limits the portfolios being returned
   * @param ordering - for sorting the results
   * @param opts - for determining how deep we fill the objects returned
   *
   * @return zero or more org units matching
   */
  @NotNull List<Portfolio> findPortfolios(String filter, SortOrder ordering, Opts opts, Person currentPerson);

  Portfolio createPortfolio(Portfolio portfolio, Opts opts, @NotNull Person createdBy) throws DuplicatePortfolioException;
  Portfolio getPortfolio(UUID id, Opts opts, Person currentPerson);
  Portfolio updatePortfolio(Portfolio portfolio, Opts opts) throws DuplicatePortfolioException, OptimisticLockingException;
  void deletePortfolio(UUID id);
}
