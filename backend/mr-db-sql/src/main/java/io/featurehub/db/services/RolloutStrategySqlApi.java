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
  public RolloutStrategySqlApi(Database database, Conversions conversions, CacheSource cacheSource) {
    this.database = database;
    this.conversions = conversions;
    this.cacheSource = cacheSource;
  }

  @Override
  public RolloutStrategyInfo createStrategy(String appId, RolloutStrategy rolloutStrategy, Person person, Opts opts) throws DuplicateNameException {
    DbApplication app = conversions.uuidApplication(appId);
    DbPerson p = conversions.uuidPerson(person);

    if (app != null && p != null) {
      final int existing = new QDbRolloutStrategy()
        .application.eq(app)
        .whenArchived.isNull()
        .name.ieq(rolloutStrategy.getName()).findCount();
      if (existing > 0)  {
        throw new RolloutStrategyApi.DuplicateNameException();
      }

      final DbRolloutStrategy rs =
        new DbRolloutStrategy.Builder().application(app)
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
  public RolloutStrategyInfo updateStrategy(String appId, RolloutStrategy rolloutStrategy, Person person, Opts opts) throws DuplicateNameException {
    DbApplication app = conversions.uuidApplication(appId);
    DbPerson p = conversions.uuidPerson(person);
    DbRolloutStrategy strategy = conversions.uuidStrategy(rolloutStrategy.getId());

    if (strategy != null && app != null && p != null) {
      if (strategy.getApplication().getId().equals(app.getId())) {
        if (!rolloutStrategy.getName().equalsIgnoreCase(strategy.getName())) {
          // is there something using the existing name?
          final int existing =
            new QDbRolloutStrategy()
              .application.eq(app)
              .name.ieq(rolloutStrategy.getName())
              .whenArchived.isNull().findCount();
          if (existing > 0)  {
            throw new RolloutStrategyApi.DuplicateNameException();
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
        }
      } else {
        log.warn("Attempted violation of strategy update by {}", person.getId());
      }
    }

    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RolloutStrategyInfo> listStrategies(String appId, boolean includeArchived, Opts opts) {
    UUID app = Conversions.ifUuid(appId);

    if (app != null) {
      QDbRolloutStrategy qRS = new QDbRolloutStrategy().application.id.eq(app);

      if (!includeArchived) {
        qRS = qRS.whenArchived.isNull();
      }

      if (opts.contains(FillOpts.SimplePeople)) {
        qRS.whoChanged.fetch();
      }

      return qRS
        .findList().stream()
        .map((rs) -> conversions.toRolloutStrategy(rs, opts)).collect(Collectors.toList());
    }

    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public RolloutStrategyInfo getStrategy(String appId, String strategyIdOrName, Opts opts) {
    UUID app = Conversions.ifUuid(appId);
    UUID sId = Conversions.ifUuid(strategyIdOrName);

    if (app != null) {
      QDbRolloutStrategy qRS = new QDbRolloutStrategy().application.id.eq(app)
          .whenArchived.isNull();

      if (sId != null) {
        qRS = qRS.id.eq(sId);
      } else {
        qRS = qRS.name.eq(strategyIdOrName);
      }

      if (opts.contains(FillOpts.SimplePeople)) {
        qRS.whoChanged.fetch();
      }

      return conversions.toRolloutStrategy(qRS.findOne(), opts);
    }

    return null;
  }

  @Override
  public RolloutStrategyInfo archiveStrategy(String appId, String strategyId, Person person, Opts opts) {
    DbApplication app = conversions.uuidApplication(appId);
    DbPerson p = conversions.uuidPerson(person);
    DbRolloutStrategy strategy = conversions.uuidStrategy(strategyId);

    if (strategy != null && app != null && p != null) {
      if (strategy.getApplication().getId().equals(app.getId())) {
        // only update and publish if it _actually_ changed
        if (strategy.getWhenArchived() == null) {
          strategy.setWhoChanged(p);
          strategy.setWhenArchived(LocalDateTime.now());

          save(strategy);

          cacheSource.publishRolloutStrategyChange(strategy);
        }

        return conversions.toRolloutStrategy(strategy, opts);
      }
    }

    return null;
  }
}
