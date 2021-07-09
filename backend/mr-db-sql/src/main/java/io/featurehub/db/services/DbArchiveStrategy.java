package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbGroup;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.mr.model.PublishAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DbArchiveStrategy implements ArchiveStrategy {
  private static final Logger log = LoggerFactory.getLogger(DbArchiveStrategy.class);
  private final Database database;
  private final CacheSource cacheSource;
  public static final String archivePrefix = ":\\:\\:";
  private final DateTimeFormatter isoDate = DateTimeFormatter.ISO_DATE_TIME;

  @Inject
  public DbArchiveStrategy(Database database, CacheSource cacheSource) {
    this.database = database;
    this.cacheSource = cacheSource;
  }

  @Override
  @Transactional
  public void archivePortfolio(DbPortfolio portfolio) {
    portfolio.setWhenArchived(LocalDateTime.now());
    portfolio.setName(portfolio.getName() + archivePrefix + isoDate.format(portfolio.getWhenArchived()));
    database.save(portfolio);
    portfolio.getApplications().forEach(this::archiveApplication);
    portfolio.getGroups().forEach(this::archiveGroup);
    portfolio.getServiceAccounts().forEach(this::archiveServiceAccount);
  }

  @Override
  @Transactional
  public void archiveApplication(DbApplication application) {
    application.setWhenArchived(LocalDateTime.now());

    database.save(application);

    application.getEnvironments().forEach(this::archiveEnvironment);
    application.getFeatures().forEach(this::archiveApplicationFeature);


  }

  @Override
  @Transactional
  public void archiveEnvironment(DbEnvironment environment) {
    environment.setWhenArchived(LocalDateTime.now());
    environment.setName(environment.getName() + archivePrefix + isoDate.format(environment.getWhenArchived()));
    database.save(environment);
    cacheSource.deleteEnvironment(environment.getId());
    new QDbEnvironment().priorEnvironment.eq(environment).findList().forEach(e -> {
      if (environment.getPriorEnvironment() != null) {
        if (e.getId().equals(environment.getPriorEnvironment().getId())) {
          e.setPriorEnvironment(null);
        } else {
          e.setPriorEnvironment(environment.getPriorEnvironment());
        }
        database.save(e);
      }
    });
  }

  @Override
  @Transactional
  public void archiveOrganization(DbOrganization organization) {
    organization.setWhenArchived(LocalDateTime.now());
    database.save(organization);
    organization.getPortfolios().forEach(this::archivePortfolio);
  }

  @Override
  @Transactional
  public void archiveServiceAccount(DbServiceAccount serviceAccount) {
    serviceAccount.setWhenArchived(LocalDateTime.now());
    serviceAccount.setName(serviceAccount.getName() + archivePrefix + isoDate.format(serviceAccount.getWhenArchived()));
    database.save(serviceAccount);
    cacheSource.deleteServiceAccount(serviceAccount.getId());
  }

  @Override
  @Transactional
  public void archiveGroup(DbGroup group) {
    group.setWhenArchived(LocalDateTime.now());
    group.setName(group.getName() + archivePrefix + isoDate.format(group.getWhenArchived()));
    database.save(group);
  }

  @Override
  @Transactional
  public void archiveApplicationFeature(DbApplicationFeature feature) {
    feature.setWhenArchived(LocalDateTime.now());
    // key is unique
    feature.setKey(feature.getKey() + archivePrefix + isoDate.format(feature.getWhenArchived()));
    database.save(feature);
    cacheSource.publishFeatureChange(feature, PublishAction.DELETE);
  }

  @Override
  @Transactional
  public void archivePerson(DbPerson person) {
    person.setWhenArchived(LocalDateTime.now());
    database.save(person);
  }
}
