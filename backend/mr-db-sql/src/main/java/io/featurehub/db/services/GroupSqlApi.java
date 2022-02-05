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
import io.featurehub.db.model.query.QDbAcl;
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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public boolean isPersonMemberOfPortfolioGroup(UUID portfolioId, UUID personId) {
    Conversions.nonNullPortfolioId(portfolioId);
    Conversions.nonNullPersonId(personId);

    return new QDbGroup().owningPortfolio.id.eq(portfolioId).peopleInGroup.id.eq(personId).exists();
  }

  private boolean isPersonMemberOfPortfolioAdminGroup(DbPortfolio portfolio, UUID personId) {
    Conversions.nonNullPersonId(personId);

    if (portfolio == null) {
      return false;
    }

    return new QDbGroup()
        .owningPortfolio
        .eq(portfolio)
        .adminGroup
        .isTrue()
        .peopleInGroup
        .id
        .eq(personId)
        .exists();
  }

  @Override
  public Group getSuperuserGroup(UUID orgId, Person personAsking) {
    Conversions.nonNullOrganisationId(orgId);
    Conversions.nonNullPerson(personAsking);

    DbPerson person = convertUtils.byPerson(personAsking);

    if (person == null) {
      return null;
    }

    final DbGroup g =
        new QDbGroup()
            .owningOrganization
            .id
            .eq(orgId)
            .adminGroup
            .isTrue()
            .owningPortfolio
            .isNull()
            .peopleInGroup
            .fetch()
            .findOne();

    if (g != null) { // make sure you are a user in at least one group otherwise you can't see this
      // group
      return convertUtils.toGroup(g, Opts.opts(FillOpts.Members));
    }

    return null;
  }

  @Override
  public List<Group> groupsPersonOrgAdminOf(UUID pId) {
    Conversions.nonNullPersonId(pId);

    return new QDbGroup()
            .whenArchived
            .isNull()
            .owningPortfolio
            .isNull()
            .adminGroup
            .eq(true)
            .peopleInGroup
            .id
            .eq(pId)
            .findList()
            .stream()
            .map(g -> convertUtils.toGroup(g, Opts.empty()))
            .collect(Collectors.toList());
  }

  @Override
  public List<Organization> orgsUserIn(UUID personI) {
    Conversions.nonNullPersonId(personI);

    return new QDbOrganization()
            .or()
            .and()
            .group
            .whenArchived
            .isNull()
            .group
            .peopleInGroup
            .id
            .eq(personI)
            .endAnd()
            .and()
            .portfolios
            .groups
            .whenArchived
            .isNull()
            .portfolios
            .groups
            .peopleInGroup
            .id
            .eq(personI)
            .endAnd()
            .endOr()
            .findList()
            .stream()
            .map(o -> convertUtils.toOrganization(o, Opts.empty()))
            .collect(Collectors.toList());
  }

  @Override
  public Group createOrgAdminGroup(UUID orgUuid, String groupName, Person whoCreated) {
    Conversions.nonNullOrganisationId(orgUuid);

    DbOrganization org = new QDbOrganization().id.eq(orgUuid).findOne();

    if (org == null
        || new QDbGroup()
            .whenArchived
            .isNull()
            .owningPortfolio
            .isNull()
            .owningOrganization
            .id
            .eq(orgUuid)
            .exists()) {
      return null; // already exists or org doesn't exist
    }

    DbGroup.Builder builder =
        new DbGroup.Builder()
            .name(groupName)
            .adminGroup(true)
            .whoCreated(convertUtils.byPerson(whoCreated))
            .owningOrganization(org);

    DbGroup dbGroup = builder.build();

    saveGroup(dbGroup);
    return convertUtils.toGroup(dbGroup, Opts.empty());
  }

  @Override
  public Group createPortfolioGroup(UUID portfolioId, Group group, Person whoCreated)
      throws DuplicateGroupException {
    Conversions.nonNullPortfolioId(portfolioId);

    DbPortfolio portfolio = convertUtils.byPortfolio(portfolioId);

    if (portfolio != null) {
      boolean isAdmin = Boolean.TRUE.equals(group.getAdmin());

      // ensure there isn't already one
      if (!isAdmin
          || !new QDbGroup()
              .whenArchived
              .isNull()
              .owningPortfolio
              .eq(portfolio)
              .adminGroup
              .isTrue()
              .exists()) {

        final DbPerson personCreatedId = convertUtils.byPerson(whoCreated);
        Set<DbAcl> acls = new HashSet<>();

        if (group.getApplicationRoles() != null) {
          group
              .getApplicationRoles()
              .forEach(
                  appRole -> {
                    if (appRole.getApplicationId() != null && appRole.getRoles() != null) {
                      DbApplication app = convertUtils.byApplication(appRole.getApplicationId());
                      if (app != null && app.getPortfolio().getId().equals(portfolio.getId())) {
                        acls.add(
                            new DbAcl.Builder()
                                .application(app)
                                .roles(appRolesToString(appRole.getRoles()))
                                .build());
                      }
                    }
                  });
        }

        // no environment roles as yet

        DbGroup dbGroup =
            new DbGroup.Builder()
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

      log.error(
          "Attempted to create a new admin group for portfolio {} and it already exists.",
          portfolio.getName());

      return null;
    } else {
      log.error(
          "Attempted to create portfolio group for portfolio {} and portfolio doesn't exist.",
          portfolioId);
    }

    return null;
  }

  private DbGroup superuserGroup(DbOrganization org) {
    return new QDbGroup()
        .whenArchived
        .isNull()
        .owningOrganization
        .eq(org)
        .owningPortfolio
        .isNull()
        .adminGroup
        .isTrue()
        .findOne();
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
    superuserGroupMembers(dbGroup.getOwningPortfolio().getOrganization())
        .forEach(
            (p) -> dbGroup.getPeopleInGroup().add(p));

    database.save(dbGroup);
  }

  public static String appRolesToString(List<ApplicationRoleType> roles) {
    return roles.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
  }

  @Override
  public Group addPersonToGroup(UUID groupId, UUID personId, Opts opts) {
    Conversions.nonNullPersonId(personId);
    Conversions.nonNullGroupId(groupId);

    DbGroup dbGroup =
        new QDbGroup()
            .id
            .eq(groupId)
            .whenArchived
            .isNull()
            .findOne(); // no adding people to archived groups

    if (dbGroup != null) {
      DbPerson person = new QDbPerson().id.eq(personId).whenArchived.isNull().findOne();

      if (person != null) {
        QDbGroup groupFinder = new QDbGroup().id.eq(groupId).peopleInGroup.id.eq(personId);

        if (opts.contains(FillOpts.Members)) {
          groupFinder.peopleInGroup.fetch(); // ensure we prefetch the users in the group
        }

        final DbGroup one = groupFinder.findOne();
        if (one == null) {
          // ebean ensures this is never null
          dbGroup.getPeopleInGroup().add(person);

          saveGroup(dbGroup);

          // they actually got added from the superusers group, so
          // lets update the portfolios
          if (dbGroup.isAdminGroup() && dbGroup.getOwningPortfolio() == null) {
            SuperuserChanges sc = new SuperuserChanges(dbGroup.getOwningOrganization());
            sc.addedSuperusers = Collections.singletonList(person);
            updateSuperusersFromPortfolioGroups(sc);
          }

          return convertUtils.toGroup(dbGroup, opts);
        } else { // they are already in the group
          return convertUtils.toGroup(one, opts);
        }
      }
    }

    return null;
  }

  @Override
  public Group getGroup(UUID gid, Opts opts, Person person) {
    Conversions.nonNullGroupId(gid);
    Conversions.nonNullPerson(person);

    QDbGroup eq = new QDbGroup().id.eq(gid).peopleInGroup.fetch();

    if (!opts.contains(FillOpts.Archived)) {
      eq = eq.whenArchived.isNull();
    }

    final DbGroup one = eq.findOne();

    if (one != null
        && (new QDbGroup().id.eq(gid).peopleInGroup.whenArchived.isNull().peopleInGroup.id.eq(person.getId().getId()).exists()
            || isSuperuser(one.findOwningOrganisation(), convertUtils.byPerson(person))
            || isPersonMemberOfPortfolioAdminGroup(
                one.getOwningPortfolio(), person.getId().getId()))) {
      return convertUtils.toGroup(one, opts);
    }

    return null;
  }

  @Override
  public Group findPortfolioAdminGroup(UUID portfolioId, Opts opts) {
    Conversions.nonNullPortfolioId(portfolioId);

    DbPortfolio portfolio = new QDbPortfolio().id.eq(portfolioId).findOne();
    if (portfolio == null) {
      return null;
    }

    QDbGroup groupFinder = new QDbGroup().owningPortfolio.eq(portfolio);
    groupFinder = groupFinder.adminGroup.is(true);
    if (opts.contains(FillOpts.Members)) {
      groupFinder.peopleInGroup.fetch(); // ensure we prefetch the users in the group
    }

    return convertUtils.toGroup(groupFinder.findOne(), opts);
  }

  @Override
  public Group findOrganizationAdminGroup(UUID orgId, Opts opts) {
    Conversions.nonNullOrganisationId(orgId);

    // there is only 1
    return convertUtils.toGroup(
        new QDbGroup()
            .whenArchived
            .isNull()
            .owningPortfolio
            .isNull()
            .owningOrganization
            .id
            .eq(orgId)
            .owningOrganization
            .fetch(QDbOrganization.Alias.id)
            .adminGroup
            .isTrue()
            .findOne(),
        opts);
  }

  @Override
  public List<Group> groupsWherePersonIsAnAdminMember(UUID personId) {
    Conversions.nonNullPersonId(personId);

    return new QDbGroup()
            .whenArchived
            .isNull()
            .peopleInGroup
            .id
            .eq(personId)
            .and()
            .adminGroup
            .isTrue()
            .endAnd()
            .findList()
            .stream()
            .map(g -> convertUtils.toGroup(g, Opts.empty()))
            .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void deleteGroup(UUID gid) {
    Conversions.nonNullGroupId(gid);

    DbGroup group = new QDbGroup().id.eq(gid).findOne();

    if (group != null) {
      archiveStrategy.archiveGroup(group);
    }
  }

  @Override
  public Group deletePersonFromGroup(UUID groupId, UUID personId, Opts opts) {
    Conversions.nonNullGroupId(groupId);
    Conversions.nonNullPersonId(personId);

    DbPerson person = convertUtils.byPerson(personId);

    if (person != null) {
      DbGroup group =
          new QDbGroup().id.eq(groupId).whenArchived.isNull().peopleInGroup.eq(person).findOne();
      // if it is an admin portfolio group and they are a superuser, you can't remove them
      if (group == null
          || (group.isAdminGroup()
              && group.getOwningPortfolio() != null
              && isSuperuser(group.getOwningPortfolio().getOrganization(), person))) {
        return null;
      }

      group.getPeopleInGroup().remove(person);

      saveGroup(group);

      // they actually got removed from the superusers group, so lets update the portfolios
      if (group.isAdminGroup() && group.getOwningPortfolio() == null) {
        SuperuserChanges sc = new SuperuserChanges(group.getOwningOrganization());
        sc.removedSuperusers = Collections.singletonList(person);
        updateSuperusersFromPortfolioGroups(sc);
      }

      return convertUtils.toGroup(group, opts);
    }

    return null;
  }

  @Transactional
  private void saveGroup(DbGroup group) {
    database.save(group);
  }

  public static class SuperuserChanges {
    public DbOrganization organization;
    public List<DbPerson> removedSuperusers = new ArrayList<>();
    public List<DbPerson> addedSuperusers = new ArrayList<>();

    public SuperuserChanges(DbOrganization organization) {
      this.organization = organization;
    }
  }

  @Override
  public Group updateGroup(
      UUID gid,
      Group gp,
      boolean updateMembers,
      boolean updateApplicationGroupRoles,
      boolean updateEnvironmentGroupRoles,
      Opts opts)
      throws OptimisticLockingException, DuplicateGroupException, DuplicateUsersException {
    Conversions.nonNullGroupId(gid);

    DbGroup group = convertUtils.byGroup(gid, opts);

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

      AclUpdates aclUpdates = null;
      if (gp.getEnvironmentRoles() != null && updateEnvironmentGroupRoles) {
        aclUpdates = updateEnvironmentMembersOfGroup(gp.getEnvironmentRoles(), group);
      }

      if (gp.getApplicationRoles() != null && updateApplicationGroupRoles) {
        updateApplicationMembersOfGroup(gp.getApplicationRoles(), group);
      }

      try {
        updateGroup(group, aclUpdates);
      } catch (DuplicateKeyException dke) {
        throw new DuplicateGroupException();
      }

      if (superuserChanges != null) {
        updateSuperusersFromPortfolioGroups(superuserChanges);
      }

      return convertUtils.toGroup(group, opts);
    }

    return null;
  }

  // now we have to walk all the way down and remove these people from all admin portfolio groups
  @Transactional
  protected void updateSuperusersFromPortfolioGroups(
      @NotNull SuperuserChanges superuserChanges) {
    for (DbGroup pGroups :
        new QDbGroup()
            .adminGroup
            .isTrue()
            .owningPortfolio
            .isNotNull()
            .owningPortfolio
            .organization
            .eq(superuserChanges.organization)
            .findList()) {

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
  private void updateGroup(DbGroup group, AclUpdates aclUpdates) {
    database.update(group);

    if (aclUpdates != null) {
      if (!aclUpdates.updates.isEmpty()) {
        database.updateAll(aclUpdates.updates);
      }

      if (!aclUpdates.deletes.isEmpty()) {
        database.deleteAll(aclUpdates.deletes);
      }

      if (!aclUpdates.creates.isEmpty()) {
        database.saveAll(aclUpdates.creates);
      }
    }
  }

  private void updateApplicationMembersOfGroup(
      List<ApplicationGroupRole> updatedApplicationRoles, DbGroup group) {
    Map<UUID, ApplicationGroupRole> desiredApplications = new HashMap<>();
    Set<UUID> addedApplications = new HashSet<>();

    updatedApplicationRoles.stream().filter(r -> r.getApplicationId() != null).forEach(
        role -> {
          desiredApplications.put(role.getApplicationId(), role);
          addedApplications.add(role.getApplicationId()); // ensure uniqueness
        });

    List<DbAcl> removedAcls = new ArrayList<>();

    group
        .getGroupRolesAcl()
        .forEach(
            acl -> {
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
    for (UUID ae : addedApplications) {
      DbApplication app = convertUtils.byApplication(ae);
      if (app != null && app.getPortfolio().getId().equals(group.getOwningPortfolio().getId())) {
        DbAcl acl = new DbAcl.Builder().application(app).group(group).build();

        resetApplicationAcl(acl, desiredApplications.get(ae));

        group.getGroupRolesAcl().add(acl);
      }
    }
  }

  private void resetApplicationAcl(DbAcl acl, ApplicationGroupRole egr) {
    if (egr.getRoles() != null) {
      final String newRoles =
          egr.getRoles().stream().map(Enum::name).sorted().collect(Collectors.joining(","));

      if (acl.getRoles() == null || !newRoles.equals(acl.getRoles())) {
        acl.setRoles(newRoles);
      }
    }
  }

  // we do it as a set of acl updates as a group can get a lot of permissions
  // across a lot of different environments, so its terribly inefficient to get
  // all of the environment acls for a group, best to get just the ones we are
  // updating
  private static class AclUpdates {
    List<DbAcl> updates = new ArrayList<>();
    List<DbAcl> deletes = new ArrayList<>();
    List<DbAcl> creates = new ArrayList<>();
  }

  private AclUpdates updateEnvironmentMembersOfGroup(
      List<EnvironmentGroupRole> environmentRoles, DbGroup group) {
    Map<UUID, EnvironmentGroupRole> desiredEnvironments = new HashMap<>();
    Set<UUID> addedEnvironments = new HashSet<>();
    AclUpdates aclUpdates = new AclUpdates();

    environmentRoles.stream().filter(r -> r.getEnvironmentId() != null).forEach(
        role -> {
          desiredEnvironments.put(role.getEnvironmentId(), role);
          addedEnvironments.add(role.getEnvironmentId()); // ensure uniqueness
        });

    new QDbAcl()
        .group
        .eq(group)
        .environment
        .id
        .in(desiredEnvironments.keySet())
        .findEach(
            acl -> {
              // leave the application acl's alone
              if (acl.getEnvironment() != null) {
                EnvironmentGroupRole egr = desiredEnvironments.get(acl.getEnvironment().getId());

                // don't add this one, we already have it, we just need to update it
                addedEnvironments.remove(acl.getEnvironment().getId());

                // if we have no roles, we need to remove the ACL
                if (egr.getRoles() == null || egr.getRoles().isEmpty()) {
                  aclUpdates.deletes.add(acl);
                } else {
                  // change the roles if necessary
                  resetEnvironmentAcl(acl, egr);
                  aclUpdates.updates.add(acl);
                }
              }
            });

    // add ones the new ones
    for (UUID ae : addedEnvironments) {
      final EnvironmentGroupRole egr = desiredEnvironments.get(ae);
      if (egr.getRoles() != null && !egr.getRoles().isEmpty()) {
        DbEnvironment env =
            convertUtils.byEnvironment(
                ae, Opts.opts(FillOpts.ApplicationIds, FillOpts.PortfolioIds));

        if (env != null
            && env.getParentApplication()
                .getPortfolio()
                .getId()
                .equals(group.getOwningPortfolio().getId())) {
          DbAcl acl = new DbAcl.Builder().environment(env).group(group).build();

          resetEnvironmentAcl(acl, egr);

          aclUpdates.creates.add(acl);
        } else {
          log.error(
              "Attempting to add an environment that doesn't exist or doesn't belong to the same portfolio {}",
              ae);
        }
      }
    }

    return aclUpdates;
  }

  private void resetEnvironmentAcl(DbAcl acl, EnvironmentGroupRole egr) {
    final String newRoles =
        egr.getRoles().stream().map(Enum::name).sorted().collect(Collectors.joining(","));

    if (acl.getRoles() == null || !newRoles.equals(acl.getRoles())) {
      acl.setRoles(newRoles);
    }
  }

  private SuperuserChanges updateMembersOfGroup(Group gp, DbGroup group)
      throws DuplicateUsersException {
    Set<UUID> uuids =
        gp.getMembers().stream()
            .filter(p -> p.getId() != null)
            .map(p -> p.getId().getId())
            .collect(Collectors.toSet());

    if (uuids.size() != gp.getMembers().size()) {
      throw new DuplicateUsersException();
    }

    Map<UUID, Person> desiredPeople =
        gp.getMembers().stream()
            .filter(p -> p.getId() != null)
            .collect(Collectors.toMap(p -> p.getId().getId(), Function.identity()));

    // if this is the superuser group, we will have to remove these people from all portfolio groups
    // as well
    List<DbPerson> removedPerson = new ArrayList<>();

    // ensure no duplicates get through
    Set<UUID> addedPeople =
        gp.getMembers().stream()
            .filter(p -> p != null && p.getId() != null)
            .map(p -> p.getId().getId())
            .collect(Collectors.toSet());

    boolean isSuperuserGroup = group.isAdminGroup() && group.getOwningPortfolio() == null;
    List<DbPerson> superusers =
        group.isAdminGroup() && !isSuperuserGroup
            ? superuserGroupMembers(group.getOwningPortfolio().getOrganization())
            : new ArrayList<>();

    group
        .getPeopleInGroup()
        .forEach(
            person -> {
              Person p = desiredPeople.get(person.getId());
              if (p == null) { // delete them
                // can't delete superusers from portfolio group. if this is the superusergroup or
                // isn't an admin group, superusers will be empty
                if (!superusers.contains(person)) {
                  removedPerson.add(person);
                }
              } else {
                addedPeople.remove(
                    p.getId().getId()); // they are already there, remove them from list to add
              }
            });
    group.getPeopleInGroup().removeAll(removedPerson);
    List<DbPerson> actuallyAddedPeople = new ArrayList<>();
    addedPeople.forEach(
        p -> {
          DbPerson person = convertUtils.byPerson(p);
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
  public List<Group> findGroups(UUID portfolioId, String filter, SortOrder ordering, Opts opts) {
    Conversions.nonNullPortfolioId(portfolioId);

    QDbGroup gFinder = new QDbGroup().owningPortfolio.id.eq(portfolioId);

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

    return gFinder.findList().stream()
        .map(g -> convertUtils.toGroup(g, opts))
        .collect(Collectors.toList());
  }

  @Override
  public void updateAdminGroupForPortfolio(UUID portfolioId, String name) {
    new QDbGroup()
        .whenArchived
        .isNull()
        .owningPortfolio
        .id
        .eq(portfolioId)
        .and()
        .adminGroup
        .isTrue()
        .endAnd()
        .findOneOrEmpty()
        .ifPresent(
            group -> {
              group.setName(name);
              saveGroup(group);
            });
  }
}
