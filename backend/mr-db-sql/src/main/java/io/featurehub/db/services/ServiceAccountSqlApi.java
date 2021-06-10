package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.DuplicateKeyException;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.ServiceAccountApi;
import io.featurehub.db.model.DbAcl;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.db.model.DbServiceAccountEnvironment;
import io.featurehub.db.model.query.QDbAcl;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbServiceAccount;
import io.featurehub.db.model.query.QDbServiceAccountEnvironment;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.mr.model.RoleType;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountPermission;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ServiceAccountSqlApi implements ServiceAccountApi {
  private static final Logger log = LoggerFactory.getLogger(ServiceAccountSqlApi.class);
  private final Database database;
  private final Conversions convertUtils;
  private final CacheSource cacheSource;
  private final ArchiveStrategy archiveStrategy;

  @Inject
  public ServiceAccountSqlApi(
      Database database,
      Conversions convertUtils,
      CacheSource cacheSource,
      ArchiveStrategy archiveStrategy) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.cacheSource = cacheSource;
    this.archiveStrategy = archiveStrategy;
  }

  @Override
  public ServiceAccount get(UUID id, Opts opts) {
    Conversions.nonNullServiceAccountId(id);

    QDbServiceAccount eq = opts(new QDbServiceAccount().id.eq(id), opts);
    return convertUtils.toServiceAccount(eq.findOne(), opts);
  }

  private QDbServiceAccount opts(QDbServiceAccount finder, Opts opts) {
    if (opts.contains(FillOpts.Permissions) || opts.contains(FillOpts.SdkURL)) {
      finder = finder.serviceAccountEnvironments.fetch();
    }
    if (!opts.contains(FillOpts.Archived)) {
      finder = finder.whenArchived.isNull();
    }
    return finder;
  }

  @Override
  public ServiceAccount update(UUID id, Person updater, ServiceAccount serviceAccount, Opts opts)
      throws OptimisticLockingException {
    Conversions.nonNullServiceAccountId(id);

    DbServiceAccount sa = new QDbServiceAccount().id.eq(id).whenArchived.isNull().findOne();

    if (sa == null) return null;

    if (serviceAccount.getVersion() == null || serviceAccount.getVersion() != sa.getVersion()) {
      throw new OptimisticLockingException();
    }

    Map<UUID, ServiceAccountPermission> updatedEnvironments = new HashMap<>();
    List<UUID> newEnvironments = new ArrayList<>();

    serviceAccount
        .getPermissions()
        .forEach(
            perm -> {
              if (perm.getEnvironmentId() != null) {
                updatedEnvironments.put(perm.getEnvironmentId(), perm);
                newEnvironments.add(perm.getEnvironmentId());
              }
            });

    List<DbServiceAccountEnvironment> deletePerms = new ArrayList<>();
    List<DbServiceAccountEnvironment> updatePerms = new ArrayList<>();
    List<DbServiceAccountEnvironment> createPerms = new ArrayList<>();

    // we drop out of this knowing which perms to delete and update
    new QDbServiceAccountEnvironment()
        .environment
        .id
        .in(updatedEnvironments.keySet())
        .serviceAccount
        .eq(sa)
        .findEach(
            upd -> {
              final UUID envId = upd.getEnvironment().getId();

              final ServiceAccountPermission perm = updatedEnvironments.get(envId);

              newEnvironments.remove(envId);

              if (perm.getPermissions() == null || perm.getPermissions().isEmpty()) {
                deletePerms.add(upd);
              } else {
                final String newPerms = convertPermissionsToString(perm.getPermissions());
                if (!newPerms.equals(upd.getPermissions())) {
                  upd.setPermissions(newPerms);
                  updatePerms.add(upd);
                }
              }
            });

    // now we need to know which perms to add
    newEnvironments.forEach(
        envId -> {
          final ServiceAccountPermission perm = updatedEnvironments.get(envId);
          if (perm.getPermissions() != null && !perm.getPermissions().isEmpty()) {
            DbEnvironment env =
                convertUtils.byEnvironment(
                    envId, Opts.opts(FillOpts.ApplicationIds, FillOpts.PortfolioIds));

            if (env != null
                && env.getParentApplication()
                    .getPortfolio()
                    .getId()
                    .equals(sa.getPortfolio().getId())) {
              createPerms.add(
                  new DbServiceAccountEnvironment.Builder()
                      .environment(env)
                      .serviceAccount(sa)
                      .permissions(convertPermissionsToString(perm.getPermissions()))
                      .build());
            }
          }
        });

    if (serviceAccount.getDescription() != null) {
      sa.setDescription(serviceAccount.getDescription());
    }

    updateServiceAccount(sa, deletePerms, updatePerms, createPerms);

    return convertUtils.toServiceAccount(sa, opts);
  }

  private String convertPermissionsToString(List<RoleType> permissions) {
    return permissions.stream().map(RoleType::toString).sorted().collect(Collectors.joining(","));
  }

  @Override
  public List<ServiceAccount> search(
      UUID pId, String filter, UUID applicationId, Person currentPerson, Opts opts) {
    Conversions.nonNullPortfolioId(pId);

    DbPerson person = convertUtils.byPerson(currentPerson);

    if (person == null) {
      return null;
    }

    DbApplication application = null;
    boolean personAdmin = false;
    if (applicationId != null) {
      application = convertUtils.byApplication(applicationId);

      if (application != null) {
        personAdmin = convertUtils.isPersonApplicationAdmin(person, application);
      }
    }

    QDbServiceAccount qFinder = opts(new QDbServiceAccount().portfolio.id.eq(pId), opts);
    if (filter != null) {
      qFinder = qFinder.name.ilike(filter);
    }

    if (application != null) {
      qFinder = qFinder.portfolio.applications.eq(application);

      qFinder = opts(qFinder, opts);
    }

    final Opts updatedOpts = opts;

    // if they are an admin they have everything, otherwise spelunk through finding relevant ACLs
    final List<DbAcl> environmentPermissions = new ArrayList<>();

    if (!personAdmin
        && application != null
        && (opts.contains(FillOpts.Permissions) || opts.contains(FillOpts.SdkURL))) {
      environmentPermissions.addAll(
          new QDbAcl()
              .roles
              .notEqualTo("")
              .environment
              .parentApplication
              .eq(application)
              .group
              .peopleInGroup
              .eq(person)
              .environment
              .fetch(QDbEnvironment.Alias.id)
              .findList());
    }

    return qFinder.findList().stream()
        .map(sa -> convertUtils.toServiceAccount(sa, updatedOpts, environmentPermissions))
        .collect(Collectors.toList());
  }

  @Override
  public ServiceAccount resetApiKey(UUID id) {
    Conversions.nonNullServiceAccountId(id);

    DbServiceAccount sa = new QDbServiceAccount().id.eq(id).whenArchived.isNull().findOne();
    if (sa == null) return null;

    sa.setApiKeyServerEval(newServerEvalKey());
    sa.setApiKeyClientEval(newClientEvalKey());
    updateOnlyServiceAccount(sa);
    asyncUpdateCache(sa, null);
    return convertUtils.toServiceAccount(sa, Opts.empty());
  }

  @Transactional
  public void cleanupServiceAccountApiKeys() {
    if (new QDbServiceAccount()
            .or()
            .apiKeyClientEval
            .isNull()
            .apiKeyServerEval
            .isNull()
            .endOr()
            .exists()) {
      log.info("Updating service account keys as incomplete.");
      new QDbServiceAccount()
          .or()
          .apiKeyClientEval
          .isNull()
          .apiKeyServerEval
          .isNull()
          .endOr()
          .findEach(
              sa -> {
                boolean updated = false;
                if (sa.getApiKeyClientEval() == null) {
                  updated = true;
                  sa.setApiKeyClientEval(newClientEvalKey());
                }
                if (sa.getApiKeyServerEval() == null) {
                  updated = true;
                  sa.setApiKeyServerEval(newServerEvalKey());
                }
                if (updated) {
                  database.update(sa);
                }
              });
    }
  }

  private String newServerEvalKey() {
    return RandomStringUtils.randomAlphanumeric(40);
  }

  private String newClientEvalKey() {
    return RandomStringUtils.randomAlphanumeric(30)
        + "*"
        + RandomStringUtils.randomAlphanumeric(20);
  }

  @Override
  public ServiceAccount create(
      UUID portfolioId, Person creator, ServiceAccount serviceAccount, Opts opts)
      throws DuplicateServiceAccountException {
    Conversions.nonNullPortfolioId(portfolioId);
    Conversions.nonNullPerson(creator);

    DbPerson who = convertUtils.byPerson(creator);
    DbPortfolio portfolio = convertUtils.byPortfolio(portfolioId);

    if (who == null || portfolio == null) return null;

    List<DbEnvironment> changedEnvironments = new ArrayList<>();
    Map<UUID, DbEnvironment> envs = environmentMap(serviceAccount);

    // now where we actually find the environment, add it into the list
    Set<DbServiceAccountEnvironment> perms =
        serviceAccount.getPermissions().stream()
            .map(
                sap -> {
                  if (sap.getEnvironmentId() != null) {
                    DbEnvironment e = envs.get(sap.getEnvironmentId());
                    if (e != null) {
                      changedEnvironments.add(e);
                      return new DbServiceAccountEnvironment.Builder()
                          .environment(e)
                          .permissions(convertPermissionsToString(sap.getPermissions()))
                          .build();
                    }
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // now create the SA and attach the perms to form the links
    DbServiceAccount sa =
        new DbServiceAccount.Builder()
            .name(serviceAccount.getName())
            .description(serviceAccount.getDescription())
            .whoChanged(who)
            .apiKeyServerEval(newServerEvalKey())
            .apiKeyClientEval(newClientEvalKey())
            .serviceAccountEnvironments(perms)
            .portfolio(portfolio)
            .build();

    perms.forEach(p -> p.setServiceAccount(sa));

    try {
      save(sa);

      asyncUpdateCache(sa, changedEnvironments);
    } catch (DuplicateKeyException dke) {
      log.warn("Duplicate service account {}", sa.getName(), dke);
      throw new DuplicateServiceAccountException();
    }

    return convertUtils.toServiceAccount(sa, opts);
  }

  private Map<UUID, DbEnvironment> environmentMap(ServiceAccount serviceAccount) {
    // find all of the UUIDs in the environment list
    List<UUID> envIds =
        serviceAccount.getPermissions().stream()
            .map(ServiceAccountPermission::getEnvironmentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    // now find them in the db in one swoop using "in" syntax
    return new QDbEnvironment()
        .id.in(envIds).whenArchived.isNull().findList().stream()
            .collect(Collectors.toMap(DbEnvironment::getId, Function.identity()));
  }

  @Transactional
  private void save(DbServiceAccount sa) {
    database.save(sa);
  }

  @Transactional
  private void updateOnlyServiceAccount(DbServiceAccount sa) {
    database.update(sa);
  }

  @Transactional
  private void updateServiceAccount(
      DbServiceAccount sa,
      List<DbServiceAccountEnvironment> deleted,
      List<DbServiceAccountEnvironment> updated,
      List<DbServiceAccountEnvironment> created) {
    database.update(sa);

    database.updateAll(updated);
    database.deleteAll(deleted);
    database.saveAll(created);

    Map<UUID, DbEnvironment> changed = new HashMap<>();

    deleted.forEach(e -> changed.put(e.getEnvironment().getId(), e.getEnvironment()));
    updated.forEach(e -> changed.put(e.getEnvironment().getId(), e.getEnvironment()));
    created.forEach(e -> changed.put(e.getEnvironment().getId(), e.getEnvironment()));

    asyncUpdateCache(sa, changed.values());
  }

  // because this is an update or save, its no problem we send this out of band of this save/update.
  private void asyncUpdateCache(
      DbServiceAccount sa, Collection<DbEnvironment> changedEnvironments) {
    cacheSource.updateServiceAccount(sa, PublishAction.UPDATE);

    if (changedEnvironments != null && !changedEnvironments.isEmpty()) {
      changedEnvironments.forEach(e -> cacheSource.updateEnvironment(e, PublishAction.UPDATE));
    }
  }

  @Override
  @Transactional
  public Boolean delete(Person deleter, UUID servcieAccountId) {
    Conversions.nonNullServiceAccountId(servcieAccountId);

    DbServiceAccount sa =
        new QDbServiceAccount().id.eq(servcieAccountId).whenArchived.isNull().findOne();
    if (sa != null) {
      archiveStrategy.archiveServiceAccount(sa);
      return Boolean.TRUE;
    }

    return Boolean.FALSE;
  }
}
