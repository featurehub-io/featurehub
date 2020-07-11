package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.DuplicateKeyException;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.DuplicateUsersException;
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
import io.featurehub.mr.model.Organization;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
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
  private final Conversions convertUtils;
  private final ArchiveStrategy archiveStrategy;

  @Inject
  public GroupSqlApi(Database database, Conversions convertUtils, ArchiveStrategy archiveStrategy) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.archiveStrategy = archiveStrategy;
  }

  @Override
  public boolean isPersonMemberOfPortfolioGroup(String portfolioId, String personId) {
    UUID portId = Conversions.ifUuid(portfolioId);
    UUID persId = Conversions.ifUuid(personId);

    return new QDbGroup().owningPortfolio.id.eq(portId).peopleInGroup.id.eq(persId).findCount() > 0;
  }

  @Override
  public Group getSuperuserGroup(String id, Person personAsking) {
    UUID orgId = Conversions.ifUuid(id);
    DbPerson person = convertUtils.uuidPerson(personAsking);
    if (orgId == null || person == null) {
      return null;
    }
    if (new QDbGroup().owningOrganization.id.eq(orgId).whenArchived.isNull().peopleInGroup.eq(person).findCount() > 0 ||
      new QDbGroup().owningPortfolio.organization.id.eq(orgId).whenArchived.isNull().peopleInGroup.eq(person).findCount() > 0) {
      final DbGroup g = new QDbGroup().owningOrganization.id.eq(orgId).peopleInGroup.fetch().findOne();
      if (g != null) { // make sure you are a user in at least one group otherwise you can't see this group
        return convertUtils.toGroup(g, Opts.opts(FillOpts.Members));
      }
    }

    return null;
  }

  @Override
  public List<Group> groupsPersonOrgAdminOf(String personId) {
    UUID pId = Conversions.ifUuid(personId);

    if (personId != null) {
      return new QDbGroup()
        .whenArchived.isNull()
        .owningPortfolio.isNull()
        .adminGroup.eq(true)
        .peopleInGroup.id.eq(pId)
        .findList().stream()
        .map(g -> convertUtils.toGroup(g, Opts.empty()))
        .collect(Collectors.toList());
    }

    return new ArrayList<>();
  }

  @Override
  public List<Organization> orgsUserIn(String personId) {
    UUID pId = Conversions.ifUuid(personId);

    if (pId != null) {
      return new QDbOrganization()
        .or()
        .and()
        .group.whenArchived.isNull()
        .group.peopleInGroup.id.eq(pId)
        .endAnd()
        .and()
        .portfolios.groups.whenArchived.isNull()
        .portfolios.groups.peopleInGroup.id.eq(pId)
        .endAnd()
        .endOr()
        .findList().stream()
        .map(o -> convertUtils.toOrganization(o, Opts.empty()))
        .collect(Collectors.toList());

    }


    return new ArrayList<>();
  }

  @Override
  public Group createOrgAdminGroup(String orgId, String groupName, Person whoCreated) {
    final Group group = Conversions.uuid(orgId).map(orgUuid -> {
      DbOrganization org = new QDbOrganization().id.eq(orgUuid).findOne();

      if (org == null || new QDbGroup().whenArchived.isNull().owningOrganization.id.eq(orgUuid).findCount() > 0) {
        return null; // already exists or org doesn't exist
      }

      DbGroup.Builder builder = new DbGroup.Builder()
        .name(groupName)
        .adminGroup(true)
        .owningOrganization(org);

      Conversions.uuid(whoCreated.getId().getId()).flatMap(pId -> new QDbPerson().id.eq(pId).findOneOrEmpty()).ifPresent(builder::whoCreated);

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
          .owningOrganization(portfolio.getOrganization())
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

        if (dbGroup.isAdminGroup()) {
          copySuperusersToPortfolioGroup(dbGroup);
        }

        return convertUtils.toGroup(dbGroup, Opts.empty());
      }

      log.error("Attempted to create a new admin group for portfolio {} and it already exists.", portfolio.getName());

      return null;
    } else {
      log.error("Attempted to create portfolio group for portfolio {} and portfolio doesn't exist.", portfolioId);
    }

    return null;
  }

  private DbGroup superuserGroup(DbOrganization org) {
    return new QDbGroup()
      .whenArchived.isNull()
      .owningOrganization.eq(org)
      .owningPortfolio.isNull()
      .adminGroup.isTrue().findOne();
  }

  private List<DbPerson> superuserGroupMembers(DbOrganization org) {
    DbGroup superuserGroup = superuserGroup(org);
    return new QDbPerson().whenArchived.isNull().groupsPersonIn.eq(superuserGroup).findList();
  }

  private boolean isSuperuser(DbOrganization org, DbPerson person) {
    if (org == null || person == null) {
      return false;
    }

    DbGroup superuserGroup = superuserGroup(org);
    return new QDbPerson().groupsPersonIn.eq(superuserGroup).id.eq(person.getId()).exists();
  }

  private void copySuperusersToPortfolioGroup(DbGroup dbGroup) {
    superuserGroupMembers(dbGroup.getOwningPortfolio().getOrganization()).forEach((p) -> {
      dbGroup.getPeopleInGroup().add(p);
    });

    database.save(dbGroup);
  }

  public static String appRolesToString(List<ApplicationRoleType> roles) {
    return roles.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
  }

  @Override
  public Group addPersonToGroup(String groupId, String personId, Opts opts) {
    return Conversions.uuid(groupId).map(gid -> {
      DbGroup dbGroup = new QDbGroup().id.eq(gid).whenArchived.isNull().findOne(); // no adding people to archived groups

      if (dbGroup != null) {
        return Conversions.uuid(personId).map(pId ->
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

              // they actually got added from the superusers group, so lets update the portfolios
              if (dbGroup.isAdminGroup() && dbGroup.getOwningPortfolio() == null) {
                SuperuserChanges sc = new SuperuserChanges(dbGroup.getOwningOrganization());
                sc.addedSuperusers = Collections.singletonList(person);
                updateSuperusersFromPortfolioGroups(dbGroup, sc);
              }

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
  public Group getGroup(String gid, Opts opts, Person person) {
    return Conversions.uuid(gid).map(groupId ->
    {
      QDbGroup eq = new QDbGroup().id.eq(groupId).peopleInGroup.fetch();

      if (!opts.contains(FillOpts.Archived)) {
        eq = eq.whenArchived.isNull();
      }

      final DbGroup one = eq.findOne();

      if (one != null && (one.getPeopleInGroup().stream().anyMatch(p -> p.getWhenArchived() == null && p.getId().toString().equals(person.getId().getId())) ||
        isSuperuser(one.findOwningOrganisation(), convertUtils.uuidPerson(person)))) {
        return convertUtils.toGroup(one, opts);
      }

      return null;
    })
      .orElse(null);
  }

  @Override
  public Group findPortfolioAdminGroup(String portfolioId, Opts opts) {
    return Conversions.uuid(portfolioId).map(pId -> {
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
    UUID org = Conversions.ifUuid(orgId);

    if (org == null) {
      return null;
    }

    // there is only 1
    return convertUtils.toGroup(new QDbGroup()
        .whenArchived.isNull()
        .owningPortfolio.isNull()
        .owningOrganization.id.eq(org)
        .owningOrganization.fetch(QDbOrganization.Alias.id)
        .adminGroup.isTrue().findOne(), opts);
  }

  @Override
  public List<Group> groupsWherePersonIsAnAdminMember(String personId) {
    return Conversions.uuid(personId)
      .map(pId -> new QDbGroup().whenArchived.isNull().peopleInGroup.id.eq(pId).and().adminGroup.isTrue().endAnd().findList()
        .stream().map(g -> convertUtils.toGroup(g, Opts.empty())).collect(Collectors.toList())).orElse(null);
  }

  @Override
  @Transactional
  public void deleteGroup(String gid) {
    Conversions.uuid(gid).flatMap(groupId -> new QDbGroup().id.eq(groupId).findOneOrEmpty()).ifPresent(archiveStrategy::archiveGroup);
  }

  @Override
  public Group
  deletePersonFromGroup(String groupId, String personId, Opts opts) {
    DbPerson person = convertUtils.uuidPerson(personId);

    if (person != null) {
      UUID gId = Conversions.ifUuid(groupId);
      if (gId != null) {
        DbGroup group = new QDbGroup().id.eq(gId).whenArchived.isNull().peopleInGroup.eq(person).findOne();
        // if it is an admin portfolio group and they are a superuser, you can't remove them
        if (group == null || (group.isAdminGroup() && group.getOwningPortfolio() != null && isSuperuser(group.getOwningPortfolio().getOrganization(), person))) {
          return null;
        }

        group.getPeopleInGroup().remove(person);

        saveGroup(group);

        // they actually got removed from the superusers group, so lets update the portfolios
        if (group.isAdminGroup() && group.getOwningPortfolio() == null) {
          SuperuserChanges sc = new SuperuserChanges(group.getOwningOrganization());
          sc.removedSuperusers = Collections.singletonList(person);
          updateSuperusersFromPortfolioGroups(group, sc);
        }

        return convertUtils.toGroup(group, opts);
      }
    }

    return null;
  }

  @Transactional
  private void saveGroup(DbGroup group) {
    database.save(group);
  }


  static class SuperuserChanges {
    DbOrganization organization;
    List<DbPerson> removedSuperusers = new ArrayList<>();
    List<DbPerson> addedSuperusers = new ArrayList<>();

    public SuperuserChanges(DbOrganization organization) {
      this.organization = organization;
    }
  }

  @Override
  public Group updateGroup(String gid, Group gp, boolean updateMembers, boolean updateApplicationGroupRoles, boolean updateEnvironmentGroupRoles, Opts opts)
    throws OptimisticLockingException, DuplicateGroupException, DuplicateUsersException {
    DbGroup group = convertUtils.uuidGroup(gid, opts);

    if (group != null && group.getWhenArchived() == null) {
      if (gp.getVersion() == null || group.getVersion() != gp.getVersion()) {
        throw new OptimisticLockingException();
      }

      if (gp.getName() != null) {
        group.setName(gp.getName());
      }


      SuperuserChanges superuserChanges = null;
      if (gp.getMembers() != null && updateMembers) {
        superuserChanges = updateMembersOfGroup(gp, group);
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

      if (superuserChanges != null) {
        updateSuperusersFromPortfolioGroups(group, superuserChanges);
      }

      return convertUtils.toGroup(group, opts);
    }

    return null;
  }

  // now we have to walk all the way down and remove these people from all admin portfolio groups
  @Transactional
  private void updateSuperusersFromPortfolioGroups(DbGroup group, SuperuserChanges superuserChanges) {
    for (DbGroup pGroups : new QDbGroup().adminGroup.isTrue().owningPortfolio.isNotNull().owningPortfolio.organization.eq(superuserChanges.organization).findList()) {

      // remove any superusers
      if (!superuserChanges.removedSuperusers.isEmpty()) {
        pGroups.getPeopleInGroup().removeAll(superuserChanges.removedSuperusers);
      }

      // add superusers but only if they aren't there already
      if (!superuserChanges.addedSuperusers.isEmpty()) {
        for (DbPerson p : superuserChanges.addedSuperusers) {
          if (!pGroups.getPeopleInGroup().contains(p)) {
            pGroups.getPeopleInGroup().add(p);
          }
        }
      }

      database.save(pGroups);
    }
  }

  @Transactional
  private void updateGroup(DbGroup group) {
    database.update(group);
  }

  private void updateApplicationMembersOfGroup(List<ApplicationGroupRole> updatedApplicationRoles, DbGroup group) {
    Map<UUID, ApplicationGroupRole> desiredApplications = new HashMap<>();
    Set<String> addedApplications = new HashSet<>();

    updatedApplicationRoles.forEach(role -> {
      Conversions.uuid(role.getApplicationId()).ifPresent(uuid -> desiredApplications.put(uuid, role));
      addedApplications.add(role.getApplicationId()); // ensure uniqueness
    });

    List<DbAcl> removedAcls = new ArrayList<>();

    group.getGroupRolesAcl().forEach(acl -> {
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
      Conversions.uuid(role.getEnvironmentId()).ifPresent(uuid -> desiredEnvironments.put(uuid, role));
      addedEnvironments.add(role.getEnvironmentId()); // ensure uniqueness
    });

    List<DbAcl> removedAcls = new ArrayList<>();

    group.getGroupRolesAcl().forEach(acl -> {
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
      DbEnvironment env = convertUtils.uuidEnvironment(ae, Opts.opts(FillOpts.ApplicationIds, FillOpts.PortfolioIds));
      if (env != null && env.getParentApplication().getPortfolio().getId().equals(group.getOwningPortfolio().getId())) {
        DbAcl acl = new DbAcl.Builder()
          .environment(env)
          .group(group)
          .build();

        resetEnvironmentAcl(acl, desiredEnvironments.get(UUID.fromString(ae)));

        group.getGroupRolesAcl().add(acl);
      } else {
        log.error("Attempting to add an environment that doesn't exist or doesn't belong to the same portfolio {}", ae);
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

  private SuperuserChanges updateMembersOfGroup(Group gp, DbGroup group) throws DuplicateUsersException {
    Set<String> uuids = gp.getMembers().stream().filter(p -> p.getId() != null).map(p -> p.getId().getId()).collect(Collectors.toSet());

    if (uuids.size() != gp.getMembers().size()) {
      throw new DuplicateUsersException();
    }

    Map<String, Person> desiredPeople = gp.getMembers().stream()
      .filter(p -> p.getId() != null)
      .collect(Collectors.toMap(p -> p.getId().getId(), Function.identity()));

    // if this is the superuser group, we will have to remove these people from all portfolio groups as well
    List<DbPerson> removedPerson = new ArrayList<>();

    // ensure no duplicates get through
    Set<String> addedPeople = gp.getMembers().stream()
      .filter(p -> p != null && p.getId() != null)
      .map(p -> p.getId().getId()).collect(Collectors.toSet());

    boolean isSuperuserGroup = group.isAdminGroup() && group.getOwningOrganization() != null;
    List<DbPerson> superusers = group.isAdminGroup() && !isSuperuserGroup ? superuserGroupMembers(group.getOwningPortfolio().getOrganization()) : new ArrayList<>();

    group.getPeopleInGroup().forEach(person -> {
      Person p = desiredPeople.get(person.getId().toString());
      if (p == null) { // delete them
        // can't delete superusers from portfolio group. if this is the superusergroup or isn't an admin group, superusers will be empty
        if (!superusers.contains(person)) {
          removedPerson.add(person);
        }
      } else {
        addedPeople.remove(p.getId().getId()); // they are already there, remove them from list to add
      }
    });
    group.getPeopleInGroup().removeAll(removedPerson);
    List<DbPerson> actuallyAddedPeople = new ArrayList<>();
    addedPeople.forEach(p -> {
      DbPerson person = convertUtils.uuidPerson(p);
      if (person != null) {
        group.getPeopleInGroup().add(person);
        actuallyAddedPeople.add(person);
      }
    });

    if (isSuperuserGroup) {
      SuperuserChanges sc = new SuperuserChanges(group.getOwningOrganization());
      sc.removedSuperusers = removedPerson;
      sc.addedSuperusers = actuallyAddedPeople;
      return sc;
    }

    return null;
  }

  @Override
  public List<Group> findGroups(String portfolioId, String filter, SortOrder ordering, Opts opts) {
    return Conversions.uuid(portfolioId).map(pId -> {
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
    Conversions.uuid(portfolioId).flatMap(pId -> new QDbGroup().whenArchived.isNull().owningPortfolio.id.eq(pId).and().adminGroup.isTrue().endAnd().findOneOrEmpty()).ifPresent(group -> {
      group.setName(name);
      saveGroup(group);
    });
  }
}
