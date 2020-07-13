package io.featurehub.db.services

import io.ebean.DB
import io.ebean.Database
import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.db.model.query.QDbPerson
import io.featurehub.db.model.query.QDbPortfolio
import io.featurehub.db.publish.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.EnvironmentGroupRole
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.RoleType
import spock.lang.Shared
import spock.lang.Specification

class ApplicationSpec extends BaseSpec {
  @Shared PersonSqlApi personSqlApi
  @Shared DbPortfolio portfolio1
  @Shared DbPortfolio portfolio2
  @Shared ApplicationSqlApi appApi
  @Shared EnvironmentSqlApi environmentSqlApi
  @Shared Person portfolioPerson

  def setupSpec() {
    baseSetupSpec()

    personSqlApi = new PersonSqlApi(database, convertUtils, archiveStrategy)

    environmentSqlApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    appApi = new ApplicationSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    // go create a new person and then portfolios and add this person as a portfolio admin
    portfolioPerson = personSqlApi.createPerson("appspec@mailinator.com", "AppSpec", "appspec", superPerson.id.id, Opts.empty());

    def portfolioSqlApi = new PortfolioSqlApi(database, convertUtils, Mock(ArchiveStrategy))
    def p1 = portfolioSqlApi.createPortfolio(new Portfolio().name("p1-app-1"), Opts.empty(), superPerson);
    def p2 = portfolioSqlApi.createPortfolio(new Portfolio().name("p1-app-2"), Opts.empty(), superPerson);

    portfolio1 = Finder.findPortfolioById(p1.id);
    portfolio2 = Finder.findPortfolioById(p2.id);
  }

  def "i should be able to create, update, and delete an application"() {
    when: "i create an application"
      Application app =  appApi.createApplication(portfolio1.id.toString(), new Application().name("ghost").description("some desc"), superPerson)
    and: "i find it"
      List<Application> found = appApi.findApplications(portfolio1.id.toString(), 'ghost', null, Opts.empty(), superPerson, true)
    and: "i update it"
      Application updated = appApi.updateApplication(app.id, app.name("ghosty"), Opts.empty())
    and: "i delete it"
      boolean deleted = appApi.deleteApplication(portfolio1.id.toString(), app.id)
    and: "i check for the application count after deletion"
      List<Application> afterDelete = appApi.findApplications(portfolio1.id.toString(), 'ghost', null, Opts.empty(), superPerson, true)
    and: "and with include archived on"
      List<Application> afterDeleteWithArchives = appApi.findApplications(portfolio1.id.toString(), 'ghost', null, Opts.opts(FillOpts.Archived), superPerson, true)
    then:
      found != null
      found.size() == 1
      app != null
      updated != null
      deleted == Boolean.TRUE
      afterDelete.size() == 0
      afterDeleteWithArchives.size() == 1
  }

  def "a person who is a member of an environment can see applications in an environment"() {
    when: "i create two applications"
      def app1 = appApi.createApplication(portfolio1.id.toString(), new Application().name("envtest-app1").description("some desc"), superPerson)
      def app2 = appApi.createApplication(portfolio1.id.toString(), new Application().name("envtest-app2").description("some desc"), superPerson)
    and: "a load-all override can find them"
      List<Application> superuserFoundApps = appApi.findApplications(portfolio1.id.toString(), 'envtest-app', null, Opts.empty(), superPerson, true)
    and: "i create a new user who has no group access"
      def user = new DbPerson.Builder().email("envtest-appX@featurehub.io").name("Irina").build();
      database.save(user)
      def person = convertUtils.toPerson(user)
    and: "this person cannot see any apps"
      def notInAnyGroupsAccess = appApi.findApplications(portfolio1.id.toString(), 'envtest-app', null, Opts.empty(), person, false)
    and: "then we give them access to a portfolio group that still has no access"
      Group group = groupSqlApi.createPortfolioGroup(portfolio1.id.toString(), new Group().name("envtest-appX"), superPerson)
      group = groupSqlApi.addPersonToGroup(group.id, person.id.id, Opts.opts(FillOpts.Members))
    and: "the superuser adds to a group as well"
      Group superuserGroup = groupSqlApi.createPortfolioGroup(portfolio1.id.toString(), new Group().name("envtest-appSuperuser"), superPerson)
      superuserGroup = groupSqlApi.addPersonToGroup(superuserGroup.id, superPerson.id.id, Opts.opts(FillOpts.Members))
    and: "with no environment access the two groups have no visibility to applications"
      def stillNotInAnyGroupsPerson = appApi.findApplications(portfolio1.id.toString(), 'envtest-app', null, Opts.empty(), person, false)
      def stillNotInAnyGroupsSuperuser = appApi.findApplications(portfolio1.id.toString(), 'envtest-app', null, Opts.empty(), superPerson, false)
    and: "i add an environment to each application and add group permissions"
      def app1Env1 = environmentSqlApi.create(new Environment().name("dev"), app1, superPerson)
      def app2Env1 = environmentSqlApi.create(new Environment().name("dev"), app2, superPerson)
      group = groupSqlApi.updateGroup(group.id, group.environmentRoles([
	      new EnvironmentGroupRole().environmentId(app1Env1.id).roles([RoleType.READ]),
	      new EnvironmentGroupRole().environmentId(app2Env1.id).roles([RoleType.READ])
      ]), true, true, true, Opts.opts(FillOpts.Members))
      superuserGroup = groupSqlApi.updateGroup(superuserGroup.id, superuserGroup.environmentRoles([
	      new EnvironmentGroupRole().environmentId(app1Env1.id).roles([RoleType.READ])
      ]), true, true, true, Opts.opts(FillOpts.Members))
    and: "person should now be able to see two groups"
      def shouldSeeTwoAppsPerson = appApi.findApplications(portfolio1.id.toString(), 'envtest-app', null, Opts.empty(), person, false)
    and: "superperson should now be able to see 1 group"
      def shouldSeeOneAppsSuperperson = appApi.findApplications(portfolio1.id.toString(), 'envtest-app', null, Opts.empty(), superPerson, false)
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
      appApi.createApplication(portfolio1.id.toString(), new Application().name("ghost1").description("some desc"), superPerson)
      appApi.createApplication(portfolio1.id.toString(), new Application().name("ghost1").description("some desc"), superPerson)
    then:
      thrown ApplicationApi.DuplicateApplicationException
  }

  def "i cannot update two applications to the same name"() {
    when: "i create two applications with the same name"
      appApi.createApplication(portfolio1.id.toString(), new Application().name("ghost1").description("some desc"), superPerson)
      def app2 = appApi.createApplication(portfolio1.id.toString(), new Application().name("ghost2").description("some desc"), superPerson)
      app2.name("ghost1")
      appApi.updateApplication(app2.id, app2, Opts.empty())
    then:
      thrown ApplicationApi.DuplicateApplicationException
  }

  def "i can create two applications with the same name in two different portfolios"() {
    when: "i create two applications with the same name"
      appApi.createApplication(portfolio1.id.toString(), new Application().name("ghost2").description("some desc"), superPerson)
      appApi.createApplication(portfolio2.id.toString(), new Application().name("ghost2").description("some desc"), superPerson)
    then:
      appApi.findApplications(portfolio1.id.toString(), "ghost2", null, Opts.empty(), null, true)
  }
}
