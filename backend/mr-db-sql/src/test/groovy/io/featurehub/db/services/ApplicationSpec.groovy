package io.featurehub.db.services

import io.featurehub.db.FilterOptType
import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
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
import spock.lang.Shared

class ApplicationSpec extends BaseSpec {
  @Shared PersonSqlApi personSqlApi
  @Shared DbPortfolio portfolio1
  @Shared DbPortfolio portfolio2
  @Shared ApplicationSqlApi appApi
  @Shared EnvironmentSqlApi environmentSqlApi
  @Shared Person portfolioPerson
  @Shared Group p1AdminGroup

  def setupSpec() {
    baseSetupSpec()

    personSqlApi = new PersonSqlApi(database, convertUtils, archiveStrategy, Mock(InternalGroupSqlApi))

    environmentSqlApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    appApi = new ApplicationSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    // go create a new person and then portfolios and add this person as a portfolio admin
    portfolioPerson = personSqlApi.createPerson("appspec@mailinator.com", "AppSpec", "appspec", superPerson.id.id, Opts.empty());

    def portfolioSqlApi = new PortfolioSqlApi(database, convertUtils, Mock(ArchiveStrategy))
    def p1 = portfolioSqlApi.createPortfolio(new Portfolio().name("p1-app-1"), Opts.empty(), superPerson);
    def p2 = portfolioSqlApi.createPortfolio(new Portfolio().name("p1-app-2"), Opts.empty(), superPerson);

    portfolio1 = Finder.findPortfolioById(p1.id);
    portfolio2 = Finder.findPortfolioById(p2.id);

    p1AdminGroup = groupSqlApi.createGroup(portfolio1.id, new Group().name("envtest-appX").admin(true), superPerson)

  }

  def "i should be able to create, update, and delete an application"() {
    when: "i create an application"
      Application app =  appApi.createApplication(portfolio1.id, new Application().name("ghost").description("some desc"), superPerson)
    and: "i find it"
      List<Application> found = appApi.findApplications(portfolio1.id, 'ghost', null, Opts.empty(), superPerson, true)
    and: "i update it"
      Application updated = appApi.updateApplication(app.id, app.name("ghosty"), Opts.empty())
    and: "i delete it"
      boolean deleted = appApi.deleteApplication(portfolio1.id, app.id)
    and: "i check for the application count after deletion"
      List<Application> afterDelete = appApi.findApplications(portfolio1.id, 'ghost', null, Opts.empty(), superPerson, true)
    and: "and with include archived on"
      List<Application> afterDeleteWithArchives = appApi.findApplications(portfolio1.id, 'ghost', null, Opts.opts(FillOpts.Archived), superPerson, true)
    then:
      found != null
      found.size() == 1
      app != null
      updated != null
      deleted == Boolean.TRUE
      afterDelete.size() == 0
      afterDeleteWithArchives.size() == 1
  }


  def "I should be able to have two portfolios with the same application name"() {
    when: "i have and application in each portfolio with different names"
        Application p1App =  appApi.createApplication(portfolio1.id, new Application().name("dupe-name1").description("some desc"), superPerson)
        Application p2App =  appApi.createApplication(portfolio2.id, new Application().name("dupe-name2").description("some desc"), superPerson)
    and: "i rename the second to the same as the first"
      def app2 = appApi.updateApplication(p2App.id, p2App.name("dupe-name1"), Opts.empty())
    then:
        app2.name == 'dupe-name1'
  }


  def "a person who is a member of an environment can see applications in an environment"() {
    when: "i create two applications"
      def app1 = appApi.createApplication(portfolio1.id, new Application().name("envtest-app1").description("some desc"), superPerson)
      def app2 = appApi.createApplication(portfolio1.id, new Application().name("envtest-app2").description("some desc"), superPerson)
    and: "a load-all override can find them"
      List<Application> superuserFoundApps = appApi.findApplications(portfolio1.id, 'envtest-app', null, Opts.empty(), superPerson, true)
    and: "i create a new user who has no group access"
      def user = new DbPerson.Builder().email("envtest-appX@featurehub.io").name("Irina").build();
      database.save(user)
      def person = convertUtils.toPerson(user)
    and: "this person cannot see any apps"
      def notInAnyGroupsAccess = appApi.findApplications(portfolio1.id, 'envtest-app', null, Opts.empty(), person, false)
    and: "then we give them access to a portfolio group that still has no access"
      Group group = groupSqlApi.createGroup(portfolio1.id, new Group().name("envtest-appX1"), superPerson)
      group = groupSqlApi.addPersonToGroup(group.id, person.id.id, Opts.opts(FillOpts.Members))
    and: "the superuser adds to a group as well"
      Group superuserGroup = groupSqlApi.createGroup(portfolio1.id, new Group().name("envtest-appSuperuser"), superPerson)
      superuserGroup = groupSqlApi.addPersonToGroup(superuserGroup.id, superPerson.id.id, Opts.opts(FillOpts.Members))
    and: "with no environment access the two groups have no visibility to applications"
      def stillNotInAnyGroupsPerson = appApi.findApplications(portfolio1.id, 'envtest-app', null, Opts.empty(), person, false)
      def stillNotInAnyGroupsSuperuser = appApi.findApplications(portfolio1.id, 'envtest-app', null, Opts.empty(), superPerson, false)
    and: "i add an environment to each application and add group permissions"
      def app1Env1 = environmentSqlApi.create(new Environment().name("dev"), app1, superPerson)
      def app2Env1 = environmentSqlApi.create(new Environment().name("dev"), app2, superPerson)
      group = groupSqlApi.updateGroup(group.id, group.environmentRoles([
	      new EnvironmentGroupRole().environmentId(app1Env1.id).roles([RoleType.READ]),
	      new EnvironmentGroupRole().environmentId(app2Env1.id).roles([RoleType.READ])
      ]), null, true, true, true, Opts.opts(FillOpts.Members))
      superuserGroup = groupSqlApi.updateGroup(superuserGroup.id, superuserGroup.environmentRoles([
	      new EnvironmentGroupRole().environmentId(app1Env1.id).roles([RoleType.READ])
      ]), null, true, true, true, Opts.opts(FillOpts.Members))
    and: "person should now be able to see two groups"
      def shouldSeeTwoAppsPerson = appApi.findApplications(portfolio1.id, 'envtest-app', null, Opts.empty(), person, false)
    and: "superperson should now be able to see 1 group"
      def shouldSeeOneAppsSuperperson = appApi.findApplications(portfolio1.id, 'envtest-app', null, Opts.empty(), superPerson, false)
    then:
      notInAnyGroupsAccess.size() == 0
      stillNotInAnyGroupsPerson.size() == 0
      stillNotInAnyGroupsSuperuser.size() == 0
      superuserFoundApps.size() == 2
      shouldSeeTwoAppsPerson.size() == 2
      shouldSeeOneAppsSuperperson.size() == 1
  }

  def "i cannot create two applications with the same name"() {
    when: "i create two applications with the same name"
      appApi.createApplication(portfolio1.id, new Application().name("ghost1").description("some desc"), superPerson)
      appApi.createApplication(portfolio1.id, new Application().name("ghost1").description("some desc"), superPerson)
    then:
      thrown ApplicationApi.DuplicateApplicationException
  }

  def "i cannot update two applications to the same name"() {
    when: "i create two applications with the same name"
      appApi.createApplication(portfolio1.id, new Application().name("ghost1").description("some desc"), superPerson)
      def app2 = appApi.createApplication(portfolio1.id, new Application().name("ghost2").description("some desc"), superPerson)
      app2.name("ghost1")
      appApi.updateApplication(app2.id, app2, Opts.empty())
    then:
      thrown ApplicationApi.DuplicateApplicationException
  }

  def "i can create two applications with the same name in two different portfolios"() {
    when: "i create two applications with the same name"
      appApi.createApplication(portfolio1.id, new Application().name("ghost2").description("some desc"), superPerson)
      appApi.createApplication(portfolio2.id, new Application().name("ghost2").description("some desc"), superPerson)
    then:
      appApi.findApplications(portfolio1.id, "ghost2", null, Opts.empty(), superPerson, true)
  }

  def "two applications in the same group can have create/feature permissions"() {
    given: "i create two applications"
      def app1 = appApi.createApplication(portfolio1.id, new Application().name("loicoudot 1").description("some desc"), superPerson)
      def app2 = appApi.createApplication(portfolio1.id, new Application().name("loicoudot 2").description("some desc"), superPerson)
    and: "i have a new group"
      def group = groupSqlApi.createGroup(portfolio1.id, new Group().name("loicoudot"), superPerson)
    when:
      def group1 = groupSqlApi.updateGroup(group.id,
        group.applicationRoles([new ApplicationGroupRole().applicationId(app1.id).roles([ApplicationRoleType.EDIT])]),
        app1.id,
        false, true, false, Opts.opts(FillOpts.Acls))
    and:
      def group2 = groupSqlApi.getGroup(group1.id, Opts.empty(), superPerson)
      group2.applicationRoles.add(new ApplicationGroupRole().applicationId(app2.id).roles([ApplicationRoleType.EDIT]))
      def group3 = groupSqlApi.updateGroup(group.id, group2, app2.id, false, true, false,
        Opts.opts(FillOpts.Acls))
    and: "i delete the role but just from application 2"
      def group4 = groupSqlApi.getGroup(group1.id, Opts.empty(), superPerson)
      def group5 = groupSqlApi.updateGroup(group.id, group4, app2.id, false, true, false,
        Opts.opts(FillOpts.Acls))
    then:
      group3.applicationRoles.size() == 2
      group3.applicationRoles.find({it.applicationId == app1.id}).roles.contains(ApplicationRoleType.EDIT)
      group3.applicationRoles.find({it.applicationId == app2.id}).roles.contains(ApplicationRoleType.EDIT)
      group5.applicationRoles.size() == 1
      group5.applicationRoles.find({it.applicationId == app2.id}) == null
  }

  def "the auto-created portfolio group has feature create/edit permissions"() {
    given: "i create an application"
      def app1 = appApi.createApplication(portfolio1.id,
        new Application().name("irinas-perm-create1").description("perm-create"), superPerson)
    when: "i convert the portfolio group to a model"
      def group = groupSqlApi.getGroup(adminGroup.id, Opts.opts(FillOpts.Acls).add(FilterOptType.Application, app1.id), superPerson)
    then:
      group.applicationRoles.find({ it.applicationId == app1.id }).roles.containsAll([ApplicationRoleType.EDIT_AND_DELETE,
                                                                                      ApplicationRoleType.CREATE])
  }

  def "application groups can store multiple Acls"() {
    given: "i create an application"
      def app1 = appApi.createApplication(portfolio1.id, new Application().name("perm-create1").description("perm-create"), superPerson)
    and: "i have a new group"
      def createdGroup = groupSqlApi.createGroup(portfolio1.id, new Group().name("perm-create-group"), superPerson)
    and: "create a new person to join the group"
      def deepme = new DbPerson.Builder().email("dj-deepme@featurehub.io").name("DJ DeepMe").build()
      database.save(deepme)
      def personIsCreator0 = appApi.personIsFeatureCreator(app1.id, deepme.id)
      def personIsEditor0 = appApi.personIsFeatureEditor(app1.id, deepme.id)
      createdGroup.addMembersItem(new Person().id(new PersonId().id(deepme.id)))
      def group = groupSqlApi.updateGroup(createdGroup.id, createdGroup, null, true, false, false, Opts.opts(FillOpts.Acls))
    when: "i add create permissions for the app to the group"
      def groupWithCreatePerms = groupSqlApi.updateGroup(group.id,
        group.applicationRoles([new ApplicationGroupRole().applicationId(app1.id).roles([ApplicationRoleType.CREATE])]),
        app1.id,
        false, true, false, Opts.opts(FillOpts.Acls))
      def creators1 = appApi.findFeatureCreators(app1.id)
      def editors1 = appApi.findFeatureEditors(app1.id)
      def personIsCreator1 = appApi.personIsFeatureCreator(app1.id, deepme.id)
      def personIsEditor1 = appApi.personIsFeatureEditor(app1.id, deepme.id)
    and: "i add edit permissions"
      def group2 = groupSqlApi.getGroup(group.id, Opts.opts(FillOpts.Acls).add(FilterOptType.Application, app1.id), superPerson)
      group2.applicationRoles[0].roles.add(ApplicationRoleType.EDIT_AND_DELETE)
      def groupWithEditAndCreatePerms = groupSqlApi.updateGroup(group.id, group2, app1.id, false,
        true, false, Opts.opts(FillOpts.Acls))
      def creators2 = appApi.findFeatureCreators(app1.id)
      def editors2 = appApi.findFeatureEditors(app1.id)
      def personIsCreator2 = appApi.personIsFeatureCreator(app1.id, deepme.id)
      def personIsEditor2 = appApi.personIsFeatureEditor(app1.id, deepme.id)
    and: "i remove the create role"
      def group3 = groupSqlApi.getGroup(group.id, Opts.opts(FillOpts.Acls).add(FilterOptType.Application, app1.id), superPerson)
      group3.applicationRoles[0].roles.removeIf { it == ApplicationRoleType.EDIT_AND_DELETE }
      def groupWithEditPerms = groupSqlApi.updateGroup(group.id, group3, app1.id, false, true, false, Opts.opts(FillOpts.Acls))
      def creators3 = appApi.findFeatureCreators(app1.id)
      def editors3 = appApi.findFeatureEditors(app1.id)
      def personIsCreator3 = appApi.personIsFeatureCreator(app1.id, deepme.id)
      def personIsEditor3 = appApi.personIsFeatureEditor(app1.id, deepme.id)
    then:
      groupWithCreatePerms.applicationRoles.find({it.applicationId == app1.id}).roles.containsAll([ApplicationRoleType.CREATE])
      groupWithEditAndCreatePerms.applicationRoles.find({it.applicationId == app1.id}).roles.containsAll([ApplicationRoleType.CREATE, ApplicationRoleType.EDIT_AND_DELETE])
      !groupWithEditPerms.applicationRoles.find({it.applicationId == app1.id}).roles.containsAll([ApplicationRoleType.EDIT_AND_DELETE])
      // admin group always has the superadmin in it with a creator + edit role
      creators1.size() == 2
      creators1.contains(deepme.id)
      editors1.size() == 1
      !editors1.containsAll(deepme.id)
      creators2.size() == 2
      editors2.size() == 2
      editors2.containsAll(deepme.id)
      creators3.size() == 2
      editors3.size() == 1
      !personIsCreator0
      personIsCreator1
      personIsCreator2
      personIsCreator3
      !personIsEditor0
      !personIsEditor1
      !personIsEditor3
      personIsEditor2
  }

  // based on this article: https://en.wikipedia.org/wiki/Ukrainian_surnames#Cossack_names
  def "i create an application and give different people access and confirm they have reader access"() {
    given: "i create one Plaksa who cannot access anything"
      def plaska = new DbPerson.Builder().email("plaksa-app1@featurehub.io").name("Plaksa").build()
      database.save(plaska)
      def plaskaNoAccess = convertUtils.toPerson(plaska)
    and: "i create one Prilipko who is a portfolio admin"
      def prilipko = new DbPerson.Builder().email("prilipko-port-mgr@mailinator.com").name("Prilipko").build()
      database.save(prilipko)
      def prilipkoPortfolioAdmin = convertUtils.toPerson(prilipko)
      def g = groupSqlApi.getGroup(p1AdminGroup.id, Opts.opts(FillOpts.Members), superPerson)
      g.members.add(prilipkoPortfolioAdmin)
      groupSqlApi.updateGroup(p1AdminGroup.id, g, null, true, false, false, Opts.empty())
    and: "I create a new portfolio group and add in Golodryga by he has no permission to an application"
      def golodryga = new DbPerson.Builder().email("Golodryga@m.com").name("Golodryga").build()
      database.save(golodryga)
      def golodrygaPortfolioMemberButNoApplicationAccess = convertUtils.toPerson(golodryga)
      groupSqlApi.createGroup(portfolio1.id, new Group().name("Twitchy Group").members([golodrygaPortfolioMemberButNoApplicationAccess]), superPerson)
    and: "I create a new portfolio group and add in Sverbylo and give her read access to production"
      def sverbylo = new DbPerson.Builder().email("sverbylo@m.com").name("Sverbylo").build()
      database.save(sverbylo)
      def sverbyloHasReadAccess = convertUtils.toPerson(sverbylo)
      def iGroup = groupSqlApi.createGroup(portfolio1.id, new Group().name("Itchy Group"), superPerson)
      iGroup = groupSqlApi.updateGroup(iGroup.id, iGroup.members([sverbyloHasReadAccess]), null, true, false, false, Opts.empty())
    when: "i create a new application"
      def newApp = appApi.createApplication(portfolio1.id, new Application().name("app-perm-check-appl1"), superPerson)
    and: "a new environment"
      def env = environmentSqlApi.create(new Environment().name("production").production(true), newApp, superPerson)
    and: "i grant the iGroup access to it"
      groupSqlApi.updateGroup(iGroup.id, iGroup.environmentRoles(
        [new EnvironmentGroupRole().roles([RoleType.READ]).environmentId(env.id)]), null, false, false, true, Opts.empty())
    then: "Prilipko is a portfolio admin and can see read the application even with no app direct access"
      appApi.personIsFeatureReader(newApp.id, prilipkoPortfolioAdmin.id.id)
     and: "Golodryga cannot see the application's features"
      !appApi.personIsFeatureReader(newApp.id, golodrygaPortfolioMemberButNoApplicationAccess.id.id)
     and: "Irina can see the features because she is the superuser"
      appApi.personIsFeatureReader(newApp.id, superPerson.id.id)
     and: "Plaska cannot see the features because she is not even a portfolio member"
      !appApi.personIsFeatureReader(newApp.id, plaskaNoAccess.id.id)
     and: "Sverbylo can see the features because she was given read access to the production environment in the application"
      appApi.personIsFeatureReader(newApp.id, sverbyloHasReadAccess.id.id)
     and: "Prilipko, Sverbylo are in feature readers list" // Irina is added by the MR layer
      appApi.findFeatureReaders(newApp.id).containsAll([sverbyloHasReadAccess.id.id, prilipkoPortfolioAdmin.id.id])
     and: "Golodryga and Plaska are not"
       !appApi.findFeatureReaders(newApp.id).contains(golodrygaPortfolioMemberButNoApplicationAccess.id.id)
       !appApi.findFeatureReaders(newApp.id).contains(plaskaNoAccess.id.id)
  }
}
