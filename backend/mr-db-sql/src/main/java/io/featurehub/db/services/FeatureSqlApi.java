package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.FeatureApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.PersonFeaturePermission;
import io.featurehub.db.model.DbAcl;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbEnvironmentFeatureStrategy;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.query.QDbAcl;
import io.featurehub.db.model.query.QDbApplication;
import io.featurehub.db.model.query.QDbApplicationFeature;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbEnvironmentFeatureStrategy;
import io.featurehub.db.model.query.QDbGroup;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.db.utils.EnvironmentUtils;
import io.featurehub.mr.model.ApplicationFeatureValues;
import io.featurehub.mr.model.ApplicationRoleType;
import io.featurehub.mr.model.EnvironmentFeatureValues;
import io.featurehub.mr.model.EnvironmentFeaturesResult;
import io.featurehub.mr.model.FeatureEnvironment;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FeatureSqlApi implements FeatureApi {
  private static final Logger log = LoggerFactory.getLogger(FeatureSqlApi.class);
  private final Database database;
  private final ConvertUtils convertUtils;
  private final CacheSource cacheSource;

  @Inject
  public FeatureSqlApi(Database database, ConvertUtils convertUtils, CacheSource cacheSource) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.cacheSource = cacheSource;
  }

  @Override
  public FeatureValue createFeatureValueForEnvironment(String eid, String key, FeatureValue featureValue, PersonFeaturePermission person) throws OptimisticLockingException, NoAppropriateRole {
    UUID eId = ConvertUtils.ifUuid(eid);

    if (!person.hasWriteRole()) {
      DbEnvironment env = new QDbEnvironment().id.eq(eId).whenArchived.isNull().findOne();
      log.warn("User has no roles for environment {} key {}", eid, key);
      if (env == null) {
        log.error("could not find environment or environment is archived");
      } else {
        log.error("error env {} app {} portfolio {}", env.getName(), env.getParentApplication().getName(), env.getParentApplication().getPortfolio().getName());
      }

      throw new NoAppropriateRole();
    }

    if (eId != null) {
      final DbEnvironmentFeatureStrategy strategy = new QDbEnvironmentFeatureStrategy().environment.id.eq(eId).feature.key.eq(key).findOne();
      log.info("strategy= {}",strategy);
      if (strategy != null) {
        // this is an update not a create, environment + app-feature key exists
        return onlyUpdateFeatureValueForEnvironment(featureValue, person, strategy);
      } else if (person.hasChangeValueRole()) {
        return onlyCreateFeatureValueForEnvironment(eid, key, featureValue, person);
      } else {
        log.info("roles for person are {} and are not enough for environment {} and key {}", person.toString(), eid, key);
        throw new NoAppropriateRole();
      }
    }

    log.info("Environment does not exist");

    return null;
  }

  private FeatureValue onlyCreateFeatureValueForEnvironment(String eid, String key, FeatureValue featureValue, PersonFeaturePermission person) throws NoAppropriateRole {
    final DbEnvironment val = convertUtils.uuidEnvironment(eid);

    if (val != null) {
      final DbApplicationFeature appFeature = new QDbApplicationFeature().key.eq(key).parentApplication.environments.eq(val).findOne();

      if (appFeature != null) {
        DbEnvironmentFeatureStrategy strategy = new DbEnvironmentFeatureStrategy.Builder()
          .environment(val)
          .feature(appFeature)
          .enabledStrategy(featureValue.getRolloutStrategy())
          .build();

        updateStrategy(featureValue, person, strategy);

        save(strategy);

        return convertUtils.toFeatureValue(strategy);
      } else {
        log.error("Attempted to create feature value in environment `{}` where feature key did not exist: `{}`", eid, key);
      }
    }

    return null;
  }

  @Transactional
  private void save(DbEnvironmentFeatureStrategy strategy) {
    database.save(strategy);

    cacheSource.publishFeatureChange(strategy);
  }

  @Override
  @Transactional
  public boolean deleteFeatureValueForEnvironment(String eid, String key) {
    UUID eId = ConvertUtils.ifUuid(eid);

    if (eId != null) {
      DbEnvironmentFeatureStrategy strategy = new QDbEnvironmentFeatureStrategy().environment.id.eq(eId).feature.key.eq(key).findOne();

      if (strategy != null) {
        cacheSource.deleteFeatureChange(strategy.getFeature(), strategy.getEnvironment().getId().toString());

        return database.delete(strategy);
      }
    }

    return false;
  }

  private FeatureValue onlyUpdateFeatureValueForEnvironment(FeatureValue featureValue, PersonFeaturePermission person, DbEnvironmentFeatureStrategy strategy) throws OptimisticLockingException, NoAppropriateRole {
    if (featureValue.getVersion() == null || strategy.getVersion() != featureValue.getVersion()) {
      throw new OptimisticLockingException();
    }

    // todo: set what changed
    String oldValue = strategy.getDefaultValue();
    boolean oldLocked = strategy.isLocked();

    updateStrategy(featureValue, person, strategy);

    save(strategy);

    return convertUtils.toFeatureValue(strategy);
  }

  private void updateStrategy(FeatureValue featureValue, PersonFeaturePermission person, DbEnvironmentFeatureStrategy strategy) throws NoAppropriateRole {
    final DbApplicationFeature feature = strategy.getFeature();

    if (person.hasChangeValueRole() && ( !strategy.isLocked() || (Boolean.FALSE.equals(featureValue.getLocked()) && person.hasUnlockRole()) )) {
      if (feature.getValueType() == FeatureValueType.NUMBER) {
        strategy.setDefaultValue(featureValue.getValueNumber() == null ? null : featureValue.getValueNumber().toString());
      } else if (feature.getValueType() == FeatureValueType.STRING) {
        strategy.setDefaultValue(featureValue.getValueString());
      } else if (feature.getValueType() == FeatureValueType.JSON) {
        strategy.setDefaultValue(featureValue.getValueJson());
      } else if (feature.getValueType() == FeatureValueType.BOOLEAN) {
        strategy.setDefaultValue(featureValue.getValueBoolean() == null ? Boolean.FALSE.toString() : featureValue.getValueBoolean().toString());
      }

      strategy.setEnabledStrategy(featureValue.getRolloutStrategy());

      strategy.setRolloutStrategyInstances(featureValue.getRolloutStrategyInstances());
    }

    // change locked before changing value, as may not be able to change value if locked
    boolean newValue = featureValue.getLocked() == null ? false : featureValue.getLocked();
    if (newValue != strategy.isLocked()) {
      if (!newValue && person.hasUnlockRole()) {
        strategy.setLocked(false);
      } else if (newValue && person.hasLockRole()) {
        strategy.setLocked(true);
      } else {
        throw new NoAppropriateRole();
      }
    }

    strategy.setWhoUpdated(convertUtils.uuidPerson(person.person));
    if (strategy.getWhoUpdated() == null) {
      log.error("Unable to set who updated on strategy {}", person.person);
    }
  }

  @Override
  public FeatureValue updateFeatureValueForEnvironment(String eid, String key, FeatureValue featureValue, PersonFeaturePermission person) throws OptimisticLockingException, NoAppropriateRole {
    return createFeatureValueForEnvironment(eid, key, featureValue, person);
  }

  @Override
  public FeatureValue getFeatureValueForEnvironment(String eid, String key) {
    UUID eId = ConvertUtils.ifUuid(eid);

    if (eId != null) {
//      new QDbEnvironmentFeatureStrategy().feature.key.eq(key).findList().forEach(fv -> {
//        log.info("Found fv in key {}", convertUtils.toFeature(fv));
//      });
      final DbEnvironmentFeatureStrategy strategy = new QDbEnvironmentFeatureStrategy().environment.id.eq(eId).feature.key.eq(key).findOne();
      return strategy == null ? null : convertUtils.toFeatureValue(strategy);
    }

    return null;
  }

  @Override
  public EnvironmentFeaturesResult getAllFeatureValuesForEnvironment(String eid) {
    UUID eId = ConvertUtils.ifUuid(eid);

    if (eId != null) {
      return new EnvironmentFeaturesResult()
        .featureValues(new QDbEnvironmentFeatureStrategy().environment.id.eq(eId)
          .feature.whenArchived.isNull().findList().stream().map(convertUtils::toFeatureValue).collect(Collectors.toList()))
          .environments(Collections.singletonList(convertUtils.toEnvironment(new QDbEnvironment().id.eq(eId).findOne(), Opts.empty())));
    }

    return null;
  }

  // we are going to have to put a transaction at this level as we want the whole thing to roll back if there is an issue
  @Override
  @Transactional
  public List<FeatureValue> updateAllFeatureValuesForEnvironment(String eid, List<FeatureValue> featureValues, PersonFeaturePermission person) throws OptimisticLockingException, NoAppropriateRole {
    UUID eId = ConvertUtils.ifUuid(eid);

    if (featureValues == null || featureValues.size() != featureValues.stream().map(FeatureValue::getKey).collect(Collectors.toSet()).size()) {
      throw new BadRequestException("Invalid update dataset");
    }

    if (eId != null) {
      final List<DbEnvironmentFeatureStrategy> existing = new QDbEnvironmentFeatureStrategy().environment.id.eq(eId).feature.whenArchived.isNull().findList();
      final Map<String, FeatureValue> newValues = featureValues.stream().collect(Collectors.toMap(FeatureValue::getKey, Function.identity()));
      // take them all and remove all fv's we were passed, leaving only EFS's we want to remove
      final Set<String> deleteKeys = existing.stream().map(e -> e.getFeature().getKey()).collect(Collectors.toSet());
      for (FeatureValue fv : featureValues) {
        deleteKeys.remove(fv.getKey());
      }

      // we should be left with only keys in deleteKeys that do not exist in the passed in list of feature values
      // and in addingKeys we should be given a list of keys which exist in the passed in FV's but didn't exist in the db
      List<DbEnvironmentFeatureStrategy> deleteStrategies = new ArrayList<>();
      for (DbEnvironmentFeatureStrategy strategy : existing) {
        if (deleteKeys.contains(strategy.getFeature().getKey())) {
          deleteStrategies.add(strategy);
        } else {
          FeatureValue fv = newValues.remove(strategy.getFeature().getKey());
          onlyUpdateFeatureValueForEnvironment(fv, person, strategy);
        }
      }

      // now for the creates
      for (String key : newValues.keySet()) {
        FeatureValue fv = newValues.get(key);
        onlyCreateFeatureValueForEnvironment(eid, key, fv, person);
      }

      if (!deleteStrategies.isEmpty()) {
        publishTheRemovalOfABunchOfStrategies(deleteStrategies);

        database.deleteAll(deleteStrategies);
      }
    }

    return null;
  }

  // can't background this because they will deleted shortly
  private void publishTheRemovalOfABunchOfStrategies(Collection<DbEnvironmentFeatureStrategy> deleteStrategies) {
    if (!deleteStrategies.isEmpty()) {
      deleteStrategies.parallelStream().forEach(strategy -> {
        cacheSource.deleteFeatureChange(strategy.getFeature(), strategy.getEnvironment().getId().toString());
      });
    }
  }

  static class EnvironmentsAndStrategies {
    public Map<UUID, DbEnvironmentFeatureStrategy> strategies;
    public Map<UUID, List<RoleType>> roles;
    public Map<UUID, DbEnvironment> environments;
    public Set<ApplicationRoleType> appRolesForThisPerson;

    public EnvironmentsAndStrategies(Map<UUID, DbEnvironmentFeatureStrategy> strategies, Map<UUID, List<RoleType>> roles, Map<UUID, DbEnvironment> environments, Set<ApplicationRoleType> appRoles) {
      this.strategies = strategies;
      this.roles = roles;
      this.environments = environments;
      this.appRolesForThisPerson = appRoles;
    }
  }


  private EnvironmentsAndStrategies strategiesUserCanAccess(String appId, String key, Person person) {

    DbPerson dbPerson = convertUtils.uuidPerson(person);
    DbApplication app = convertUtils.uuidApplication(appId);

    if (app == null || app.getWhenArchived() != null || dbPerson == null || dbPerson.getWhenArchived() != null) {
      return null;
    }

    Map<UUID, DbEnvironmentFeatureStrategy> strategiesResult = new HashMap<>();
    Map<UUID, List<RoleType>> roles = new HashMap<>();
    Map<UUID, DbEnvironment> environments = new HashMap<>();

    boolean personAdmin = isPersonAdmin(dbPerson, app);

    Map<UUID, DbEnvironmentFeatureStrategy> strategies =
      new QDbEnvironmentFeatureStrategy().feature.key.eq(key).feature.parentApplication
        .eq(app)
        .findList()
        .stream()
        .collect(Collectors.toMap(e -> e.getEnvironment().getId(), Function.identity()));

    new QDbAcl().environment.parentApplication.eq(app).group.peopleInGroup.eq(dbPerson).findList().forEach(fe -> {
      log.debug("Found environment `{}`, app `{}`, group `{}`, roles `{}`",
        fe.getEnvironment() == null ? "<none>" : fe.getEnvironment().getName(),
        fe.getApplication() == null ? "<none>" : fe.getApplication().getName(),
        fe.getGroup().getName(), fe.getRoles());
      final List<RoleType> roleTypes = convertUtils.splitEnvironmentRoles(fe.getRoles());

      if (roleTypes != null) {
        UUID envId = fe.getEnvironment().getId();
        roles.put(envId, roleTypes);
        environments.put(envId, fe.getEnvironment());

        DbEnvironmentFeatureStrategy strategy = strategies.remove(envId);

        if (strategy != null) {
          strategiesResult.put(envId, strategy);
        }
      }
    });

    // they have at least one environment or they are an admin
    if (environments.size() > 0 || personAdmin) {
      final List<RoleType> adminRoles = Collections.singletonList(RoleType.READ);
      final List<RoleType> emptyRoles = Collections.emptyList();
      new QDbEnvironment().parentApplication.eq(app).findList().forEach(env -> {
        if (environments.get(env.getId()) == null) {
          environments.put(env.getId(), env);
          roles.put(env.getId(), personAdmin ? adminRoles : emptyRoles);
        }
      });
    }

    Set<ApplicationRoleType> appRoles =
      new QDbAcl().application.isNotNull()
        .select(QDbAcl.Alias.roles).roles.isNotNull()
        .group.whenArchived.isNotNull()
        .group.owningPortfolio.eq(app.getPortfolio())
        .group.peopleInGroup.eq(dbPerson).findList().stream().map(appAcl -> convertUtils.splitApplicationRoles(appAcl.getRoles())).flatMap(List::stream).collect(Collectors.toSet());

    return new EnvironmentsAndStrategies(strategiesResult, roles, environments, appRoles);
  }

  private boolean isPersonAdmin(DbPerson dbPerson, DbApplication app) {
    DbOrganization org = app.getPortfolio().getOrganization();
    // if a person is in a null portfolio group or portfolio group
    return new QDbGroup().peopleInGroup.eq(dbPerson).or().owningOrganization.eq(org).and().adminGroup.isTrue().owningPortfolio.applications.eq(app).endAnd().endOr().findCount() > 0;
  }

  @Override
  public List<FeatureEnvironment> getFeatureValuesForApplicationForKeyForPerson(String appId, String key, Person person) {
    EnvironmentsAndStrategies result = strategiesUserCanAccess(appId, key, person);

    if (result != null) {
      return result.environments.keySet().stream()
        .map(e -> convertUtils.toFeatureEnvironment(result.strategies.get(e), result.roles.get(e), result.environments.get(e), Opts.opts(FillOpts.ServiceAccounts)))
        .collect(Collectors.toList());
    }

    return null;
  }

  @Override
  @Transactional
  public void updateAllFeatureValuesByApplicationForKey(String id, String key, List<FeatureValue> featureValue, Person person, boolean removeValuesNotPassed) throws OptimisticLockingException, NoAppropriateRole {
    EnvironmentsAndStrategies result = strategiesUserCanAccess(id, key, person);

    if (result != null) {
      // environment id -> role in that environment
      Map<UUID, List<RoleType>> environmentToRoleMap = result.roles;
      // environment -> feature value
      Map<UUID, DbEnvironmentFeatureStrategy> strategiesToDelete = result.strategies;

      for (FeatureValue fv : featureValue) {
        UUID envId = ConvertUtils.ifUuid(fv.getEnvironmentId());
        if (envId == null) {
          log.warn("Trying to update for environment `{}` and environment id is invalid.", envId);
          throw new NoAppropriateRole();
        }

        List<RoleType> roles = environmentToRoleMap.get(envId);
        if (roles == null) {
          log.warn("Trying to update for environment `{}` and environment id has no roles (no permissions).", envId);
          throw new NoAppropriateRole();
        }

        createFeatureValueForEnvironment(fv.getEnvironmentId(), key, fv,
          new PersonFeaturePermission.Builder()
            .person(person)
            .appRoles(result.appRolesForThisPerson)
            .roles(new HashSet<>(roles)).build());

        strategiesToDelete.remove(envId); // we processed this environment ok, didn't throw a wobbly
      }

      // now remove any ability to remove feature values that are flags
      final List<UUID> invalidDeletions = strategiesToDelete.keySet().stream()
        .filter(u -> (strategiesToDelete.get(u).getFeature().getValueType() != FeatureValueType.BOOLEAN))
        .collect(Collectors.toList());
      invalidDeletions.forEach(strategiesToDelete::remove);

      if (removeValuesNotPassed) {
        publishTheRemovalOfABunchOfStrategies(strategiesToDelete.values());
        database.deleteAll(strategiesToDelete.values());
      }
    }
  }

  private EnvironmentFeatureValues environmentToFeatureValues(DbAcl acl, boolean personIsAdmin) {
    List<RoleType> roles;

    if (personIsAdmin) {
      roles = Arrays.asList(RoleType.values());
    } else {
      roles = convertUtils.splitEnvironmentRoles(acl.getRoles());
    }

    if (roles == null || roles.isEmpty()) {
      return null;
    }

    return new EnvironmentFeatureValues()
      .environmentId(acl.getEnvironment().getId().toString())
      .environmentName(acl.getEnvironment().getName())
      .priorEnvironmentId(acl.getEnvironment().getPriorEnvironment() == null ? null : acl.getEnvironment().getPriorEnvironment().getId().toString())
      .roles(roles)
      .features(new QDbEnvironmentFeatureStrategy()
        .environment.eq(acl.getEnvironment())
        .environment.whenArchived.isNull()
        .feature.whenArchived.isNull()
        .findList().stream().map(convertUtils::toFeatureValue).collect(Collectors.toList()));
  }

  @Override
  public ApplicationFeatureValues findAllFeatureAndFeatureValuesForEnvironmentsByApplication(String appId, Person person) {
    DbPerson dbPerson = convertUtils.uuidPerson(person);
    DbApplication app = convertUtils.uuidApplication(appId);

    if (app != null && dbPerson != null && app.getWhenArchived() == null && dbPerson.getWhenArchived() == null) {
      final Opts empty = Opts.empty();

      boolean personAdmin = isPersonAdmin(dbPerson, app);

      Map<String, DbEnvironment> environmentOrderingMap = new HashMap<>();
      // the requirement is that we only send back environments they have at least READ access to
      final List<EnvironmentFeatureValues> permEnvs =
          new QDbAcl()
            .environment.whenArchived.isNull()
            .environment.parentApplication.eq(app)
            .environment.parentApplication.whenArchived.isNull()
            .environment.parentApplication.groupRolesAcl.fetch()
            .group.whenArchived.isNull()
            .group.peopleInGroup.eq(dbPerson).findList()
        .stream()
        .peek(acl -> environmentOrderingMap.put(acl.getEnvironment().getId().toString(), acl.getEnvironment()))
        .map(acl -> environmentToFeatureValues(acl, personAdmin))
        .filter(Objects::nonNull)
        .filter(efv -> !efv.getRoles().isEmpty())

        .collect(Collectors.toList());

//      Set<String> envs = permEnvs.stream().map(EnvironmentFeatureValues::getEnvironmentId).distinct().collect(Collectors.toSet());

      Map<String, EnvironmentFeatureValues> envs = new HashMap<>();

      // merge any duplicates, this occurs because the database query can return duplicate lines
      permEnvs.forEach(e -> {
        EnvironmentFeatureValues original = envs.get(e.getEnvironmentId());
        if (original != null) { // merge them
          Set<String> originalFeatureValueIds = original.getFeatures().stream().map(FeatureValue::getId).collect(Collectors.toSet());
          e.getFeatures().forEach(fv -> {
            if (!originalFeatureValueIds.contains(fv.getId())) {
              original.getFeatures().add(fv);
            }
          });

          e.getRoles().forEach(rt -> {
            if (!original.getRoles().contains(rt)) {
              original.getRoles().add(rt);
            }
          });
        } else {
          envs.put(e.getEnvironmentId(), e);
        }
      });

      // now we have a flat-map of individual environments  the user has actual access to, but they may be an admin, so
      // if so, we need to fill those in


      if (personAdmin) {
        // now go through all the environments for this app
        List<DbEnvironment> environments = new QDbEnvironment().whenArchived.isNull().order().name.desc().parentApplication.eq(app).findList();
        // envId, DbEnvi

        environments.forEach(e -> {
          if (envs.get(e.getId().toString()) == null) {
            environmentOrderingMap.put(e.getId().toString(), e);

            final EnvironmentFeatureValues e1 =
              new EnvironmentFeatureValues()
                .environmentName(e.getName())
                .priorEnvironmentId(e.getPriorEnvironment() == null ? null : e.getPriorEnvironment().getId().toString())
                .environmentId(e.getId().toString())
                .roles(Arrays.asList(RoleType.values())) // all access (as admin)
                .features(new QDbEnvironmentFeatureStrategy()
                  .feature.whenArchived.isNull()
                  .environment.eq(e)
                  .findList().stream().map(convertUtils::toFeatureValue).collect(Collectors.toList()));

            envs.put(e1.getEnvironmentId(), e1);
          }
        });
      }

      List<EnvironmentFeatureValues> finalValues = new ArrayList<>(envs.values());

      finalValues.sort((o1, o2) -> {
          final DbEnvironment env1 = environmentOrderingMap.get(o1.getEnvironmentId());
          final DbEnvironment env2 = environmentOrderingMap.get(o2.getEnvironmentId());

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

      return
        new ApplicationFeatureValues()
          .applicationId(appId)
          .features(new QDbApplicationFeature()
            .whenArchived.isNull()
            .parentApplication.eq(app)
            .findList().stream().map(f -> convertUtils.toApplicationFeature(f, empty)).collect(Collectors.toList()))
          .environments(finalValues)
        ;
    }

    return null;
  }




  // todo: I hate this API, its way way too ICBM
  @Override
  public EnvironmentFeaturesResult lastFeatureValueChanges(Person from) {
    DbPerson dbPerson = convertUtils.uuidPerson(from);

    if (dbPerson == null) {
      return null;
    }

    EnvironmentFeaturesResult result = new EnvironmentFeaturesResult();

    // if this person is a superuser we can drop all other requirements
    boolean superuser = new QDbGroup().adminGroup.isTrue().whenArchived.isNull().owningPortfolio.isNull().findCount() > 0;
    if (!superuser) {
      // find fv's where the person is in an environment's ACL group OR
      // in an environment's applications' portfolio's admin group
      result.featureValues(new QDbEnvironmentFeatureStrategy().setMaxRows(20).or()
        .environment.groupRolesAcl.group.peopleInGroup.eq(dbPerson)
          .and().environment.parentApplication.portfolio.groups.adminGroup.isTrue().environment.parentApplication.portfolio.groups.peopleInGroup.eq(dbPerson).endAnd()
        .endOr().order().whenUpdated.desc().findList().stream().map(convertUtils::toFeatureValue).collect(Collectors.toList()));
    } else {
      result.featureValues(new QDbEnvironmentFeatureStrategy().setMaxRows(20).order("whenUpdated desc").findList().stream().map(convertUtils::toFeatureValue).collect(Collectors.toList()));
    }

    List<UUID> envIds = result.getFeatureValues().stream().map(fv -> ConvertUtils.ifUuid(fv.getEnvironmentId())).collect(Collectors.toList());
    result.environments(new QDbEnvironment().id.in(envIds).whenArchived.isNull().findList().stream().map(dbEnv -> convertUtils.toEnvironment(dbEnv, Opts.empty())).collect(Collectors.toList()));
    result.applications(new QDbApplication().environments.id.in(envIds).whenArchived.isNull().findList().stream().map(app -> convertUtils.toApplication(app, Opts.empty())).collect(Collectors.toList()));
    List<UUID> featureValueIds = result.getFeatureValues().stream().map(fv -> ConvertUtils.ifUuid(fv.getId())).collect(Collectors.toList());
    result.features(
      new QDbApplicationFeature().whenArchived.isNull().environmentFeatures.id.in(featureValueIds).findList().stream().map(appF -> convertUtils.toApplicationFeature(appF, Opts.empty())).collect(Collectors.toList()));

    return result;
  }
}
