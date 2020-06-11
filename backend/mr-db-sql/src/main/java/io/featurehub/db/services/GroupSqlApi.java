package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.DuplicateKeyException;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbAcl;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbGroup;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.query.QDbGroup;
import io.featurehub.db.model.query.QDbOrganization;
import io.featurehub.db.model.query.QDbPerson;
import io.featurehub.db.model.query.QDbPortfolio;
import io.featurehub.mr.model.ApplicationGroupRole;
import io.featurehub.mr.model.ApplicationRoleType;
import io.featurehub.mr.model.EnvironmentGroupRole;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class GroupSqlApi implements io.featurehub.db.api.GroupApi {
  private static final Logger log = LoggerFactory.getLogger(GroupSqlApi.class);
  private final Database database;
  private final ConvertUtils convertUtils;
  private final ArchiveStrategy archiveStrategy;

  @Inject
  public GroupSqlApi(Database database, ConvertUtils convertUtils, ArchiveStrategy archiveStrategy) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.archiveStrategy = archiveStrategy;
  }

  @Override
  public boolean isPersonMemberOfPortfolioGroup(String portfolioId, String personId) {
    UUID portId = ConvertUtils.ifUuid(portfolioId);
    UUID persId = ConvertUtils.ifUuid(personId);

    return new QDbGroup().owningPortfolio.id.eq(portId).peopleInGroup.id.eq(persId).findCount() > 0;
  }

  @Override
  public Group createOrgAdminGroup(String orgId, String groupName, Person whoCreated) {
    final Group group = ConvertUtils.uuid(orgId).map(orgUuid -> {
      DbOrganization org = new QDbOrganization().id.eq(orgUuid).findOne();

      if (org == null || new QDbGroup().whenArchived.isNull().owningOrganization.id.eq(orgUuid).findCount() > 0) {
        return null; // already exists or org doesn't exist
      }

      DbGroup.Builder builder = new DbGroup.Builder()
        .name(groupName)
        .adminGroup(true)
        .owningOrganization(org);

      ConvertUtils.uuid(whoCreated.getId().getId()).flatMap(pId -> new QDbPerson().id.eq(pId).findOneOrEmpty()).ifPresent(builder::whoCreated);

      DbGroup dbGroup = builder.build();

      saveGroup(dbGroup);
      return convertUtils.toGroup(dbGroup, Opts.empty());
    }).orElse(null);
    return group;
  }

  @Override
  public Group createPortfolioGroup(String portfolioId, Group group, Person whoCreated) throws DuplicateGroupException {
    DbPortfolio portfolio = convertUtils.uuidPortfolio(portfolioId);

    if (portfolio != null) {
      boolean isAdmin = Boolean.TRUE.equals(group.getAdmin());

      // ensure there isn't already one
      if (!isAdmin || new QDbGroup().whenArchived.isNull().owningPortfolio.eq(portfolio).adminGroup.isTrue().findCount() == 0) {

        final DbPerson personCreatedId = convertUtils.uuidPerson(whoCreated);
          Set<DbAcl> acls = new HashSet<>();

          if (group.getApplicationRoles() != null) {
            group.getApplicationRoles().forEach(appRole -> {
              if (appRole.getApplicationId() != null && appRole.getRoles() != null) {
                DbApplication app = convertUtils.uuidApplication(appRole.getApplicationId());
                if (app != null && app.getPortfolio().getId().equals(portfolio.getId())) {
                  acls.add(new DbAcl.Builder().application(app).roles(appRolesToString(appRole.getRoles())).build());
                }
              }
            });
          }

          // no environment roles as yet

          DbGroup dbGroup = new DbGroup.Builder()
            .owningPortfolio(portfolio)
            .adminGroup(isAdmin)
            .name(group.getName())
            .whoCreated(personCreatedId)
            .groupRolesAcl(acls)
            .build();


          try {
            saveGroup(dbGroup);
          } catch (DuplicateKeyException dke) {
            throw new DuplicateGroupException();
          }

          return convertUtils.toGroup(dbGroup, Opts.empty());
      }

      return null;
    }

    return null;
  }

  public static String appRolesToString(List<ApplicationRoleType> roles) {
    return roles.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
  }

  @Override
  public Group addPersonToGroup(String groupId, String personId, Opts opts) {
    return ConvertUtils.uuid(groupId).map(gid -> {
      DbGroup dbGroup = new QDbGroup().id.eq(gid).whenArchived.isNull().findOne(); // no adding people to archived groups

      if (dbGroup != null) {
        return ConvertUtils.uuid(personId).map(pId ->
          new QDbPerson().id.eq(pId).whenArchived.isNull().findOneOrEmpty().map(person -> {
            // make sure we can't find them
            QDbGroup groupFinder = new QDbGroup().id.eq(gid).peopleInGroup.id.eq(pId);

            if (opts.contains(FillOpts.Members)) {
              groupFinder.peopleInGroup.fetch(); // ensure we prefetch the users in the group
            }

            final DbGroup one = groupFinder.findOne();
            if (one == null) {
              // ebean ensures this is never null
              dbGroup.getPeopleInGroup().add(person);

              saveGroup(dbGroup);

              return convertUtils.toGroup(dbGroup, opts);
            } else { // they are already in the group
              return convertUtils.toGroup(one, opts);
            }
          })
          .orElse(null))
          .orElse(null);
      }

      return null;
    }).orElse(null);
  }

  @Override
  public Group getGroup(String gid, Opts opts) {
    return ConvertUtils.uuid(gid).map(groupId ->
    {
      QDbGroup eq = new QDbGroup().id.eq(groupId);
      if (!opts.contains(FillOpts.Archived)) {
        eq = eq.whenArchived.isNull();
      }
      return convertUtils.toGroup(eq.findOne(), opts);
    })
      .orElse(null);
  }

  @Override
  public Group findPortfolioAdminGroup(String portfolioId, Opts opts) {
    return ConvertUtils.uuid(portfolioId).map(pId -> {
      DbPortfolio portfolio = new QDbPortfolio().id.eq(pId).findOne();
      if (portfolio == null) {
        return null;
      }

      QDbGroup groupFinder = new QDbGroup().owningPortfolio.eq(portfolio);
      groupFinder = groupFinder.adminGroup.is(true);
      if (opts.contains(FillOpts.Members)) {
        groupFinder.peopleInGroup.fetch(); // ensure we prefetch the users in the group
      }

      return convertUtils.toGroup(groupFinder.findOne(), opts);
    }).orElse(null);
  }

  @Override
  public Group findOrganizationAdminGroup(String orgId, Opts opts) {
    UUID org = ConvertUtils.ifUuid(orgId);

    if (org == null) {
      return null;
    }

    // there is only 1
    return convertUtils.toGroup(new QDbGroup().whenArchived.isNull().owningOrganization.id.eq(org).adminGroup.isTrue().findOne(), opts);
  }

  @Override
  public List<Group> groupsWherePersonIsAnAdminMember(String personId) {
    return ConvertUtils.uuid(personId)
      .map(pId -> new QDbGroup().whenArchived.isNull().peopleInGroup.id.eq(pId).and().adminGroup.isTrue().endAnd().findList()
        .stream().map(g -> convertUtils.toGroup(g, Opts.empty())).collect(Collectors.toList())).orElse(null);
  }

  @Override
  @Transactional
  public void deleteGroup(String gid) {
    ConvertUtils.uuid(gid).flatMap(groupId -> new QDbGroup().id.eq(groupId).findOneOrEmpty()).ifPresent(archiveStrategy::archiveGroup);
  }

  @Override
  public Group deletePersonFromGroup(String groupId, String personId, Opts opts) {
    return ConvertUtils.uuid(groupId).map(gId -> {
      return ConvertUtils.uuid(personId).map(pId -> {
        // cannot change archived group
        DbGroup group = new QDbGroup().id.eq(gId).whenArchived.isNull().peopleInGroup.id.eq(pId).findOne();

        if (group != null) {
          return group.getPeopleInGroup().stream().filter(p -> p.getId().equals(pId)).findFirst().map(person -> {
            group.getPeopleInGroup().remove(person);

            saveGroup(group);

            return convertUtils.toGroup(group, opts);
          })
            .orElse(null);
        }

        return null;
      }).orElse(null);
    }).orElse(null);
  }

  @Transactional
  private void saveGroup(DbGroup group) {
    database.save(group);
  }

  @Override
  public Group updateGroup(String gid, Group gp, boolean updateMembers, boolean updateApplicationGroupRoles, boolean updateEnvironmentGroupRoles, Opts opts) throws OptimisticLockingException, DuplicateGroupException {
    DbGroup group = convertUtils.uuidGroup(gid, opts);

    if (group != null && group.getWhenArchived() == null) {
      if (gp.getVersion() == null || group.getVersion() != gp.getVersion()) {
        throw new OptimisticLockingException();
      }

      if (gp.getName() != null) {
        group.setName(gp.getName());
      }

      if (gp.getMembers() != null && updateMembers) {
        updateMembersOfGroup(gp, group);
      }

      if (gp.getEnvironmentRoles() != null && updateEnvironmentGroupRoles) {
        updateEnvironmentMembersOfGroup(gp.getEnvironmentRoles(), group);
      }

      if (gp.getApplicationRoles() != null && updateApplicationGroupRoles) {
        updateApplicationMembersOfGroup(gp.getApplicationRoles(), group);
      }

      try {
        updateGroup(group);
      } catch (DuplicateKeyException dke) {
        throw new DuplicateGroupException();
      }

      return convertUtils.toGroup(group, opts);
    }

    return null;
  }

  @Transactional
  private void updateGroup(DbGroup group) {
    database.update(group);
  }

  private void updateApplicationMembersOfGroup(List<ApplicationGroupRole> updatedApplicationRoles, DbGroup group) {
    Map<UUID, ApplicationGroupRole> desiredApplications = new HashMap<>();
    Set<String> addedApplications = new HashSet<>();

    updatedApplicationRoles.forEach(role -> {
      ConvertUtils.uuid(role.getApplicationId()).ifPresent(uuid -> desiredApplications.put(uuid, role));
      addedApplications.add(role.getApplicationId()); // ensure uniqueness
    });

    List<DbAcl> removedAcls = new ArrayList<>();

    group.getGroupRolesAcl().forEach( acl -> {
      // leave the application acl's alone
      if (acl.getApplication() != null) {
        ApplicationGroupRole egr = desiredApplications.get(acl.getApplication().getId());
        if (egr == null) { // we have it but we don't want it
//          log.info("removing acl {}", acl);
          removedAcls.add(acl);
        } else {
          // don't add this one, we already have it
          addedApplications.remove(egr.getApplicationId());
          // change the roles if necessary
          resetApplicationAcl(acl, egr);
        }
      }
    });

    // delete ones that are no longer valid
    group.getGroupRolesAcl().removeAll(removedAcls);

    // add ones that we want
    for (String ae : addedApplications) {
      DbApplication app = convertUtils.uuidApplication(ae);
      if (app != null && app.getPortfolio().getId().equals(group.getOwningPortfolio().getId())) {
        DbAcl acl = new DbAcl.Builder()
          .application(app)
          .group(group)
          .build();

        resetApplicationAcl(acl, desiredApplications.get(UUID.fromString(ae)));

        group.getGroupRolesAcl().add(acl);
      }
    }
  }


  private void resetApplicationAcl(DbAcl acl, ApplicationGroupRole egr) {
    if (egr.getRoles() != null) {
      final String newRoles = egr.getRoles().stream().map(Enum::name).sorted().collect(Collectors.joining(","));

      if (acl.getRoles() == null || !newRoles.equals(acl.getRoles())) {
        acl.setRoles(newRoles);
      }
    }
  }


  private void updateEnvironmentMembersOfGroup(List<EnvironmentGroupRole> environmentRoles, DbGroup group) {
    Map<UUID, EnvironmentGroupRole> desiredEnvironments = new HashMap<>();
    Set<String> addedEnvironments = new HashSet<>();

    environmentRoles.forEach(role -> {
      ConvertUtils.uuid(role.getEnvironmentId()).ifPresent(uuid -> desiredEnvironments.put(uuid, role));
      addedEnvironments.add(role.getEnvironmentId()); // ensure uniqueness
    });

    List<DbAcl> removedAcls = new ArrayList<>();

    group.getGroupRolesAcl().forEach( acl -> {
      // leave the application acl's alone
      if (acl.getEnvironment() != null) {
        EnvironmentGroupRole egr = desiredEnvironments.get(acl.getEnvironment().getId());
        if (egr == null) { // we have it but we don't want it
          removedAcls.add(acl);
        } else {
          // don't add this one, we already have it
          addedEnvironments.remove(egr.getEnvironmentId());
          // change the roles if necessary
          resetEnvironmentAcl(acl, egr);
        }
      }
    });

    // delete ones that are no longer valid
    group.getGroupRolesAcl().removeAll(removedAcls);

    // add ones that we want
    for (String ae : addedEnvironments) {
      DbEnvironment env = convertUtils.uuidEnvironment(ae);
      if (env != null && env.getParentApplication().getPortfolio().getId().equals(group.getOwningPortfolio().getId())) {
        DbAcl acl = new DbAcl.Builder()
          .environment(env)
          .group(group)
          .build();

        resetEnvironmentAcl(acl, desiredEnvironments.get(UUID.fromString(ae)));

        group.getGroupRolesAcl().add(acl);
      }
    }
  }

  private void resetEnvironmentAcl(DbAcl acl, EnvironmentGroupRole egr) {
    if (egr.getRoles() != null) {
      final String newRoles = egr.getRoles().stream().map(Enum::name).sorted().collect(Collectors.joining(","));

      if (acl.getRoles() == null || !newRoles.equals(acl.getRoles())) {
        acl.setRoles(newRoles);
      }
    }
  }

  private void updateMembersOfGroup(Group gp, DbGroup group) {
    Map<String, Person> desiredPeople = gp.getMembers().stream()
      .filter(p -> p.getId() != null)
      .collect(Collectors.toMap(p -> p.getId().getId(), Function.identity()));

    List<DbPerson> removedPerson = new ArrayList<>();
    // ensure no duplicates get through
    Set<String> addedPeople = gp.getMembers().stream()
      .filter(p -> p != null && p.getId() != null)
      .map(p -> p.getId().getId()).collect(Collectors.toSet());

    group.getPeopleInGroup().forEach(person -> {
      Person p = desiredPeople.get(person.getId().toString());
      if (p == null) { // delete them
        removedPerson.add(person);
      } else {
        addedPeople.remove(p.getId().getId()); // they are already there, remove them from list to add
      }
    });
    group.getPeopleInGroup().removeAll(removedPerson);
    addedPeople.forEach(p -> {
      DbPerson person = convertUtils.uuidPerson(p);
      if (person != null) {
        group.getPeopleInGroup().add(person);
      }
    });
  }

  @Override
  public List<Group> findGroups(String portfolioId, String filter, SortOrder ordering, Opts opts) {
    return ConvertUtils.uuid(portfolioId).map(pId -> {
        QDbGroup gFinder = new QDbGroup().owningPortfolio.id.eq(pId);

        if (filter != null && filter.trim().length() > 0) {
          gFinder = gFinder.name.ilike("%" + filter.trim() + "%");
        }

      if (ordering != null) {
        if (ordering == SortOrder.ASC) {
          gFinder = gFinder.order().name.asc();
        } else if (ordering == SortOrder.DESC) {
          gFinder = gFinder.order().name.desc();
        }
      }

      if (!opts.contains(FillOpts.Archived)) {
        gFinder = gFinder.whenArchived.isNull();
      }

      return gFinder.findList().stream().map(g -> convertUtils.toGroup(g, opts)).collect(Collectors.toList());
    }).orElse(null);
  }

  @Override
  public void updateAdminGroupForPortfolio(String portfolioId, String name) {
    ConvertUtils.uuid(portfolioId).flatMap(pId -> new QDbGroup().whenArchived.isNull().owningPortfolio.id.eq(pId).and().adminGroup.isTrue().endAnd().findOneOrEmpty()).ifPresent(group -> {
      group.setName(name);
      saveGroup(group);
    });
  }
}
