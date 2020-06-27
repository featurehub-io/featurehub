package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.EnvironmentRoles;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbEnvironmentFeatureStrategy;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.FeatureState;
import io.featurehub.db.model.query.QDbAcl;
import io.featurehub.db.model.query.QDbApplicationFeature;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbGroup;
import io.featurehub.db.model.query.QDbPortfolio;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.db.utils.EnvironmentUtils;
import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.ApplicationRoleType;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.mr.model.RoleType;
import io.featurehub.mr.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class EnvironmentSqlApi implements EnvironmentApi {
  private static final Logger log = LoggerFactory.getLogger(EnvironmentSqlApi.class);
  private final Database database;
  private final ConvertUtils convertUtils;
  private final CacheSource cacheSource;
  private final ArchiveStrategy archiveStrategy;

  @Inject
  public EnvironmentSqlApi(Database database, ConvertUtils convertUtils, CacheSource cacheSource, ArchiveStrategy archiveStrategy) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.cacheSource = cacheSource;
    this.archiveStrategy = archiveStrategy;
  }

  @Override
  public EnvironmentRoles personRoles(Person current, String eid) {
    DbEnvironment e = convertUtils.uuidEnvironment(eid);
    DbPerson p = convertUtils.uuidPerson(current);

    Set<RoleType> roles = new HashSet<>();
    Set<ApplicationRoleType> appRoles = new HashSet<>();

    if (e != null && p != null) {
      // is this person a portfolio admin? if so, they have all access to all environments in the portfolio
      if (new QDbGroup().adminGroup.isTrue().whenArchived.isNull().peopleInGroup.eq(p).owningPortfolio.applications.environments.eq(e).exists()) {
        return new EnvironmentRoles.Builder()
          .applicationRoles(new HashSet<>(Arrays.asList(ApplicationRoleType.values())))
          .environmentRoles(new HashSet<>(Arrays.asList(RoleType.values()))).build();
      }


      new QDbAcl().environment.eq(e).group.peopleInGroup.eq(p).findList().forEach(fe -> {
        final List<RoleType> splitRoles = convertUtils.splitEnvironmentRoles(fe.getRoles());
        if (splitRoles != null) {
          roles.addAll(splitRoles);
        }
      });

      new QDbAcl().application.eq(e.getParentApplication()).group.peopleInGroup.eq(p).findList().forEach(fe -> {
        final List<ApplicationRoleType> splitRoles = convertUtils.splitApplicationRoles(fe.getRoles());
        if (splitRoles != null && splitRoles.contains(ApplicationRoleType.FEATURE_EDIT)) {
          appRoles.add(ApplicationRoleType.FEATURE_EDIT);
        }
      });
    }

    return new EnvironmentRoles.Builder().applicationRoles(appRoles).environmentRoles(roles).build();
  }

  @Override
  public boolean delete(String id) {
    DbEnvironment env = convertUtils.uuidEnvironment(id);

    if (env != null) {
      archiveStrategy.archiveEnvironment(env);
    }

    return env != null;
  }


  @Override
  public Environment get(String id, Opts opts, Person current) {
    final UUID envId = ConvertUtils.ifUuid(id);

    if (envId != null) {
      DbPerson currentPerson = convertUtils.uuidPerson(current);

      if (currentPerson != null) {
        QDbEnvironment env = new QDbEnvironment().id.eq(envId);
        if (convertUtils.personIsNotSuperAdmin(currentPerson)) {
          env = env.parentApplication.portfolio.groups.peopleInGroup.id.eq(currentPerson.getId());
        }

        if (opts.contains(FillOpts.ServiceAccounts)) {
          env = env.serviceAccountEnvironments.fetch();
        }

        if (opts.contains(FillOpts.Features)) {
          env = env.environmentFeatures.fetch();
        }

        return env.findOneOrEmpty().map(e -> convertUtils.toEnvironment(e, opts)).orElse(null);
      }
    }

    return null;
  }

  @Override
  @Transactional
  public Environment update(String envId, Environment env, Opts opts) throws OptimisticLockingException, DuplicateEnvironmentException, InvalidEnvironmentChangeException {
    DbEnvironment environment = convertUtils.uuidEnvironment(envId);

    if (environment != null) {
      if (env.getVersion() == null || environment.getVersion() != env.getVersion()) {
        throw new OptimisticLockingException();
      }

      dupeEnvironmentNameCheck(env, environment);

      circularPriorEnvironmentCheck(env, environment);

      environment.setDescription(env.getDescription());

      if (env.getProduction() != null) {
        environment.setProductionEnvironment(Boolean.TRUE.equals(env.getProduction()));
      }

      update(environment);

      return convertUtils.toEnvironment(environment, opts);
    }

    return null;
  }

  private void circularPriorEnvironmentCheck(Environment env, DbEnvironment environment) throws InvalidEnvironmentChangeException {
    // find anything that pointed to this environment and set it to what we used to point to
    DbEnvironment newPriorEnvironment = env.getPriorEnvironmentId() == null ? null : convertUtils.uuidEnvironment(env.getPriorEnvironmentId());

    if (newPriorEnvironment == null) {
      environment.setPriorEnvironment(null);
    } else {
      // our purpose here is to make sure that if the newPriorEnvironment's tree as we walk up points to US (ennvironment)
      // then we have to point it to our old prior environment.
      DbEnvironment currentEnvOldPrior = environment.getPriorEnvironment();

      environment.setPriorEnvironment(newPriorEnvironment);
      DbEnvironment walk = newPriorEnvironment;
      // so we walk up the NEW parent environment seeing if it points to us, and if so, point it to our old parent
      // make it null
      // otherwise walk up the tree until we can't walk anymore. Find any environments that
      // used to point to
      while (walk != null && walk.getPriorEnvironment() != null) {
        DbEnvironment oldPrior = walk.getPriorEnvironment();
        if (walk.getPriorEnvironment().getId().equals(environment.getId())) {
          walk.setPriorEnvironment(currentEnvOldPrior);
          database.update(walk);
        }
        walk = oldPrior;
      }
    }
  }

  private void dupeEnvironmentNameCheck(Environment env, DbEnvironment dbEnv) throws DuplicateEnvironmentException {
    if (env.getName() != null) {
      env.setName(env.getName().trim());

      if (!dbEnv.getName().equals(env.getName())) {
        DbEnvironment dupe = new QDbEnvironment().and().name.eq(env.getName()).parentApplication.eq(dbEnv.getParentApplication()).endAnd().findOne();
        if (dupe != null && !dupe.getId().equals(dbEnv.getId())) {
          throw new DuplicateEnvironmentException();
        }
      }

      dbEnv.setName(env.getName());
    }
  }

  // we assume
  // - person who created is a portfolio or superuser admin
  // - env has been validated for content
  @Override
  public Environment create(Environment env, Application app, Person whoCreated) throws DuplicateEnvironmentException, InvalidEnvironmentChangeException {
    DbApplication application = convertUtils.uuidApplication(app.getId());
    if (application != null) {
      if (new QDbEnvironment().and().name.eq(env.getName()).whenArchived.isNull().parentApplication.eq(application).endAnd().findCount() > 0) {
        throw new DuplicateEnvironmentException();
      }
      DbEnvironment priorEnvironment = convertUtils.uuidEnvironment(env.getPriorEnvironmentId());
      if (priorEnvironment != null && !priorEnvironment.getParentApplication().getId().equals(application.getId())) {
        throw new InvalidEnvironmentChangeException();
      }
      // so we don't have an environment so lets order them and put this one before the 1st one
      if (priorEnvironment == null) {
        final List<DbEnvironment> environments = new QDbEnvironment().parentApplication.eq(application).whenArchived.isNull().findList();
        if (!environments.isEmpty()) {
          promotionSortedEnvironments(environments);
          priorEnvironment = environments.get(environments.size()-1);
        }
      }
      DbEnvironment newEnv = new DbEnvironment.Builder()
        .description(env.getDescription())
        .name(env.getName())
        .priorEnvironment(priorEnvironment)
        .parentApplication(application)
        .productionEnvironment(Boolean.TRUE.equals(env.getProduction()))
        .build();

      final DbEnvironment createdEnvironment = update(newEnv);

      cacheSource.updateEnvironment(createdEnvironment, PublishAction.CREATE);

      discoverMissingBooleanApplicationFeaturesForThisEnvironment(createdEnvironment, whoCreated);

      return convertUtils.toEnvironment(createdEnvironment, Opts.empty());
    }

    return null;
  }

  private void discoverMissingBooleanApplicationFeaturesForThisEnvironment(DbEnvironment createdEnvironment, Person whoCreated) {
    final List<DbEnvironmentFeatureStrategy> newFeatures
      = new QDbApplicationFeature().whenArchived.isNull().parentApplication.eq(createdEnvironment.getParentApplication()).valueType.eq(FeatureValueType.BOOLEAN).findList().stream()
      .map(af -> {
        return new DbEnvironmentFeatureStrategy.Builder()
          .defaultValue(Boolean.FALSE.toString())
          .environment(createdEnvironment)
          .feature(af)
          .featureState(FeatureState.ENABLED)
          .locked(true)
          .whoUpdated(convertUtils.uuidPerson(whoCreated))
          .build();
      }).collect(Collectors.toList());

    saveAllFeatures(newFeatures);

    for (DbEnvironmentFeatureStrategy nf : newFeatures) {
      cacheSource.publishFeatureChange(nf);
    }
  }

  @Transactional
  private void saveAllFeatures(List<DbEnvironmentFeatureStrategy> newFeatures) {
    newFeatures.forEach(database::save);

  }

  void promotionSortedEnvironments(List<DbEnvironment> environments) {
    Map<UUID, DbEnvironment> environmentOrderingMap = environments.stream().collect(Collectors.toMap(DbEnvironment::getId, e -> e));

    environments.sort((o1, o2) -> {
      final DbEnvironment env1 = environmentOrderingMap.get(o1.getId());
      final DbEnvironment env2 = environmentOrderingMap.get(o2.getId());

      Integer w = EnvironmentUtils.walkAndCompare(env1, env2);
      if (w == null) {
        w = EnvironmentUtils.walkAndCompare(env2, env1);
        if (w == null) {
          if (env1.getPriorEnvironment() == null && env2.getPriorEnvironment() == null) {
            return 0;
          }
          if (env1.getPriorEnvironment() != null && env2.getPriorEnvironment() == null) {
            return 1;
          }
          return -1;
        } else {
          return w * -1;
        }
      }

      return w;
    });

    environments.forEach(e -> log.info("ENVIRONMENT: {}", e.getName()));
  }

  @Transactional
  private DbEnvironment update(DbEnvironment env) {
    database.save(env);
    cacheSource.updateEnvironment(env, PublishAction.UPDATE);
    return env;
  }

  @Override
  public List<Environment> search(String appId, String filter, SortOrder order, Opts opts, Person current) {
    DbApplication application = convertUtils.uuidApplication(appId);
    if (application != null) {
      DbPerson currentPerson = convertUtils.uuidPerson(current);

      if (currentPerson != null) {
        QDbEnvironment eq = new QDbEnvironment().parentApplication.eq(application);
        if (filter != null) {
          eq = eq.name.ilike("%" + filter + "%");
        }

        eq = fetchEnvironmentOpts(opts, eq);

        if (SortOrder.ASC == order) {
          eq = eq.order().name.asc();
        } else if (SortOrder.DESC == order) {
          eq = eq.order().name.desc();
        }

        if (!opts.contains(FillOpts.Archived)) {
          eq = eq.whenArchived.isNull();
        }

        if (convertUtils.personIsNotSuperAdmin(currentPerson)) {
          eq = eq.parentApplication.portfolio.groups.peopleInGroup.id.eq(currentPerson.getId());
        }

        return eq.findList().stream().map(e -> convertUtils.toEnvironment(e, opts)).collect(Collectors.toList());
      }
    }

    return new ArrayList<>();
  }

  @Override
  public Portfolio findPortfolio(String envId) {
    UUID eId = ConvertUtils.ifUuid(envId);

    if (eId != null) {
      return convertUtils.toPortfolio(new QDbPortfolio().applications.environments.id.eq(eId).findOne(), Opts.empty());
    }

    return null;
  }

  private QDbEnvironment fetchEnvironmentOpts(Opts opts, QDbEnvironment eq) {
    if (opts.contains(FillOpts.Acls)) {
      eq = eq.groupRolesAcl.fetch();
    }
    return eq;
  }

  @Override
  public List<Environment> setOrdering(Application app, List<Environment> environments) {
    DbApplication dbApp = convertUtils.uuidApplication(app.getId());

    Map<String, DbEnvironment> envs = dbApp.getEnvironments().stream().collect(Collectors.toMap(e -> e.getId().toString(), Function.identity()));

    for(Environment e : environments) {
      DbEnvironment dbEnv = envs.get(e.getId());
      if (dbEnv == null) {
        log.error("Environment {} not found", e.getId());
        return null;
      }
      if (dbEnv.getVersion() != e.getVersion()) {
        log.error("Environment version should be {} and is {}", dbEnv.getVersion(), e.getVersion());
        return null;
      }
      if (e.getPriorEnvironmentId() != null && envs.get(e.getPriorEnvironmentId()) == null) {
        log.error("Attempted to set a prior environment id you didn't pass");
        return null;
      }
    }

    Map<String, Environment> destinations = environments.stream().collect(Collectors.toMap(Environment::getId, Function.identity()));

    for(Environment e : environments) {
      // create a slot for each environment
      Map<String, Integer> spot = environments.stream().collect(Collectors.toMap(Environment::getId, (e1) -> 0));
      // set our one to "visited"
      spot.put(e.getId(), 1);
      // now walk backwards until we either hit the end or see "visited"
      String currentId = e.getPriorEnvironmentId();
      while (currentId != null && spot.get(currentId) == 0) {
        spot.put(currentId, 1);
        currentId = destinations.get(currentId).getPriorEnvironmentId();
      }
      if (currentId != null) {
        log.error("circular environment in {}", currentId);
        return null;
      }
    }

    // ok they all seem to be ok
    updatePriorEnvironmentIds(envs, environments);

    Opts emptyOpts = Opts.opts();

    return new QDbEnvironment().parentApplication.id.eq(dbApp.getId()).findList().stream().map(e -> convertUtils.toEnvironment(e, emptyOpts)).collect(Collectors.toList());
  }


  @Transactional
  private void updatePriorEnvironmentIds(Map<String, DbEnvironment> envs, List<Environment> environments) {
    for(Environment e : environments) {
      DbEnvironment env = envs.get(e.getId());
      DbEnvironment prior = e.getPriorEnvironmentId() == null ? null : envs.get(e.getPriorEnvironmentId());

      env.setPriorEnvironment(prior);
      database.save(env);
    }
  }
}
