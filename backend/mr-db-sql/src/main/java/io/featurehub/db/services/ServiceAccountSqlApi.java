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
import io.featurehub.mr.model.RoleType;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbServiceAccount;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountPermission;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
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
  private final ConvertUtils convertUtils;
  private final CacheSource cacheSource;
  private final ArchiveStrategy archiveStrategy;

  @Inject
  public ServiceAccountSqlApi(Database database, ConvertUtils convertUtils, CacheSource cacheSource, ArchiveStrategy archiveStrategy) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.cacheSource = cacheSource;
    this.archiveStrategy = archiveStrategy;
  }

  @Override
  public ServiceAccount get(String saId, Opts opts) {
    UUID id = ConvertUtils.ifUuid(saId);

    if (id != null) {
      QDbServiceAccount eq = opts(new QDbServiceAccount().id.eq(id), opts);
      return convertUtils.toServiceAccount(eq.findOne(), opts);
    }

    return null;
  }

  private QDbServiceAccount opts(QDbServiceAccount finder, Opts opts) {
    if (opts.contains(FillOpts.Permissions) || opts.contains(FillOpts.SdkURL) ) {
      finder = finder.serviceAccountEnvironments.fetch();
    }
    if (!opts.contains(FillOpts.Archived)) {
      finder = finder.whenArchived.isNull();
    }
    return finder;
  }

  static class EnvironmentChange {
    private DbEnvironment environment;

    public EnvironmentChange(DbEnvironment environment) {
      this.environment = environment;
    }

    public DbEnvironment getEnvironment() {
      return environment;
    }
  }


  @Override
  public ServiceAccount update(String saId, Person updater, ServiceAccount serviceAccount, Opts opts) throws OptimisticLockingException {
    UUID id = ConvertUtils.ifUuid(saId);
    if (id != null) {
      DbServiceAccount sa = new QDbServiceAccount().id.eq(id).whenArchived.isNull().findOne();

      if (sa != null) {
        if (serviceAccount.getVersion() == null || serviceAccount.getVersion() != sa.getVersion()) {
          throw new OptimisticLockingException();
        }

        // lets see what environments they want to retain (or add)
        Map<String, DbEnvironment> envs = environmentMap(serviceAccount);
        Map<String, ServiceAccountPermission> perms =  // environment id  -> SAP mapping
          serviceAccount.getPermissions().stream().collect(Collectors.toMap(ServiceAccountPermission::getEnvironmentId, Function.identity()));

        List<DbServiceAccountEnvironment> deleteEnvironments = new ArrayList<>();

        // we end up with `envs` holding the environments this update needs and
        // deleteEnvironments containing those we should remove from this service account
        List<EnvironmentChange> changedEnvironments = new ArrayList<>();
        for (DbServiceAccountEnvironment sae : sa.getServiceAccountEnvironments()) {
          DbEnvironment env = envs.remove(sae.getEnvironment().getId().toString());
          if (env == null) {
            changedEnvironments.add(new EnvironmentChange(sae.getEnvironment()));
            deleteEnvironments.add(sae);
          } else { // we are keeping this one, so find it again in the SA and copy the new permissions in
            ServiceAccountPermission perm = perms.get(env.getId().toString());
            final String newPerms = convertPermissionsToString(perm.getPermissions());
            if (!newPerms.equals(sae.getPermissions())) {
              changedEnvironments.add(new EnvironmentChange(env));
            }
            sae.setPermissions(newPerms);
          }
        }

        database.deleteAll(deleteEnvironments);

        // and now add all the new ones, just like create
        envs.values().forEach(env -> {
          String envId = env.getId().toString();
          ServiceAccountPermission perm = perms.get(envId);
          changedEnvironments.add(new EnvironmentChange(env));
          sa.getServiceAccountEnvironments().add(
            new DbServiceAccountEnvironment.Builder()
              .environment(envs.get(envId))
              .permissions(convertPermissionsToString(perm.getPermissions()))
              .build());
        });

        sa.setDescription(serviceAccount.getDescription());

        update(sa, changedEnvironments);

        return convertUtils.toServiceAccount(sa, opts);
      }
    }

    return null;
  }

  private String convertPermissionsToString(List<RoleType> permissions) {
    return permissions.stream().map(RoleType::toString).sorted().collect(Collectors.joining(","));
  }

  @Override
  public List<ServiceAccount> search(String portfolioId, String filter, String applicationId, Person currentPerson, Opts opts) {
    UUID pId = ConvertUtils.ifUuid(portfolioId);
    DbPerson personId = convertUtils.uuidPerson(currentPerson);

    if (pId == null || personId == null) {
      return null;
    }

    DbApplication application = null;
    boolean personAdmin = false;
    if (applicationId != null) {
      application = convertUtils.uuidApplication(applicationId);

      if (application != null) {
        personAdmin = convertUtils.isPersonApplicationAdmin(personId, application);
      }
    }


    QDbServiceAccount qFinder = opts(new QDbServiceAccount().portfolio.id.eq(pId), opts);
    if (filter != null) {
      qFinder = qFinder.name.ilike(filter);
    }

    if (application != null) {
      qFinder = qFinder
        .portfolio.applications.eq(application);

      qFinder = opts(qFinder, opts);
    }

    final Opts updatedOpts = opts;

    // if they are an admin they have everything, otherwise spelunk through finding relevant ACLs
    final List<DbAcl> environmentPermissions = new ArrayList<>();

    if (!personAdmin && application != null && (opts.contains(FillOpts.Permissions) || opts.contains(FillOpts.SdkURL))) {
      environmentPermissions.addAll(new QDbAcl()
        .roles.notEqualTo("")
        .environment.parentApplication.eq(application)
        .group.peopleInGroup.eq(personId)
        .environment.fetch(QDbEnvironment.Alias.id).findList());
    }

    return qFinder.findList().stream().map(sa -> convertUtils.toServiceAccount(sa, updatedOpts, environmentPermissions)).collect(Collectors.toList());
  }

  @Override
  public ServiceAccount resetApiKey(String saId) {
    UUID id = ConvertUtils.ifUuid(saId);
    if (id != null) {
      DbServiceAccount sa = new QDbServiceAccount().id.eq(id).whenArchived.isNull().findOne();
      if (sa != null) {
        sa.setApiKey(RandomStringUtils.random(80));
        update(sa, null);
        return convertUtils.toServiceAccount(sa, Opts.empty());
      }
    }

    return null;
  }

  @Override
  public ServiceAccount create(String portfolioId, Person creator, ServiceAccount serviceAccount, Opts opts)
    throws DuplicateServiceAccountException {
    DbPerson who = convertUtils.uuidPerson(creator);
    DbPortfolio portfolio = convertUtils.uuidPortfolio(portfolioId);

    if (who != null && portfolio != null) {
      List<EnvironmentChange> changedEnvironments = new ArrayList<>();
      Map<String, DbEnvironment> envs = environmentMap(serviceAccount);

      // now where we actually find the environment, add it into the list
      Set<DbServiceAccountEnvironment> perms =
      serviceAccount.getPermissions().stream().map(sap -> {
        if (sap.getEnvironmentId() != null) {
          DbEnvironment e = envs.get(sap.getEnvironmentId());
          if (e != null) {
            changedEnvironments.add(new EnvironmentChange(e));
            return new DbServiceAccountEnvironment.Builder()
              .environment(e)
              .permissions(convertPermissionsToString(sap.getPermissions()))
              .build();
          }
        }
        return null;
      }).filter(Objects::nonNull).collect(Collectors.toSet());

        // now create the SA and attach the perms to form the links
      DbServiceAccount sa = new DbServiceAccount.Builder()
        .name(serviceAccount.getName())
        .description(serviceAccount.getDescription())
        .whoChanged(who)
        .apiKey(RandomStringUtils.randomAlphanumeric(80))
        .serviceAccountEnvironments(perms)
        .portfolio(portfolio)
        .build();

      perms.forEach(p -> p.setServiceAccount(sa));

      try {
        save(sa, changedEnvironments);

        asyncUpdateCache(sa, changedEnvironments);
      } catch (DuplicateKeyException dke) {
        log.warn("Duplicate service account {}", sa.getName(), dke);
        throw new DuplicateServiceAccountException();
      }


      return convertUtils.toServiceAccount(sa, opts);
    }

    return null;
  }

  private Map<String, DbEnvironment> environmentMap(ServiceAccount serviceAccount) {
    // find all of the UUIDs in the environment list
    List<UUID> envIds = serviceAccount.getPermissions().stream().map(sap -> {
      if (sap.getEnvironmentId() != null) {
        return ConvertUtils.ifUuid(sap.getEnvironmentId());
      }
      return null;
    }).filter(Objects::nonNull).collect(Collectors.toList());

    // now find them in the db in one swoop using "in" syntax
    return new QDbEnvironment().id.in(envIds).whenArchived.isNull().findList().stream()
      .collect(Collectors.toMap(e -> e.getId().toString(), Function.identity()));
  }

  @Transactional
  private void save(DbServiceAccount sa, List<EnvironmentChange> changedEnvironments) {
    database.save(sa);

  }

  @Transactional
  private void update(DbServiceAccount sa, List<EnvironmentChange> changedEnvironments) {
    database.update(sa);

    asyncUpdateCache(sa, changedEnvironments);
  }

  // because this is an update or save, its no problem we send this out of band of this save/update.
  private void asyncUpdateCache(DbServiceAccount sa, List<EnvironmentChange> changedEnvironments) {
    cacheSource.updateServiceAccount(sa, PublishAction.UPDATE );

    if (changedEnvironments != null && !changedEnvironments.isEmpty()) {
      changedEnvironments.forEach(e -> cacheSource.updateEnvironment(e.environment, PublishAction.UPDATE));
    }
  }

  @Override
  @Transactional
  public Boolean delete(Person deleter, String serviceAccountId) {
    UUID id = ConvertUtils.ifUuid(serviceAccountId);
    if (id != null) {
      DbServiceAccount sa = new QDbServiceAccount().id.eq(id).whenArchived.isNull().findOne();
      if (sa != null) {
        archiveStrategy.archiveServiceAccount(sa);
        return Boolean.TRUE;
      }
    }

    return Boolean.FALSE;
  }
}
