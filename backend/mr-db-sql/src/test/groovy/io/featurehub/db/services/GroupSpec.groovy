package io.featurehub.db.services


import io.featurehub.db.FilterOptType
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbGroup
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.ApplicationGroupRole
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.EnvironmentGroupRole
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.SortOrder
import spock.lang.Shared

class GroupSpec extends BaseSpec {
  @Shared PortfolioSqlApi portfolioApi
  @Shared DbPerson user
  @Shared Portfolio commonPortfolio
  @Shared Application commonApplication1
  @Shared Application commonApplication2
  @Shared ApplicationSqlApi applicationSqlApi
  @Shared EnvironmentSqlApi environmentSqlApi
  @Shared Environment env1App1
  @Shared Group portfolioAdminGroup
  PersonSqlApi personApi
  Conversions conversions

  def setupSpec() {
    baseSetupSpec()

    portfolioApi = new PortfolioSqlApi(database, convertUtils, archiveStrategy)
    environmentSqlApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    user = dbSuperPerson

    applicationSqlApi = new ApplicationSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
    commonPortfolio = portfolioApi.createPortfolio(new Portfolio().name("acl common portfolio").organizationId(org.id), Opts.empty(), superPerson)
    commonApplication1 = applicationSqlApi.createApplication(commonPortfolio.id, new Application().name("acl common app").description("acl common app"), superPerson)
    env1App1 = environmentSqlApi.create(new Environment().name("acl common app env1"), commonApplication1, superPerson)
    commonApplication2 = applicationSqlApi.createApplication(commonPortfolio.id, new Application().name("acl common app2").description("acl common app2"), superPerson)

    portfolioAdminGroup = groupSqlApi.createGroup(commonPortfolio.id, new Group().name("admin group").admin(true), superPerson)
  }

  def setup() {
    personApi = new PersonSqlApi(database, convertUtils, archiveStrategy, groupSqlApi)
    conversions = new ConvertUtils()
  }

  def "group ACL filtering by application works as expected"() {
    given: "i have a second application"
      def app2 = applicationSqlApi.createApplication(commonPortfolio.id, new Application().name("acl-test-filter").description("acl test filter"), superPerson)
    and: "i have an environment in the second application"
      def env2 = environmentSqlApi.create(new Environment().name("acl-test-filter-env").description("acl-test-filter-env"), app2, superPerson)
    and: "create a new group"
      def group = groupSqlApi.createGroup(commonPortfolio.id, new Group().name("acl-test-filter-group"), superPerson)
    and: "i create permissions in the group for both environments"
      def groupUpdated = groupSqlApi.updateGroup(group.id, group.environmentRoles([
        new EnvironmentGroupRole().environmentId(env1App1.id).roles([RoleType.UNLOCK]),
        new EnvironmentGroupRole().environmentId(env2.id).roles([RoleType.CHANGE_VALUE])
      ]), null,
        false, false, true, Opts.opts(FillOpts.Acls))
    when: "i ask for the permissions only for application 2"
      def groupApp2 = groupSqlApi.getGroup(group.id, Opts.opts(FillOpts.Acls).add(FilterOptType.Application, app2.id), superPerson)
    then:
      groupUpdated.environmentRoles.size() == 2
      groupApp2.environmentRoles.size() == 1
      groupApp2.environmentRoles[0].roles.containsAll([RoleType.READ, RoleType.CHANGE_VALUE])
  }

  def "i create a group and the user in it, the portfolio admin, and the superuser can get its details"() {
    given: "i create a normal user"
      def bobToken = personApi.create('plain-bob@mailinator.com', "William", superuser)
      def bob = personApi.getByToken(bobToken.token, Opts.empty())
    and: "i create a portfolio admin user"
      def janeToken = personApi.create('plain-jane@mailinator.com', 'Jane', superuser)
      def jane = personApi.getByToken(janeToken.token, Opts.empty())
    and: "i create a new group in the common portfolio"
      Group g = groupSqlApi.createGroup(commonPortfolio.id, new Group().name("plain-bob-group"), superPerson)
    and: "i update it with the basic user"
      groupSqlApi.updateGroup(g.id, g.members([bob]), null, true, false, false, Opts.empty())
    when: "i add jane as a portfolio admin"
      def latestGroup = groupSqlApi.getGroup(portfolioAdminGroup.id, Opts.opts(FillOpts.Members), superPerson)
      groupSqlApi.updateGroup(portfolioAdminGroup.id, portfolioAdminGroup.addMembersItem(jane), null, true, false, false, Opts.empty())
    then: "bob can get the group"
      groupSqlApi.getGroup(g.id, Opts.opts(FillOpts.Members), bob)
    and: "jane can get the group"
      groupSqlApi.getGroup(g.id, Opts.opts(FillOpts.Members), jane)
    and: "irina/superuser can get the group"
      groupSqlApi.getGroup(g.id, Opts.opts(FillOpts.Members), superPerson)
  }

  def "i can create an admin group for a portfolio and can't create another"() {
    given: "i have a portfolio"
      Portfolio p = portfolioApi.createPortfolio(new Portfolio().name("Main App").organizationId(org.id), Opts.empty(), superPerson)
    when: "i create an admin group for it"
      Group g = groupSqlApi.createGroup(p.id, new Group().name("admin-group").admin(true), superPerson)
    and: "i create another admin group for it"
      Group second = groupSqlApi.createGroup(p.id, new Group().name("second").admin(true), superPerson)
    and: "i look for it"
      Group pAdminGroup = groupSqlApi.findPortfolioAdminGroup(p.id, Opts.opts(FillOpts.Members))
    then: "the first admin group exists"
      g.id != null
      g.name != null
      groupSqlApi.getGroup(g.id, Opts.empty(), superPerson).name == g.name
    and: "the second one never got created"
      second == null
    and: "it was able to find the admin group from the portfolio"
      pAdminGroup != null
      pAdminGroup.name == 'admin-group'
  }

  def "i can create more than one non-admin group for a portfolio"() {
    given: "i have a portfolio"
      Portfolio p = portfolioApi.createPortfolio(new Portfolio().name("Main App1").organizationId(org.id), Opts.empty(), superPerson)
    when: "i create a group for it"
      Group g = groupSqlApi.createGroup(p.id, new Group().name("non-admin-group"), superPerson)
    and: "i create another group for it"
      Group second = groupSqlApi.createGroup(p.id, new Group().name("second-non-admin"), superPerson)
    then: "the first group exists"
      g.id != null
      g.name != null
      groupSqlApi.getGroup(g.id, Opts.empty(), superPerson).name == g.name
    and: "the second one exists"
      second != null
      second.id != null
      groupSqlApi.getGroup(second.id, Opts.empty(), superPerson).name == second.name
  }

  def "i can't create a group for a non-existent portfolio"() {
    when: "i create a group for a fantasy portfolio"
      Group g = groupSqlApi.createGroup(UUID.randomUUID(), new Group().name("non-admin-group"), superPerson)
    then:  "no group is created"
      g == null
  }

  def "i can't create a group for a non-existent organization"() {
    when: "i try and create a group for a non existent organization"
      Group g = groupSqlApi.createOrgAdminGroup(UUID.randomUUID(), "whatever", superPerson)
    then:
      g == null
  }

  def "i can't create a group for an organization where a group already exists (1 only)"() {
    when: "try and create another group for admin"
      Group g = groupSqlApi.createOrgAdminGroup(org.id, "whatever", superPerson)
    then:
      g == null
  }

  def "i can only add users to a group that exists"() {
    when:
      Group g = groupSqlApi.addPersonToGroup(UUID.randomUUID(), superuser, Opts.empty())
    then:
      g == null
  }

  static int counter = 5;

  private Group nonAdminGroup() {
    Portfolio p = portfolioApi.createPortfolio(new Portfolio().name("Main App$counter").organizationId(org.id), Opts.empty(), superPerson)
    Group g = groupSqlApi.createGroup(p.id, new Group().name("non-admin-group$counter"), superPerson)
    counter ++
    return g
  }

  def "i can't add non existent people to a group"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    when: "i add a non existent person"
      Group ng = groupSqlApi.addPersonToGroup(g.id, UUID.randomUUID(), Opts.empty())
    then:
      ng == null
  }

  def "i can't create the same portfolio group name twice"() {
    given: "i have a group called ecks"
      groupSqlApi.createGroup(commonPortfolio.id, new Group().name("ecks"), superPerson)
    when: "i try and create another group with the same name"
      groupSqlApi.createGroup(commonPortfolio.id, new Group().name("ecks"), superPerson)
    then: "it throws a DuplicateGroupException"
      thrown(GroupApi.DuplicateGroupException)
  }

  def "i can't update to the same portfolio group name twice"() {
    given: "i have a group called ecks"
      groupSqlApi.createGroup(commonPortfolio.id, new Group().name("update-ecks"), superPerson)
    when: "i try and create another group with the same name"
      def g = groupSqlApi.createGroup(commonPortfolio.id, new Group().name("update-ecks1"), superPerson)
      groupSqlApi.updateGroup(g.id, g.name("update-ecks"), null, true, true, true, Opts.empty())
    then: "it throws a DuplicateGroupException"
      thrown(GroupApi.DuplicateGroupException)
  }


  def "i cannot add the same person to the same group twice"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    and: "i have a person"
      def person = new DbPerson(name: 'bob', email: 'bob-double-group@fred.com')
      database.save(person)
      def personId = person.id
    when: "i add a person to the group"
      Group ng = groupSqlApi.addPersonToGroup(g.id, personId, Opts.empty())
    and: "i add the person to the group again"
      Group sng = groupSqlApi.addPersonToGroup(g.id, personId, Opts.opts(FillOpts.Members))
    and: "i check the org admin groups this generic person is part of"
      def adminGroups = groupSqlApi.groupsPersonOrgAdminOf(personId)
    then:
      ng != null
      sng.members.count({m -> m.id.id == personId}) == 1
      adminGroups.isEmpty()
  }

  def "i can add three people to the same group"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    and: "i have a person"
      List<DbPerson> people = []
      (1..3).each { it ->
        def person = new DbPerson(name: 'bob', email: "bob-same-${it}@fred.com")
        database.save(person)
        people.add(person)
      }
    when: "i add all people to the group"
      Group group
      people.each { DbPerson p ->
        group = groupSqlApi.addPersonToGroup(g.id, p.id, Opts.opts(FillOpts.Members))
      }
    then:
      group != null
      group.members.size() == 3
  }

  def "i cannot find non-existent portfolio's admin group"() {
    when: "i try and find the admin group of a non existent portfolio"
      Group admin = groupSqlApi.findPortfolioAdminGroup(UUID.randomUUID(), Opts.empty())
    then:
      admin == null
  }

//  def "i cannot find a non-existent organization admin group"() {
//    when:
//      Group admin = groupSqlApi.findOrganizationAdminGroup(UUID.randomUUID(), Opts.opts(FillOpts.Members))
//    then:
//      admin == null
//  }

  def "i can find the admin group of the org"() {
    when:
      Group admin = groupSqlApi.findOrganizationAdminGroup(org.getId(), Opts.opts(FillOpts.Members))
    then:
      admin != null
  }

  def "i can create, add people to it, remove them and finally delete the group"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    and: "i have a person"
      def person = new DbPerson(name: 'bob', email: 'bob-create-delete@fred.com')
      database.save(person)
    and: "i am not a member of the portfolio"
      boolean amNotMember = !groupSqlApi.isPersonMemberOfPortfolioGroup(g.portfolioId, person.id)
    when: "i add a person to the group"
      Group addedToGroup = groupSqlApi.addPersonToGroup(g.id, person.id, Opts.opts(FillOpts.Members))
    and: "i am confirmed to be a portfolio member"
      boolean amMember = groupSqlApi.isPersonMemberOfPortfolioGroup(g.portfolioId, person.id)
    and: "i delete the person from the group"
      Group deletedFromGroup = groupSqlApi.deletePersonFromGroup(g.id, person.id, Opts.opts(FillOpts.Members))
    and: "i delete the whole group"
      groupSqlApi.deleteGroup(g.id)
    and: "i try and find the group"
      Group finding = groupSqlApi.getGroup(g.id, Opts.empty(), superPerson)
    and: "am not a portfolio member again"
      boolean amNotMemberAgain = !groupSqlApi.isPersonMemberOfPortfolioGroup(g.portfolioId, person.id)
    then:
      addedToGroup.members.size() == 1
      deletedFromGroup.members.size() == 0
      finding == null
      amNotMember
      amMember
      amNotMemberAgain
  }

  def "i cannot delete someone who isn't in a group"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    and: "i have a person"
      def person = new DbPerson(name: 'bob', email: 'bob-not-in-group@fred.com')
      database.save(person)
    when: "i try and delete them from the group"
      Group deletedFromGroup = groupSqlApi.deletePersonFromGroup(g.id, person.id, Opts.opts(FillOpts.Members))
    then:
      deletedFromGroup == null
  }

  def "i cannot delete a non-existent person from a group (no blowing up)"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    when:
      Group deletedFromGroup = groupSqlApi.deletePersonFromGroup(g.id, UUID.randomUUID(), Opts.opts(FillOpts.Members))
    then:
      deletedFromGroup == null
  }

  def "i rename a group"() {
    given: "i have a group"
      Group g = nonAdminGroup().name("new name")
    when: "i rename it"
      groupSqlApi.updateGroup(g.id, g, null, true, true, true, Opts.empty())
    and: "find it again"
      Group ng = groupSqlApi.getGroup(g.id, Opts.empty(), superPerson)
    then:
      ng.name == "new name"
  }

  def "i add a couple of people, then remove one and then add a new one to the group"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    and: "some people"
      DbPerson p1 = new DbPerson.Builder().name("Sasha").email("sasha@").build()
      DbPerson p2 = new DbPerson.Builder().name("Alena").email("alena@").build()
      DbPerson p3 = new DbPerson.Builder().name("Toya").email("toya@").build()
      database.save(p1)
      database.save(p2)
      database.save(p3)
    when: "i update the group to add Sasha and Alena"
      g.members = [
        new Person().id(new PersonId().id(p1.id)),
        new Person().id(new PersonId().id(p2.id)),
      ]
      def g2 = groupSqlApi.updateGroup(g.id, g, null, true, true, true, new Opts().add(FillOpts.Members))
    and: "I updated the group to remove Alena and add Toya"
      def g2_copy = g2.copy()
      g2_copy.members = [
        new Person().id(new PersonId().id(p1.id)),
        new Person().id(new PersonId().id(p3.id)),
      ]
      groupSqlApi.updateGroup(g.id, g2_copy, null, true, true, true, new Opts().add(FillOpts.Members))
      def g3 = groupSqlApi.getGroup(g.id, new Opts().add(FillOpts.Members), superPerson)
    then:
      g2.members.size() == 2
      g2.members*.name.contains('Alena')
      g2.members*.name.contains('Sasha')
      g3.members.size() == 2
      g3.members*.name.contains('Sasha')
      g3.members*.name.contains('Toya')
  }

  def "i cannot rename a non-existent group"() {
    when: "i rename it"
      Group g = groupSqlApi.updateGroup(UUID.randomUUID(), new Group().name("new name"), null,
        true, true, true, Opts.empty())
    then:
      g == null
  }

  def "i create several groups for a portfolio, and then look for them and they can be found in a search"() {
    given: "i have two portfolios"
      Portfolio p1 = portfolioApi.createPortfolio(new Portfolio().name("Main App0").organizationId(org.id), Opts.empty(), superPerson)
      Portfolio p2 = portfolioApi.createPortfolio(new Portfolio().name("Second App").organizationId(org.id), Opts.empty(), superPerson)
    and: "i have three groups under each with the same names"
      List<Group> p1Groups = []
      List<Group> p2Groups = []
      (1..3).each { it ->
        p1Groups.add groupSqlApi.createGroup(p1.id, new Group().name("group ${it}"), superPerson)
        p2Groups.add groupSqlApi.createGroup(p2.id, new Group().name("group ${it}"), superPerson)
      }
    when: "i search for groups under p1"
      List<Group> groupsP1 = groupSqlApi.findGroups(p1.getId(), null, SortOrder.ASC, Opts.empty())
    and: "just search for 2 in portfolio 1"
      List<Group> groupsP1Just2 = groupSqlApi.findGroups(p1.getId(), '2', SortOrder.ASC, Opts.empty())
    and: "i reverse sort for groups under p2"
      List<Group> groupsP2 = groupSqlApi.findGroups(p2.getId(), null, SortOrder.DESC, Opts.empty())
    and: "just search for 3 in portfolio 2"
      List<Group> groupsP2Just3 = groupSqlApi.findGroups(p2.getId(), '3', SortOrder.DESC, Opts.empty())
    then:
      groupsP1.size() == 3
      groupsP1Just2.size() == 1
      groupsP1*.name == ['group 1', 'group 2', 'group 3']
      groupsP1Just2*.name == ['group 2']
    and:
      groupsP2.size() == 3
      groupsP2Just3.size() == 1
      groupsP2*.name == ['group 3', 'group 2', 'group 1']
      groupsP2Just3*.name == ['group 3']
  }

  def "I create four groups of which two are admin and add a person to one admin and one non admin group and they should have 1 admin group"() {
    given: "I have a portfolio"
      Portfolio pi = portfolioApi.createPortfolio(new Portfolio().name("access test").organizationId(org.id), Opts.empty(), superPerson)
    and: "I create four groups"
      def g1 = groupSqlApi.createGroup(pi.id, new Group().name("g1-access").admin(true), superPerson)
      def g2 = groupSqlApi.createGroup(pi.id, new Group().name("g2-access").admin(true), superPerson)
      def g3 = groupSqlApi.createGroup(pi.id, new Group().name("g3-access"), superPerson)
      def g4 = groupSqlApi.createGroup(pi.id, new Group().name("g4-access"), superPerson)
    and: "i add a person to this group"
      DbPerson user = new DbPerson.Builder().email("bob-test@featurehub.io").name("Rob test").build();
      database.save(user);
      groupSqlApi.addPersonToGroup(g1.id, user.id, Opts.empty())
      groupSqlApi.addPersonToGroup(g3.id, user.id, Opts.empty())
    when: "i get their admin groups"
      List<Group> groups = groupSqlApi.groupsWherePersonIsAnAdminMember(user.id)
    then:
      groups.size() == 1
      groups[0].name == 'g1-access'
  }

  def "I wish to update a group with environment acls"() {
    given: "I have a portfolio"
      Portfolio pi = portfolioApi.createPortfolio(new Portfolio().name("acl test1").organizationId(org.id), Opts.empty(), superPerson)
    and: "I create a group"
      def g1 = groupSqlApi.createGroup(pi.id, new Group().name("g1-access-acl1").admin(false), superPerson)
    and: "an application"
      def portfo = database.find(DbPortfolio, pi.id)
      def app = new DbApplication.Builder().name("g1-name-acl").portfolio(portfo).whoCreated(user).build()
      database.save(app)
    and: "an environment"
      DbEnvironment env = new DbEnvironment.Builder().name("gp-acl-1").parentApplication(app).whoCreated(user).build()
      database.save(env)
    and: "another environment"
      DbEnvironment env2 = new DbEnvironment.Builder().name("gp-acl-2").parentApplication(app).whoCreated(user).build()
      database.save(env2)
    and: "i add a third environment that i wont actually add roles for but ensure it doesnt get added to the group ACLs"
      DbEnvironment env3 = new DbEnvironment.Builder().name("gp-acl-3").parentApplication(app).whoCreated(user).build()
      database.save(env3)
    when: "I update the group to include acls"
      g1.environmentRoles = [new EnvironmentGroupRole().environmentId(env.id).roles([RoleType.CHANGE_VALUE, RoleType.LOCK])]
      def updGroup = groupSqlApi.updateGroup(g1.id, g1, null, true, true, true, new Opts().add(FillOpts.Acls))
    and: "i get the group with acls requested"
      def getUpd = groupSqlApi.getGroup(g1.id, new Opts().add(FillOpts.Acls), superPerson)
    and: "the i update the roles"
      g1.environmentRoles = [
                new EnvironmentGroupRole().environmentId(env.id).roles([RoleType.LOCK, RoleType.READ]),
                new EnvironmentGroupRole().environmentId(env3.id)]
      def updGroup1 = groupSqlApi.updateGroup(g1.id, g1, null, true, true, true, new Opts().add(FillOpts.Acls))
    and: "then i add another environment role and remove the first"
      g1.environmentRoles = [new EnvironmentGroupRole().environmentId(env2.id).roles([RoleType.UNLOCK]), // READ implicit
                             new EnvironmentGroupRole().environmentId(env.id),
                             new EnvironmentGroupRole().environmentId(env3.id) ]
      def updGroup2 = groupSqlApi.updateGroup(g1.id, g1, null, true, true, true, new Opts().add(FillOpts.Acls))
    and: "then i add back in just environment 1 but it should preserve environment 2"
      g1.environmentRoles = [new EnvironmentGroupRole().environmentId(env.id).roles([RoleType.LOCK])] // READ implicit
      def updGroup3 = groupSqlApi.updateGroup(g1.id, g1, null, true, true, true, new Opts().add(FillOpts.Acls))
    and: "i get the env2 with its group roles"
      def fullEnv2 = environmentSqlApi.get(env2.id, Opts.opts(FillOpts.Acls), superPerson)
    then:
      updGroup.environmentRoles.size() == 1
      updGroup.environmentRoles[0].roles.containsAll([RoleType.CHANGE_VALUE, RoleType.LOCK])
      getUpd.environmentRoles.size() == 1
      getUpd.environmentRoles[0].roles.containsAll([RoleType.CHANGE_VALUE, RoleType.LOCK])
      updGroup1.environmentRoles.size() == 1
      updGroup1.environmentRoles[0].roles.containsAll([RoleType.READ, RoleType.LOCK])
      updGroup2.environmentRoles.size() == 1
      updGroup2.environmentRoles[0].roles.containsAll([RoleType.UNLOCK])
      updGroup3.environmentRoles.size() == 2
      updGroup3.environmentRoles.find({it.environmentId == env.id})
      updGroup3.environmentRoles.find({it.environmentId == env2.id})
      fullEnv2.groupRoles.find({it.groupId == g1.id})?.roles?.containsAll([RoleType.UNLOCK, RoleType.READ])
  }

  def "i wish to add an application acl to a group"() {
    given: "i create a group with the application acl"
      def g1 = groupSqlApi.createGroup(commonPortfolio.id,
        new Group().name("app acl group1")
          .applicationRoles([
            new ApplicationGroupRole()
              .roles([ApplicationRoleType.FEATURE_EDIT])
              .applicationId(commonApplication1.id)
          ]), superPerson)
    and: "i find the group including the acls"
      def found = groupSqlApi.getGroup(g1.id, Opts.opts(FillOpts.Acls), superPerson)
    and: "i update the group to include environment acls"
      def updating = found.copy()
      updating.environmentRoles = [
        new EnvironmentGroupRole().roles([RoleType.CHANGE_VALUE]).environmentId(env1App1.id).groupId(g1.id)
      ]
      updating.applicationRoles.add(new ApplicationGroupRole().roles([ApplicationRoleType.FEATURE_EDIT]).applicationId(commonApplication2.id))
      def up1 = groupSqlApi.updateGroup(g1.id, updating, null, true, true, true, Opts.opts(FillOpts.Acls))
    when: "i find the group"
      def found1 = groupSqlApi.getGroup(g1.id, Opts.opts(FillOpts.Acls), superPerson)
    then:
      found.applicationRoles.size() == 1
      found1.environmentRoles.size() == 1
      found1.applicationRoles.size() == 2
  }

  def "i have two superadmins and one can remove the other as a superadmin"() {
    given: "i have another superadmin"
      def iSuperPerson = new DbPerson.Builder().email("irushka@featurehub.io").name("Irina").build()
      database.save(iSuperPerson)
      def adminGroup = groupSqlApi.findOrganizationAdminGroup(org.id, Opts.empty())
      groupSqlApi.addPersonToGroup(adminGroup.id, iSuperPerson.id, Opts.empty())
    and: "i get the person"
      def person = personApi.get(iSuperPerson.id, Opts.opts(FillOpts.Groups))
    when: "i update the person to remove the superuser group"
      person.groups.removeIf(it -> it.admin && !it.portfolioId)
      def updatedPerson = personApi.update(person.id.id, person, Opts.opts(FillOpts.Groups), superPerson.id.id)
    then:
      updatedPerson.groups.find(g -> g.admin && g.portfolioId == null) == null
//      1 * internalGroupSqlApi.adminGroupsPersonBelongsTo(person.id.id) >> [internalGroupSqlApi.superuserGroup(conversions.dbOrganization())]
  }
}
