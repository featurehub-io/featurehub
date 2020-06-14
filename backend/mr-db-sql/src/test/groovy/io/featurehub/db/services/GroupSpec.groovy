package io.featurehub.db.services

import io.ebean.DB
import io.ebean.Database
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbPerson
import io.featurehub.db.publish.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.ApplicationGroupRole
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.EnvironmentGroupRole
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.SortOrder
import spock.lang.Shared
import spock.lang.Specification

class GroupSpec extends Specification {
  @Shared Database database
  @Shared ConvertUtils convertUtils
  @Shared GroupSqlApi groupApi
  @Shared PortfolioSqlApi portfolioApi
  @Shared UUID superuser
  @Shared Organization org
  @Shared Person superPerson
  @Shared DbPerson user
  @Shared Portfolio commonPortfolio
  @Shared Application commonApplication1
  @Shared Application commonApplication2
  @Shared ApplicationSqlApi applicationSqlApi
  @Shared EnvironmentSqlApi environmentSqlApi
  @Shared Environment env1App1

  def setupSpec() {
    System.setProperty("ebean.ddl.generate", "true")
    System.setProperty("ebean.ddl.run", "true")
    database = DB.getDefault()
    convertUtils = new ConvertUtils(database)
    def archiveStrategy = new DbArchiveStrategy(database, convertUtils, Mock(CacheSource))
    groupApi = new GroupSqlApi(database, convertUtils, archiveStrategy)
    portfolioApi = new PortfolioSqlApi(database, convertUtils, archiveStrategy)
    environmentSqlApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    def orgApi = new OrganizationSqlApi(database, convertUtils)
//    org = orgApi.get()
//    if (org == null) {
      org = orgApi.save(new Organization().name("group-org"))
//    }

    println("org is ${org.id} : ${org.name}")

    def q = new QDbPerson(database)
    assert q != null
    user = Finder.findByEmail("irina@featurehub.io")
    if (user == null) {
      user = new DbPerson.Builder().email("irina@featurehub.io").name("Irina").build();
      database.save(user);
    }

    superuser = user.getId()
    superPerson = new Person().id(new PersonId().id(superuser.toString()))

    // create the admin group and put Irina in it
    Group adminGroup = groupApi.createOrgAdminGroup(org.getId(), "super-user-admin-group1", superPerson)
    groupApi.addPersonToGroup(adminGroup.getId(), user.getId().toString(), Opts.empty())

    applicationSqlApi = new ApplicationSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
    commonPortfolio = portfolioApi.createPortfolio(new Portfolio().name("acl common portfolio").organizationId(org.id), Opts.empty(), superPerson)
    commonApplication1 = applicationSqlApi.createApplication(commonPortfolio.id, new Application().name("acl common app").description("acl common app"), superPerson)
    env1App1 = environmentSqlApi.create(new Environment().name("acl common app env1"), commonApplication1, superPerson)
    commonApplication2 = applicationSqlApi.createApplication(commonPortfolio.id, new Application().name("acl common app2").description("acl common app2"), superPerson)
  }

  def "i can create an admin group for a portfolio and can't create another"() {
    given: "i have a portfolio"
      Portfolio p = portfolioApi.createPortfolio(new Portfolio().name("Main App").organizationId(org.id), Opts.empty(), superPerson)
    when: "i create an admin group for it"
      Group g = groupApi.createPortfolioGroup(p.id, new Group().name("admin-group").admin(true), superPerson)
    and: "i create another admin group for it"
      Group second = groupApi.createPortfolioGroup(p.id, new Group().name("second").admin(true), superPerson)
    and: "i look for it"
      Group pAdminGroup = groupApi.findPortfolioAdminGroup(p.id, Opts.opts(FillOpts.Members))
    then: "the first admin group exists"
      g.id != null
      g.name != null
      groupApi.getGroup(g.id, Opts.empty(), superPerson).name == g.name
    and: "the second one never got created"
      second == null
    and: "it was able to find the admin group from the portfolio"
      pAdminGroup != null
      pAdminGroup.name == 'admin-group'
  }

  def "i can create more than more non-admin group for a portfolio"() {
    given: "i have a portfolio"
      Portfolio p = portfolioApi.createPortfolio(new Portfolio().name("Main App1").organizationId(org.id), Opts.empty(), superPerson)
    when: "i create a group for it"
      Group g = groupApi.createPortfolioGroup(p.id, new Group().name("non-admin-group"), superPerson)
    and: "i create another group for it"
      Group second = groupApi.createPortfolioGroup(p.id, new Group().name("second-non-admin"), superPerson)
    then: "the first group exists"
      g.id != null
      g.name != null
      groupApi.getGroup(g.id, Opts.empty(), superPerson).name == g.name
    and: "the second one exists"
      second != null
      second.id != null
      groupApi.getGroup(second.id, Opts.empty(), superPerson).name == second.name
  }

  def "i can't create a group for a non-existent portfolio"() {
    when: "i create a group for a fantasy portfolio"
      Group g = groupApi.createPortfolioGroup(UUID.randomUUID().toString(), new Group().name("non-admin-group"), superPerson)
    then:  "no group is created"
      g == null
  }

  def "i can't create a group for a non-existent organization"() {
    when: "i try and create a group for a non existent organization"
      Group g = groupApi.createOrgAdminGroup(UUID.randomUUID().toString(), "whatever", superPerson)
    then:
      g == null
  }

  def "i can't create a group for an organization where a group already exists (1 only)"() {
    when: "try and create another group for admin"
      Group g = groupApi.createOrgAdminGroup(org.id, "whatever", superPerson)
    then:
      g == null
  }

  def "i can only add users to a group that exists"() {
    when:
      Group g = groupApi.addPersonToGroup(UUID.randomUUID().toString(), superuser.toString(), Opts.empty())
    then:
      g == null
  }

  static int counter = 5;

  private Group nonAdminGroup() {
    Portfolio p = portfolioApi.createPortfolio(new Portfolio().name("Main App$counter").organizationId(org.id), Opts.empty(), superPerson)
    Group g = groupApi.createPortfolioGroup(p.id, new Group().name("non-admin-group$counter"), superPerson)
    counter ++
    return g
  }

  def "i can't add non existent people to a group"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    when: "i add a non existent person"
      Group ng = groupApi.addPersonToGroup(g.id, UUID.randomUUID().toString(), Opts.empty())
    then:
      ng == null
  }

  def "i can't create the same portfolio group name twice"() {
    given: "i have a group called ecks"
      groupApi.createPortfolioGroup(commonPortfolio.id, new Group().name("ecks"), superPerson)
    when: "i try and create another group with the same name"
      groupApi.createPortfolioGroup(commonPortfolio.id, new Group().name("ecks"), superPerson)
    then: "it throws a DuplicateGroupException"
      thrown(GroupApi.DuplicateGroupException)
  }

  def "i can't update to the same portfolio group name twice"() {
    given: "i have a group called ecks"
      groupApi.createPortfolioGroup(commonPortfolio.id, new Group().name("update-ecks"), superPerson)
    when: "i try and create another group with the same name"
      def g = groupApi.createPortfolioGroup(commonPortfolio.id, new Group().name("update-ecks1"), superPerson)
      groupApi.updateGroup(g.id, g.name("update-ecks"), true, true, true, Opts.empty())
    then: "it throws a DuplicateGroupException"
      thrown(GroupApi.DuplicateGroupException)
  }


  def "i cannot add the same person to the same group twice"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    and: "i have a person"
      def person = new DbPerson(name: 'rob', email: 'rob-double-group@fred.com')
      database.save(person)
      def personId = person.id.toString()
    when: "i add a person to the group"
      Group ng = groupApi.addPersonToGroup(g.id, personId, Opts.empty())
    and: "i add the person to the group again"
      Group sng = groupApi.addPersonToGroup(g.id, personId, Opts.opts(FillOpts.Members))
    then:
      ng != null
      sng.members.count({m -> m.id.id == personId}) == 1
  }

  def "i can add three people to the same group"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    and: "i have a person"
      List<DbPerson> people = []
      (1..3).each { it ->
        def person = new DbPerson(name: 'rob', email: "rob-same-${it}@fred.com")
        database.save(person)
        people.add(person)
      }
    when: "i add all people to the group"
      Group group
      people.each { DbPerson p ->
        group = groupApi.addPersonToGroup(g.id, p.id.toString(), Opts.opts(FillOpts.Members))
      }
    then:
      group != null
      group.members.size() == 3
  }

  def "i cannot find non-existent portfolio's admin group"() {
    when: "i try and find the admin group of a non existent portfolio"
      Group admin = groupApi.findPortfolioAdminGroup(UUID.randomUUID().toString(), Opts.empty())
    then:
      admin == null
  }

//  def "i cannot find a non-existent organization admin group"() {
//    when:
//      Group admin = groupApi.findOrganizationAdminGroup(UUID.randomUUID().toString(), Opts.opts(FillOpts.Members))
//    then:
//      admin == null
//  }

  def "i can find the admin group of the org"() {
    when:
      Group admin = groupApi.findOrganizationAdminGroup(org.getId(), Opts.opts(FillOpts.Members))
    then:
      admin != null
  }

  def "i can create, add people to it, remove them and finally delete the group"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    and: "i have a person"
      def person = new DbPerson(name: 'rob', email: 'rob-create-delete@fred.com')
      database.save(person)
    and: "i am not a member of the portfolio"
      boolean amNotMember = !groupApi.isPersonMemberOfPortfolioGroup(g.portfolioId, person.id.toString())
    when: "i add a person to the group"
      Group addedToGroup = groupApi.addPersonToGroup(g.id, person.id.toString(), Opts.opts(FillOpts.Members))
    and: "i am confirmed to be a portfolio member"
      boolean amMember = groupApi.isPersonMemberOfPortfolioGroup(g.portfolioId, person.id.toString())
    and: "i delete the person from the group"
      Group deletedFromGroup = groupApi.deletePersonFromGroup(g.id, person.id.toString(), Opts.opts(FillOpts.Members))
    and: "i delete the whole group"
      groupApi.deleteGroup(g.id)
    and: "i try and find the group"
      Group finding = groupApi.getGroup(g.id, Opts.empty(), superPerson)
    and: "am not a portfolio member again"
      boolean amNotMemberAgain = !groupApi.isPersonMemberOfPortfolioGroup(g.portfolioId, person.id.toString())
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
      def person = new DbPerson(name: 'rob', email: 'rob-not-in-group@fred.com')
      database.save(person)
    when: "i try and delete them from the group"
      Group deletedFromGroup = groupApi.deletePersonFromGroup(g.id, person.id.toString(), Opts.opts(FillOpts.Members))
    then:
      deletedFromGroup == null
  }

  def "i cannot delete a non-existent person from a group (no blowing up)"() {
    given: "i have a group"
      Group g = nonAdminGroup()
    when:
      Group deletedFromGroup = groupApi.deletePersonFromGroup(g.id, UUID.randomUUID().toString(), Opts.opts(FillOpts.Members))
    then:
      deletedFromGroup == null
  }

  def "i rename a group"() {
    given: "i have a group"
      Group g = nonAdminGroup().name("new name")
    when: "i rename it"
      groupApi.updateGroup(g.id, g, true, true, true, Opts.empty())
    and: "find it again"
      Group ng = groupApi.getGroup(g.id, Opts.empty(), superPerson)
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
        new Person().id(new PersonId().id(p1.id.toString())),
        new Person().id(new PersonId().id(p2.id.toString())),
      ]
      def g2 = groupApi.updateGroup(g.id, g, true, true, true, new Opts().add(FillOpts.Members))
    and: "I updated the group to remove Alena and add Toya"
      def g2_copy = g2.copy()
      g2_copy.members = [
        new Person().id(new PersonId().id(p1.id.toString())),
        new Person().id(new PersonId().id(p3.id.toString())),
      ]
      groupApi.updateGroup(g.id, g2_copy, true, true, true, new Opts().add(FillOpts.Members))
      def g3 = groupApi.getGroup(g.id, new Opts().add(FillOpts.Members), superPerson)
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
      Group g = groupApi.updateGroup(UUID.randomUUID().toString(), new Group().name("new name"), true, true, true, Opts.empty())
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
        p1Groups.add groupApi.createPortfolioGroup(p1.id, new Group().name("group ${it}"), superPerson)
        p2Groups.add groupApi.createPortfolioGroup(p2.id, new Group().name("group ${it}"), superPerson)
      }
    when: "i search for groups under p1"
      List<Group> groupsP1 = groupApi.findGroups(p1.getId(), null, SortOrder.ASC, Opts.empty())
    and: "just search for 2 in portfolio 1"
      List<Group> groupsP1Just2 = groupApi.findGroups(p1.getId(), '2', SortOrder.ASC, Opts.empty())
    and: "i reverse sort for groups under p2"
      List<Group> groupsP2 = groupApi.findGroups(p2.getId(), null, SortOrder.DESC, Opts.empty())
    and: "just search for 3 in portfolio 2"
      List<Group> groupsP2Just3 = groupApi.findGroups(p2.getId(), '3', SortOrder.DESC, Opts.empty())
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
      def g1 = groupApi.createPortfolioGroup(pi.id, new Group().name("g1-access").admin(true), superPerson)
      def g2 = groupApi.createPortfolioGroup(pi.id, new Group().name("g2-access").admin(true), superPerson)
      def g3 = groupApi.createPortfolioGroup(pi.id, new Group().name("g3-access"), superPerson)
      def g4 = groupApi.createPortfolioGroup(pi.id, new Group().name("g4-access"), superPerson)
    and: "i add a person to this group"
      DbPerson user = new DbPerson.Builder().email("rob-test@featurehub.io").name("Rob test").build();
      database.save(user);
      groupApi.addPersonToGroup(g1.id, user.id.toString(), Opts.empty())
      groupApi.addPersonToGroup(g3.id, user.id.toString(), Opts.empty())
    when: "i get their admin groups"
      List<Group> groups = groupApi.groupsWherePersonIsAnAdminMember(user.id.toString())
    then:
      groups.size() == 1
      groups[0].name == 'g1-access'
  }

  def "I wish to update a group with environment acls"() {
    given: "I have a portfolio"
      Portfolio pi = portfolioApi.createPortfolio(new Portfolio().name("acl test1").organizationId(org.id), Opts.empty(), superPerson)
    and: "I create a group"
      def g1 = groupApi.createPortfolioGroup(pi.id, new Group().name("g1-access-acl1").admin(false), superPerson)
    and: "an application"
      def portfo = database.find(DbPortfolio, UUID.fromString(pi.id))
      def app = new DbApplication.Builder().name("g1-name-acl").portfolio(portfo).whoCreated(user).build()
      database.save(app)
    and: "an environment"
      DbEnvironment env = new DbEnvironment.Builder().name("gp-acl-1").parentApplication(app).whoCreated(user).build()
      database.save(env)
    and: "another environment"
      DbEnvironment env2 = new DbEnvironment.Builder().name("gp-acl-2").parentApplication(app).whoCreated(user).build()
      database.save(env2)
    when: "I update the group to include acls"
      g1.environmentRoles = [new EnvironmentGroupRole().environmentId(env.id.toString()).roles([RoleType.CHANGE_VALUE, RoleType.LOCK])]
      def updGroup = groupApi.updateGroup(g1.id, g1, true, true, true, new Opts().add(FillOpts.Acls))
    and: "i get the group with acls requested"
      def getUpd = groupApi.getGroup(g1.id, new Opts().add(FillOpts.Acls), superPerson)
    and: "the i update the roles"
      g1.environmentRoles = [new EnvironmentGroupRole().environmentId(env.id.toString()).roles([RoleType.LOCK, RoleType.READ])]
      def updGroup1 = groupApi.updateGroup(g1.id, g1, true, true, true, new Opts().add(FillOpts.Acls))
    and: "then i add another environment role and remove the first"
      g1.environmentRoles = [new EnvironmentGroupRole().environmentId(env2.id.toString()).roles([RoleType.UNLOCK, RoleType.READ])]
      def updGroup2 = groupApi.updateGroup(g1.id, g1, true, true, true, new Opts().add(FillOpts.Acls))
    then:
      updGroup.environmentRoles.size() == 1
      updGroup.environmentRoles[0].roles.sort() == [RoleType.CHANGE_VALUE, RoleType.LOCK].sort()
      getUpd.environmentRoles.size() == 1
      getUpd.environmentRoles[0].roles.sort() == [RoleType.CHANGE_VALUE, RoleType.LOCK].sort()
      updGroup1.environmentRoles.size() == 1
      updGroup1.environmentRoles[0].roles.sort() == [RoleType.READ, RoleType.LOCK].sort()
      updGroup2.environmentRoles.size() == 1
      updGroup2.environmentRoles[0].roles.sort() == [RoleType.READ, RoleType.UNLOCK].sort()
  }

  def "i wish to add an application acl to a group"() {
    given: "i create a group with the application acl"
      def g1 = groupApi.createPortfolioGroup(commonPortfolio.id,
        new Group().name("app acl group1")
          .applicationRoles([
            new ApplicationGroupRole()
              .roles([ApplicationRoleType.FEATURE_EDIT])
              .applicationId(commonApplication1.id)
          ]), superPerson)
    and: "i find the group including the acls"
      def found = groupApi.getGroup(g1.id, Opts.opts(FillOpts.Acls), superPerson)
    and: "i update the group to include environment acls"
      def updating = found.copy()
      updating.environmentRoles = [
        new EnvironmentGroupRole().roles([RoleType.CHANGE_VALUE]).environmentId(env1App1.id).groupId(g1.id)
      ]
      updating.applicationRoles.add(new ApplicationGroupRole().roles([ApplicationRoleType.FEATURE_EDIT]).applicationId(commonApplication2.id))
      def up1 = groupApi.updateGroup(g1.id, updating, true, true, true, Opts.opts(FillOpts.Acls))
    when: "i find the group"
      def found1 = groupApi.getGroup(g1.id, Opts.opts(FillOpts.Acls), superPerson)
    then:
      found.applicationRoles.size() == 1
      found1.environmentRoles.size() == 1
      found1.applicationRoles.size() == 2
  }
}
