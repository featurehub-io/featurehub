package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.FeatureApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.PersonFeaturePermission;
import io.featurehub.db.api.RolloutStrategyValidator;
import io.featurehub.db.listener.FeatureUpdateBySDKApi;
import io.featurehub.db.model.DbAcl;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbFeatureValue;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.query.QDbAcl;
import io.featurehub.db.model.query.QDbApplicationFeature;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbFeatureValue;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.db.services.strategies.StrategyDiffer;
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
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class FeatureSqlApi implements FeatureApi, FeatureUpdateBySDKApi {
  private static final Logger log = LoggerFactory.getLogger(FeatureSqlApi.class);
  private final Database database;
  private final Conversions convertUtils;
  private final CacheSource cacheSource;
  private final RolloutStrategyValidator rolloutStrategyValidator;
  private final StrategyDiffer strategyDiffer;

  @Inject
  public FeatureSqlApi(Database database, Conversions convertUtils, CacheSource cacheSource,
                       RolloutStrategyValidator rolloutStrategyValidator, StrategyDiffer strategyDiffer) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.cacheSource = cacheSource;
    this.rolloutStrategyValidator = rolloutStrategyValidator;
    this.strategyDiffer = strategyDiffer;
  }

  @Override
  public FeatureValue createFeatureValueForEnvironment(UUID eId, String key, FeatureValue featureValue,
                                                       PersonFeaturePermission person)
    throws OptimisticLockingException, RolloutStrategyValidator.InvalidStrategyCombination, NoAppropriateRole {
    Conversions.nonNullEnvironmentId(eId);

    if (featureValue == null) {
      throw new IllegalArgumentException("featureValue is null and must be provided");
    }

    if (person == null) {
      throw new IllegalArgumentException("person with permission info must not be null");
    }

    if (!person.hasWriteRole()) {
      DbEnvironment env = new QDbEnvironment().id.eq(eId).whenArchived.isNull().findOne();
      log.warn("User has no roles for environment {} key {}", eId, key);
      if (env == null) {
        log.error("could not find environment or environment is archived");
      } else {
        log.error("error env {} app {} portfolio {}", env.getName(), env.getParentApplication().getName(), env.getParentApplication().getPortfolio().getName());
      }

      throw new NoAppropriateRole();
    }

    rolloutStrategyValidator.validateStrategies(featureValue.getRolloutStrategies(),
      featureValue.getRolloutStrategyInstances()).hasFailedValidation();

    final DbFeatureValue dbFeatureValue = new QDbFeatureValue().environment.id.eq(eId).feature.key.eq(key).findOne();
    if (dbFeatureValue != null) {
      // this is an update not a create, environment + app-feature key exists
      return onlyUpdateFeatureValueForEnvironment(featureValue, person, dbFeatureValue);
    } else if (person.hasChangeValueRole() || person.hasLockRole() || person.hasUnlockRole()) {
      return onlyCreateFeatureValueForEnvironment(eId, key, featureValue, person);
    } else {
      log.info("roles for person are {} and are not enough for environment {} and key {}", person.toString(), eId, key);
      throw new NoAppropriateRole();
    }
  }

  private FeatureValue onlyCreateFeatureValueForEnvironment(UUID eid, String key, FeatureValue featureValue,
                                                            PersonFeaturePermission person) throws NoAppropriateRole {
    final DbEnvironment val = convertUtils.byEnvironment(eid);

    if (val != null) {
      final DbApplicationFeature appFeature = new QDbApplicationFeature().key.eq(key).parentApplication.environments.eq(val).findOne();

      if (appFeature != null) {
        if (strategyDiffer.invalidStrategyInstances(featureValue.getRolloutStrategyInstances(), appFeature)) {
          log.error("Invalid rollout strategy instances");
          return null;
        }

        DbFeatureValue dbFeatureValue = new DbFeatureValue.Builder()
          .environment(val)
          .feature(appFeature)
          .build();

        updateFeatureValue(featureValue, person, dbFeatureValue);

        save(dbFeatureValue);

        return convertUtils.toFeatureValue(dbFeatureValue);
      } else {
        log.error("Attempted to create feature value in environment `{}` where feature key did not exist: `{}`", eid, key);
      }
    }

    return null;
  }


  @Transactional
  private void save(DbFeatureValue featureValue) {
    database.save(featureValue);

    cacheSource.publishFeatureChange(featureValue);
  }

  @Override
  @Transactional
  public boolean deleteFeatureValueForEnvironment(UUID eId, String key) {
    Conversions.nonNullEnvironmentId(eId);

    DbFeatureValue strategy = new QDbFeatureValue().environment.id.eq(eId).feature.key.eq(key).findOne();

    if (strategy != null) {
      cacheSource.deleteFeatureChange(strategy.getFeature(), strategy.getEnvironment().getId());

      return database.delete(strategy);
    }

    return false;
  }

  private FeatureValue onlyUpdateFeatureValueForEnvironment(FeatureValue featureValue, PersonFeaturePermission person, DbFeatureValue strategy) throws OptimisticLockingException, NoAppropriateRole {
    if (featureValue.getVersion() == null || strategy.getVersion() != featureValue.getVersion()) {
      throw new OptimisticLockingException();
    }

    if (strategyDiffer.invalidStrategyInstances(featureValue.getRolloutStrategyInstances(), strategy.getFeature())) {
      log.error("Invalid rollout strategy instances");
      return null;
    }

    updateFeatureValue(featureValue, person, strategy);

    save(strategy);

    return convertUtils.toFeatureValue(strategy);
  }

  private void updateFeatureValue(FeatureValue featureValue, PersonFeaturePermission person, DbFeatureValue strategy) throws NoAppropriateRole {
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

      if (featureValue.getRolloutStrategies() != null) {
        featureValue.getRolloutStrategies().forEach(rs -> {
          if (rs.getId() == null) {
            rs.setId(UUID.randomUUID().toString());
          }
        });
      }

      strategy.setRolloutStrategies(featureValue.getRolloutStrategies());

      strategyDiffer.createDiff(featureValue, strategy);
    }

    // change locked before changing value, as may not be able to change value if locked
    boolean newValue = featureValue.getLocked() != null && featureValue.getLocked();
    if (newValue != strategy.isLocked()) {
      if (!newValue && person.hasUnlockRole()) {
        strategy.setLocked(false);
      } else if (newValue && person.hasLockRole()) {
        strategy.setLocked(true);
      } else {
        throw new NoAppropriateRole();
      }
    }

    strategy.setWhoUpdated(convertUtils.byPerson(person.person));
    if (strategy.getWhoUpdated() == null) {
      log.error("Unable to set who updated on strategy {}", person.person);
    }
  }

  @Override
  public FeatureValue updateFeatureValueForEnvironment(UUID eid, String key, FeatureValue featureValue,
                                                       PersonFeaturePermission person) throws OptimisticLockingException, NoAppropriateRole,
     RolloutStrategyValidator.InvalidStrategyCombination {
    return createFeatureValueForEnvironment(eid, key, featureValue, person);
  }

  @Override
  public FeatureValue getFeatureValueForEnvironment(UUID eid, String key) {
    Conversions.nonNullEnvironmentId(eid);

    final DbFeatureValue strategy =
      new QDbFeatureValue().environment.id.eq(eid).feature.key.eq(key).sharedRolloutStrategies.fetch().findOne();
    return strategy == null ? null : convertUtils.toFeatureValue(strategy);
  }

  @Override
  public EnvironmentFeaturesResult getAllFeatureValuesForEnvironment(UUID eId) {
    Conversions.nonNullEnvironmentId(eId);
    return new EnvironmentFeaturesResult()
      .featureValues(new QDbFeatureValue().environment.id.eq(eId)
        .feature.whenArchived.isNull().findList().stream().map(convertUtils::toFeatureValue).collect(Collectors.toList()))
        .environments(Collections.singletonList(convertUtils.toEnvironment(new QDbEnvironment().id.eq(eId).findOne(), Opts.empty())));
  }

  // we are going to have to put a transaction at this level as we want the whole thing to roll back if there is an issue
  @Override
  @Transactional
  public List<FeatureValue> updateAllFeatureValuesForEnvironment(UUID eId, List<FeatureValue> featureValues,
                                                                 PersonFeaturePermission person)
    throws OptimisticLockingException, NoAppropriateRole,  RolloutStrategyValidator.InvalidStrategyCombination {
    Conversions.nonNullEnvironmentId(eId);

    if (featureValues == null || featureValues.size() != featureValues.stream().map(FeatureValue::getKey).collect(Collectors.toSet()).size()) {
      throw new IllegalArgumentException("Invalid update dataset");
    }

    // ensure the strategies are valid from a conceptual perspective
    RolloutStrategyValidator.ValidationFailure failure = new RolloutStrategyValidator.ValidationFailure();

    for (FeatureValue fv : featureValues) {
      rolloutStrategyValidator.validateStrategies(fv.getRolloutStrategies(), fv.getRolloutStrategyInstances(), failure);
    }

    failure.hasFailedValidation();

    final List<DbFeatureValue> existing = new QDbFeatureValue().environment.id.eq(eId).feature.whenArchived.isNull().findList();
    final Map<String, FeatureValue> newValues = featureValues.stream().collect(Collectors.toMap(FeatureValue::getKey, Function.identity()));
    // take them all and remove all fv's we were passed, leaving only EFS's we want to remove
    final Set<String> deleteKeys = existing.stream().map(e -> e.getFeature().getKey()).collect(Collectors.toSet());
    for (FeatureValue fv : featureValues) {
      deleteKeys.remove(fv.getKey());
    }

    // we should be left with only keys in deleteKeys that do not exist in the passed in list of feature values
    // and in addingKeys we should be given a list of keys which exist in the passed in FV's but didn't exist in the db
    List<DbFeatureValue> deleteStrategies = new ArrayList<>();
    for (DbFeatureValue strategy : existing) {
      if (deleteKeys.contains(strategy.getFeature().getKey())) {
        if (strategy.getFeature().getValueType() != FeatureValueType.BOOLEAN) {
          deleteStrategies.add(strategy); // can't delete booleans
        }
      } else {
        FeatureValue fv = newValues.remove(strategy.getFeature().getKey());
        onlyUpdateFeatureValueForEnvironment(fv, person, strategy);
      }
    }

    // now for the creates
    for (String key : newValues.keySet()) {
      FeatureValue fv = newValues.get(key);
      onlyCreateFeatureValueForEnvironment(eId, key, fv, person);
    }

    if (!deleteStrategies.isEmpty()) {
      publishTheRemovalOfABunchOfStrategies(deleteStrategies);

      database.deleteAll(deleteStrategies);
    }

    return new QDbFeatureValue().environment.id.eq(eId).feature.whenArchived.isNull().findList().stream().map(convertUtils::toFeatureValue).collect(Collectors.toList());
  }

  // can't background this because they will deleted shortly
  private void publishTheRemovalOfABunchOfStrategies(Collection<DbFeatureValue> deleteStrategies) {
    if (!deleteStrategies.isEmpty()) {
      deleteStrategies.parallelStream().forEach(strategy -> cacheSource.deleteFeatureChange(strategy.getFeature(), strategy.getEnvironment().getId()));
    }
  }

  @Override
  public void updateFeature(String sdkUrl, UUID eid, String featureKey, boolean updatingValue,
                            Function<FeatureValueType, FeatureValue> buildFeatureValue)
      throws RolloutStrategyValidator.InvalidStrategyCombination {
    Conversions.nonNullEnvironmentId(eid);

    // not checking permissions, edge checks those
    DbApplicationFeature feature = new QDbApplicationFeature().parentApplication.environments.id.eq(eid).key.eq(featureKey).findOne();

    if (feature == null) return;

    DbFeatureValue fv = new QDbFeatureValue().environment.id.eq(eid).feature.eq(feature).findOne();

    FeatureValue newValue = buildFeatureValue.apply(feature.getValueType());

    rolloutStrategyValidator.validateStrategies(newValue.getRolloutStrategies(),
      newValue.getRolloutStrategyInstances()).hasFailedValidation();

    boolean saveNew = (fv == null);

    if (saveNew) { // creating
      fv = new DbFeatureValue.Builder()
        .environment(new QDbEnvironment().id.eq(eid).findOne())
        .feature(feature)
        .locked(true)
        .build();
    }

    if (updatingValue) {
      switch (fv.getFeature().getValueType()) {
        case BOOLEAN:
          fv.setDefaultValue(newValue.getValueBoolean() == null ? Boolean.FALSE.toString() : newValue.getValueBoolean().toString());
          break;
        case STRING:
          fv.setDefaultValue(newValue.getValueString());
          break;
        case NUMBER:
          fv.setDefaultValue(newValue.getValueNumber() == null ? null : newValue.getValueNumber().toString());
          break;
        case JSON:
          fv.setDefaultValue(newValue.getValueJson());
          break;
      }
    }

    if (newValue.getLocked() != null) {
      fv.setLocked(newValue.getLocked());
    }

    // API can never change strategies
    save(fv);
  }

  static class EnvironmentsAndStrategies {
    public Map<UUID, DbFeatureValue> strategies;
    public Map<UUID, List<RoleType>> roles;
    public Map<UUID, DbEnvironment> environments;
    public Set<ApplicationRoleType> appRolesForThisPerson;

    public EnvironmentsAndStrategies(Map<UUID, DbFeatureValue> strategies, Map<UUID, List<RoleType>> roles, Map<UUID, DbEnvironment> environments, Set<ApplicationRoleType> appRoles) {
      this.strategies = strategies;
      this.roles = roles;
      this.environments = environments;
      this.appRolesForThisPerson = appRoles;
    }
  }


  private EnvironmentsAndStrategies strategiesUserCanAccess(UUID appId, String key, Person person) {

    DbPerson dbPerson = convertUtils.byPerson(person);
    DbApplication app = convertUtils.byApplication(appId);

    if (app == null || app.getWhenArchived() != null || dbPerson == null || dbPerson.getWhenArchived() != null) {
      return null;
    }

    Map<UUID, DbFeatureValue> strategiesResult = new HashMap<>();
    Map<UUID, List<RoleType>> roles = new HashMap<>();
    Map<UUID, DbEnvironment> environments = new HashMap<>();

    boolean personAdmin = convertUtils.isPersonApplicationAdmin(dbPerson, app);

    Map<UUID, DbFeatureValue> strategies =
      new QDbFeatureValue().feature.key.eq(key).feature.parentApplication
        .eq(app)
        .findList()
        .stream()
        .collect(Collectors.toMap(e -> e.getEnvironment().getId(), Function.identity()));

    final List<RoleType> adminRoles = Arrays.asList(RoleType.values());

    if (!personAdmin) { // is they aren't a portfolio admin, figure out what their permissions are to each environment
      new QDbAcl().environment.parentApplication.eq(app).group.peopleInGroup.eq(dbPerson).findList().forEach(fe -> {
        log.debug("Found environment `{}`, app `{}`, group `{}`, roles `{}`",
          fe.getEnvironment() == null ? "<none>" : fe.getEnvironment().getName(),
          fe.getApplication() == null ? "<none>" : fe.getApplication().getName(),
          fe.getGroup().getName(), fe.getRoles());
        final List<RoleType> roleTypes = convertUtils.splitEnvironmentRoles(fe.getRoles());

        if (roleTypes != null && !roleTypes.isEmpty()) {
          UUID envId = fe.getEnvironment().getId();
          roles.put(envId, roleTypes);
          environments.put(envId, fe.getEnvironment());

          DbFeatureValue strategy = strategies.remove(envId);

          if (strategy != null) {
            strategiesResult.put(envId, strategy);
          }
        }
      });
    }

    // they have at least one environment or they are an admin
    if (environments.size() > 0 || personAdmin) {
      final List<RoleType> emptyRoles = Collections.emptyList();
      new QDbEnvironment().parentApplication.eq(app).findList().forEach(env -> {
        if (environments.get(env.getId()) == null) {
          environments.put(env.getId(), env);
          roles.put(env.getId(), personAdmin ? adminRoles : emptyRoles);
          if (personAdmin) {
            DbFeatureValue strategy = strategies.remove(env.getId());

            if (strategy != null) {
              strategiesResult.put(env.getId(), strategy);
            }
          }
        }
      });
    }

    Set<ApplicationRoleType> appRoles =
      new QDbAcl().application.isNotNull()
        .select(QDbAcl.Alias.roles).roles.isNotNull()
        .group.whenArchived.isNull()
        .group.owningPortfolio.eq(app.getPortfolio())
        .group.peopleInGroup.eq(dbPerson).findList().stream()
        .map(appAcl -> convertUtils.splitApplicationRoles(appAcl.getRoles()))
        .flatMap(List::stream).collect(Collectors.toSet());

    return new EnvironmentsAndStrategies(strategiesResult, roles, environments, appRoles);
  }

  @Override
  public List<FeatureEnvironment> getFeatureValuesForApplicationForKeyForPerson(UUID appId, String key, Person person) {
    Conversions.nonNullApplicationId(appId);
    EnvironmentsAndStrategies result = strategiesUserCanAccess(appId, key, person);

    if (result != null) {
      return result.environments.keySet().stream()
        .map(e -> convertUtils.toFeatureEnvironment(result.strategies.get(e), result.roles.get(e),
            result.environments.get(e), Opts.opts(FillOpts.ServiceAccounts)))
        .collect(Collectors.toList());
    }

    return null;
  }

  @Override
  @Transactional
  public void updateAllFeatureValuesByApplicationForKey(UUID id, String key, List<FeatureValue> featureValue,
              Person person, boolean removeValuesNotPassed) throws OptimisticLockingException, NoAppropriateRole,
     RolloutStrategyValidator.InvalidStrategyCombination {
    // prevalidate, this will happen again but we should do it before anything else
    Conversions.nonNullApplicationId(id);

    if (featureValue == null) {
      throw new IllegalArgumentException("featureValue is required");
    }

    RolloutStrategyValidator.ValidationFailure failure = new RolloutStrategyValidator.ValidationFailure();

    for (FeatureValue fv : featureValue) {
      rolloutStrategyValidator.validateStrategies(fv.getRolloutStrategies(), fv.getRolloutStrategyInstances(), failure);
    }

    failure.hasFailedValidation();

    EnvironmentsAndStrategies result = strategiesUserCanAccess(id, key, person);

    if (result != null) {
      // environment id -> role in that environment
      Map<UUID, List<RoleType>> environmentToRoleMap = result.roles;
      // environment -> feature value
      Map<UUID, DbFeatureValue> strategiesToDelete = result.strategies;

      for (FeatureValue fv : featureValue) {
        UUID envId = fv.getEnvironmentId();
        if (envId == null) {
          log.warn("Trying to update for environment `{}` and environment id is invalid.", fv.getEnvironmentId());
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
      .environmentId(acl.getEnvironment().getId())
      .environmentName(acl.getEnvironment().getName())
      .priorEnvironmentId(acl.getEnvironment().getPriorEnvironment() == null ? null : acl.getEnvironment().getPriorEnvironment().getId())
      .roles(roles)
      .features(new QDbFeatureValue()
        .environment.eq(acl.getEnvironment())
        .environment.whenArchived.isNull()
        .feature.whenArchived.isNull()
        .findList().stream().map(convertUtils::toFeatureValue).collect(Collectors.toList()));
  }

  @Override
  public ApplicationFeatureValues findAllFeatureAndFeatureValuesForEnvironmentsByApplication(UUID appId,
                                                                                             Person person) {
    Conversions.nonNullApplicationId(appId);
    Conversions.nonNullPerson(person);

    DbPerson dbPerson = convertUtils.byPerson(person);
    DbApplication app = convertUtils.byApplication(appId);

    if (app != null && dbPerson != null && app.getWhenArchived() == null && dbPerson.getWhenArchived() == null) {
      final Opts empty = Opts.empty();

      boolean personAdmin = convertUtils.isPersonApplicationAdmin(dbPerson, app);

      Map<UUID, DbEnvironment> environmentOrderingMap = new HashMap<>();
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
        .peek(acl -> environmentOrderingMap.put(acl.getEnvironment().getId(), acl.getEnvironment()))
        .map(acl -> environmentToFeatureValues(acl, personAdmin))
        .filter(Objects::nonNull)
        .filter(efv -> !efv.getRoles().isEmpty())

        .collect(Collectors.toList());

//      Set<String> envs = permEnvs.stream().map(EnvironmentFeatureValues::getEnvironmentId).distinct().collect(Collectors.toSet());

      Map<UUID, EnvironmentFeatureValues> envs = new HashMap<>();

      // merge any duplicates, this occurs because the database query can return duplicate lines
      permEnvs.forEach(e -> {
        EnvironmentFeatureValues original = envs.get(e.getEnvironmentId());
        if (original != null) { // merge them
          Set<UUID> originalFeatureValueIds =
            original.getFeatures().stream().map(FeatureValue::getId).collect(Collectors.toSet());
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


      if (!permEnvs.isEmpty() || personAdmin) {
        // now go through all the environments for this app
        List<DbEnvironment> environments = new QDbEnvironment().whenArchived.isNull().order().name.desc().parentApplication.eq(app).findList();
        // envId, DbEnvi

        List<RoleType> roles = personAdmin ? Arrays.asList(RoleType.values()) : new ArrayList<>();

        environments.forEach(e -> {
          if (envs.get(e.getId()) == null) {
            environmentOrderingMap.put(e.getId(), e);

            final EnvironmentFeatureValues e1 =
              new EnvironmentFeatureValues()
                .environmentName(e.getName())
                .priorEnvironmentId(e.getPriorEnvironment() == null ? null : e.getPriorEnvironment().getId())
                .environmentId(e.getId())
                .roles(roles) // all access (as admin)
                .features(!personAdmin ? new ArrayList<>() : new QDbFeatureValue()
                  .feature.whenArchived.isNull()
                  .environment.eq(e)
                  .findList().stream()
                    .map(convertUtils::toFeatureValue).collect(Collectors.toList()));

            envs.put(e1.getEnvironmentId(), e1);
          }
        });
      }

      // we have to get all of them and sort them into order because this person may not have access
      // to all of them, so we will lose the sort order if we try and order them
      // so we get them all, sort them, and then pick them out of the map one by one
      final List<DbEnvironment> sortingEnvironments =
        new ArrayList<>(new QDbEnvironment().select(QDbEnvironment.Alias.id).parentApplication.id.eq(appId).findList());

      EnvironmentUtils.sortEnvironments(sortingEnvironments);

      List<EnvironmentFeatureValues> finalValues = new ArrayList<>();

      sortingEnvironments.forEach(e -> {
        final EnvironmentFeatureValues efv = envs.get(e.getId());
        if (efv != null) {
          finalValues.add(efv);
        }
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
}
