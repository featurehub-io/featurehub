package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.RolloutStrategyApi;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbRolloutStrategy;
import io.featurehub.db.model.query.QDbRolloutStrategy;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RolloutStrategySqlApi implements RolloutStrategyApi {
  private static final Logger log = LoggerFactory.getLogger(RolloutStrategySqlApi.class);
  private final Database database;
  private final Conversions conversions;
  private final CacheSource cacheSource;

  @Inject
  public RolloutStrategySqlApi(
      Database database, Conversions conversions, CacheSource cacheSource) {
    this.database = database;
    this.conversions = conversions;
    this.cacheSource = cacheSource;
  }

  @Override
  public RolloutStrategyInfo createStrategy(
      UUID appId, RolloutStrategy rolloutStrategy, Person person, Opts opts)
      throws DuplicateNameException {
    Conversions.nonNullApplicationId(appId);
    Conversions.nonNullPerson(person);

    if (rolloutStrategy == null) {
      throw new IllegalArgumentException("rolloutStrategy required");
    }

    DbApplication app = conversions.byApplication(appId);
    DbPerson p = conversions.byPerson(person);

    if (app != null && p != null) {
      final boolean existing =
          new QDbRolloutStrategy()
              .application
              .eq(app)
              .whenArchived
              .isNull()
              .name
              .ieq(rolloutStrategy.getName())
              .exists();
      if (existing) {
        throw new RolloutStrategyApi.DuplicateNameException();
      }

      final DbRolloutStrategy rs =
          new DbRolloutStrategy.Builder()
              .application(app)
              .whoChanged(p)
              .strategy(rolloutStrategy)
              .name(rolloutStrategy.getName())
              .build();

      try {
        save(rs);

        return conversions.toRolloutStrategy(rs, opts);
      } catch (Exception e) {
        throw new RolloutStrategyApi.DuplicateNameException();
      }
    }

    return null;
  }

  @Transactional
  private void save(DbRolloutStrategy rs) {
    database.save(rs);
  }

  @Override
  public RolloutStrategyInfo updateStrategy(
      UUID appId, RolloutStrategy rolloutStrategy, Person person, Opts opts)
      throws DuplicateNameException {
    Conversions.nonNullApplicationId(appId);
    Conversions.nonNullPerson(person);

    if (rolloutStrategy == null || rolloutStrategy.getId() == null) {
      throw new IllegalArgumentException("RolloutStrategy.id is required");
    }

    DbApplication app = conversions.byApplication(appId);
    DbPerson p = conversions.byPerson(person);
    DbRolloutStrategy strategy = byStrategy(appId, rolloutStrategy.getId().toString(), Opts.empty()).findOne();

    if (strategy != null && app != null && p != null) {
      if (strategy.getApplication().getId().equals(app.getId())) {

        // check if we are renaming it and if so, are we using a duplicate name
        if (!strategy.getName().equalsIgnoreCase(rolloutStrategy.getName())) {
          // is there something using the existing name?
          final boolean existing =
              new QDbRolloutStrategy()
                  .application
                  .eq(app)
                  .name
                  .ieq(rolloutStrategy.getName())
                  .whenArchived
                  .isNull()
                  .exists();
          if (existing) {
            throw new RolloutStrategyApi.DuplicateNameException();
          }
        }

        strategy.setStrategy(rolloutStrategy);
        strategy.setName(rolloutStrategy.getName());
        strategy.setWhoChanged(p);

        try {
          save(strategy);

          cacheSource.publishRolloutStrategyChange(strategy);

          return conversions.toRolloutStrategy(strategy, opts);
        } catch (Exception e) {
          throw new RolloutStrategyApi.DuplicateNameException();
        }
      } else {
        log.warn("Attempted violation of strategy update by {}", person.getId());
      }
    }

    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RolloutStrategyInfo> listStrategies(UUID appId, boolean includeArchived, Opts opts) {
    Conversions.nonNullApplicationId(appId);

    QDbRolloutStrategy qRS = new QDbRolloutStrategy().application.id.eq(appId);

    if (!includeArchived) {
      qRS = qRS.whenArchived.isNull();
    }

    if (opts.contains(FillOpts.SimplePeople)) {
      qRS.whoChanged.fetch();
    }

    return qRS.findList().stream()
        .map((rs) -> conversions.toRolloutStrategy(rs, opts))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public RolloutStrategyInfo getStrategy(UUID appId, String strategyIdOrName, Opts opts) {
    Conversions.nonNullApplicationId(appId);

    if (strategyIdOrName == null) {
      throw new IllegalArgumentException("strategy name/id required");
    }

    return conversions.toRolloutStrategy(byStrategy(appId, strategyIdOrName, opts).findOne(), opts);
  }

  private QDbRolloutStrategy byStrategy(UUID appId, String strategyIdOrName, Opts opts) {
    UUID sId = Conversions.checkUuid(strategyIdOrName);

    QDbRolloutStrategy qRS =
      new QDbRolloutStrategy().application.id.eq(appId).whenArchived.isNull();

    if (sId != null) {
      qRS = qRS.id.eq(sId);
    } else {
      qRS = qRS.name.eq(strategyIdOrName);
    }

    if (opts.contains(FillOpts.SimplePeople)) {
      qRS.whoChanged.fetch();
    }

    return qRS;
  }

  @Override
  public RolloutStrategyInfo archiveStrategy(
      UUID appId, String strategyIdOrName, Person person, Opts opts) {
    Conversions.nonNullApplicationId(appId);
    Conversions.nonNullPerson(person);

    if (strategyIdOrName == null) {
      throw new IllegalArgumentException("strategy name/id required");
    }

    DbApplication app = conversions.byApplication(appId);
    DbPerson p = conversions.byPerson(person);
    DbRolloutStrategy strategy = byStrategy(appId, strategyIdOrName, Opts.empty()).findOne();

    if (strategy != null && app != null && p != null) {
      // only update and publish if it _actually_ changed
      if (strategy.getWhenArchived() == null) {
        strategy.setWhoChanged(p);
        strategy.setWhenArchived(LocalDateTime.now());

        save(strategy);

        cacheSource.publishRolloutStrategyChange(strategy);
      }

      return conversions.toRolloutStrategy(strategy, opts);
    }

    return null;
  }
}
