package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.DuplicateKeyException;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbGroup;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.query.QDbGroup;
import io.featurehub.db.model.query.QDbPortfolio;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class PortfolioSqlApi implements io.featurehub.db.api.PortfolioApi {
  private static final Logger log = LoggerFactory.getLogger(PortfolioSqlApi.class);
  private final Database database;
  private final Conversions convertUtils;
  private final ArchiveStrategy archiveStrategy;

  @Inject
  public PortfolioSqlApi(
      Database database, Conversions convertUtils, ArchiveStrategy archiveStrategy) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.archiveStrategy = archiveStrategy;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Portfolio> findPortfolios(
      String filter, SortOrder ordering, Opts opts, Person currentPerson) {
    DbPerson personDoingFind = convertUtils.byPerson(currentPerson);

    if (personDoingFind == null) {
      return new ArrayList<>();
    }

    QDbPortfolio pFinder = new QDbPortfolio().organization.eq(convertUtils.getDbOrganization());

    if (filter != null && filter.trim().length() > 0) {
      pFinder = pFinder.name.ilike('%' + filter.trim() + '%');
    }

    if (ordering != null) {
      if (ordering == SortOrder.ASC) {
        pFinder = pFinder.order().name.asc();
      } else if (ordering == SortOrder.DESC) {
        pFinder = pFinder.order().name.desc();
      }
    }

    if (convertUtils.personIsNotSuperAdmin(personDoingFind)) {
      pFinder = pFinder.groups.peopleInGroup.id.eq(personDoingFind.getId());
    }

    pFinder = finder(pFinder, opts);

    return pFinder.findList().stream()
        .map(p -> convertUtils.toPortfolio(p, opts))
        .collect(Collectors.toList());
  }

  private QDbPortfolio finder(QDbPortfolio pFinder, Opts opts) {
    if (opts.contains(FillOpts.Groups)) {
      pFinder = pFinder.groups.fetch();
    }

    if (opts.contains(FillOpts.Portfolios)) {
      pFinder = pFinder.whoCreated.fetch();
    }

    if (opts.contains(FillOpts.Applications)) {
      pFinder = pFinder.applications.fetch();
    }

    if (!opts.contains(FillOpts.Archived)) {
      pFinder = pFinder.whenArchived.isNull();
    }

    return pFinder;
  }

  @Override
  public Portfolio createPortfolio(Portfolio portfolio, Opts opts, Person createdBy)
      throws DuplicatePortfolioException {
    if (portfolio == null || portfolio.getName() == null) {
      throw new IllegalArgumentException("portfolio:name is required");
    }

    final DbOrganization org = convertUtils.getDbOrganization();
    final DbPerson person = convertUtils.byPerson(createdBy);

    if (createdBy != null && person == null) {
      throw new IllegalArgumentException("Created by person is an invalid argument (does not exist)");
    }

    duplicateCheck(portfolio, null, org);

    DbPortfolio dbPortfolio =
        new DbPortfolio.Builder()
            .name(convertUtils.limitLength(portfolio.getName(), 200))
            .description(convertUtils.limitLength(portfolio.getDescription(), 400))
            .organization(org)
            .whoCreated(person)
            .build();

    updatePortfolio(dbPortfolio);

    return convertUtils.toPortfolio(dbPortfolio, opts);
  }

  @Transactional
  private void updatePortfolio(DbPortfolio portfolio) throws DuplicatePortfolioException {
    try {
      database.save(portfolio);
    } catch (DuplicateKeyException dke) {
      throw new DuplicatePortfolioException();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Portfolio getPortfolio(UUID id, Opts opts, Person currentPerson) {
    Conversions.nonNullPortfolioId(id);
    Conversions.nonNullPerson(currentPerson);

    DbPerson personDoingFind = convertUtils.byPerson(currentPerson);

    if (personDoingFind == null) {
      return null;
    }

    QDbPortfolio finder = finder(new QDbPortfolio().id.eq(id), opts);
    if (convertUtils.personIsNotSuperAdmin(personDoingFind)) {
      finder = finder.groups.peopleInGroup.id.eq(personDoingFind.getId());
    }

    return finder.findOneOrEmpty().map(portf -> convertUtils.toPortfolio(portf, opts)).orElse(null);
  }

  @Override
  public Portfolio updatePortfolio(Portfolio portfolio, Opts opts)
      throws DuplicatePortfolioException, OptimisticLockingException {
    if (portfolio == null || portfolio.getId() == null) {
      throw new IllegalArgumentException("Portfolio:id is required");
    }

    DbPortfolio portf = convertUtils.byPortfolio(portfolio.getId());

    if (portf != null) {
      if (portfolio.getVersion() == null || portfolio.getVersion() != portf.getVersion()) {
        throw new OptimisticLockingException();
      }

      duplicateCheck(portfolio, portf, portf.getOrganization());

      portf.setName(portfolio.getName());
      portf.setDescription(portfolio.getDescription());

      // this is actually a possible leak for duplicate portfolio names, we will get an exception
      // here.
      updatePortfolio(portf);

      // rename the group
      new QDbGroup()
          .adminGroup
          .eq(true)
          .and()
          .owningPortfolio
          .eq(portf)
          .endAnd()
          .findOneOrEmpty()
          .ifPresent(
              group -> {
                group.setName(portfolio.getName());
                updateGroup(group);
              });

      return convertUtils.toPortfolio(portf, opts);
    }

    return null;
  }

  private void duplicateCheck(Portfolio portfolio, DbPortfolio portf, DbOrganization organization)
      throws DuplicatePortfolioException {
    // if you are changing your name to its existing name, thats fine.  optimisation step
    if (portf != null
        && portfolio.getName() != null
        && portf.getName().equalsIgnoreCase(portfolio.getName())) {
      return;
    }
    // check for name duplicates
    DbPortfolio nameCheck =
        new QDbPortfolio()
            .whenArchived
            .isNull()
            .organization
            .eq(organization)
            .name
            .ieq(portfolio.getName())
            .findOne();
    if (nameCheck != null && (portf == null || !nameCheck.getId().equals(portf.getId()))) {
      throw new DuplicatePortfolioException();
    }
  }

  @Transactional
  private void updateGroup(DbGroup group) {
    database.save(group);
  }

  @Override
  @Transactional
  public void deletePortfolio(UUID id) {
    Conversions.nonNullPortfolioId(id);

    new QDbPortfolio().id.eq(id).findOneOrEmpty().ifPresent(archiveStrategy::archivePortfolio);
  }
}
