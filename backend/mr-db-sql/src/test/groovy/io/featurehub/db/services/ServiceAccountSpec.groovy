package io.featurehub.db.services


import io.featurehub.db.FilterOptType
import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.publish.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.EnvironmentGroupRole
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.ServiceAccount
import io.featurehub.mr.model.ServiceAccountPermission
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

class ServiceAccountSpec extends BaseSpec {
  private static final Logger log = LoggerFactory.getLogger(ServiceAccountSpec.class)
  @Shared PersonSqlApi personSqlApi
  @Shared ServiceAccountSqlApi sapi
  @Shared ApplicationSqlApi applicationSqlApi
  @Shared EnvironmentSqlApi environmentSqlApi
  @Shared DbPortfolio portfolio1
  @Shared DbApplication application1
  @Shared DbEnvironment environment1
  @Shared DbEnvironment environment2
  @Shared DbEnvironment environment3
  @Shared UUID portfolio1Id
  @Shared EnvironmentApi environmentApi

  def setupSpec() {
    baseSetupSpec()

    personSqlApi = new PersonSqlApi(database, convertUtils, archiveStrategy)
    environmentSqlApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
    applicationSqlApi = new ApplicationSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
    sapi = new ServiceAccountSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    // now set up the environments we need
    UUID orgUUID = org.id
    DbOrganization organization = Finder.findDbOrganization()
    portfolio1 = new DbPortfolio.Builder().name("p1-env-1").whoCreated(dbSuperPerson).organization(organization).build()
    database.save(portfolio1)
    portfolio1Id = portfolio1.id
    def portfolioGroup = groupSqlApi.createPortfolioGroup(portfolio1Id, new Group().admin(true), superPerson)
    application1 = new DbApplication.Builder().name("app-env-1").portfolio(portfolio1).whoCreated(dbSuperPerson).build()
    database.save(application1)
    environment1 = new DbEnvironment.Builder().whoCreated(dbSuperPerson).name("e1").parentApplication(application1).build();
    database.save(environment1)
    environment2 = new DbEnvironment.Builder().whoCreated(dbSuperPerson).name("e2").parentApplication(application1).build();
    database.save(environment2)
    environment3 = new DbEnvironment.Builder().whoCreated(dbSuperPerson).name("e3").parentApplication(application1).build();
    database.save(environment3)

    environmentApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    groupSqlApi.updateGroup(portfolioGroup.id, portfolioGroup.environmentRoles(
      [
        new EnvironmentGroupRole().roles([RoleType.READ]).environmentId(environment1.id),
        new EnvironmentGroupRole().roles([RoleType.READ]).environmentId(environment2.id),
        new EnvironmentGroupRole().roles([RoleType.READ]).environmentId(environment3.id),
      ]
    ), false, false, true, Opts.empty())
  }

  def "service ACL filtering works by application"() {
    given: "i have a second application"
      def app2 = applicationSqlApi.createApplication(portfolio1Id, new Application().name("acl-sa-test-filter").description("acl test filter"), superPerson)
    and: "i have an environment in the second application"
      def env2 = environmentSqlApi.create(new Environment().name("acl-sa-test-filter-env").description("acl-test-filter-env"), app2, superPerson)
    and: "i create a new service account"
      def sa = new ServiceAccount().name('sa-acl-filter').description('sa-acl-filter')
          .permissions([
            new ServiceAccountPermission()
              .environmentId(environment1.id)
              .permissions([RoleType.LOCK]),
            new ServiceAccountPermission()
              .environmentId(env2.id)
              .permissions([RoleType.CHANGE_VALUE])
          ])
      sa = sapi.create(portfolio1Id, superPerson, sa, Opts.opts(FillOpts.Permissions))
    when: "i query for the service account filtering by the new app"
      def saFiltered = sapi.get(sa.id, Opts.opts(FillOpts.Permissions).add(FilterOptType.Application, app2.id))
    then:
      sa.permissions.size() == 2
      saFiltered.permissions.size() == 1
      saFiltered.permissions[0].permissions.containsAll([RoleType.READ, RoleType.CHANGE_VALUE])
  }

  def "i can create a service account with no environments"() {
    given: "i have a service account"
      def sa = new ServiceAccount()
        .name("sa-2").description("sa-1 test")
    when: "i save it"
      sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    then:
      sapi.search(portfolio1Id, "sa-2", null, superPerson,
        Opts.empty()).find({it.name == 'sa-2' && sa.description == 'sa-1 test'})?.apiKeyClientSide != null
      sapi.search(portfolio1Id, "sa-2", null, superPerson,
        Opts.empty()).find({it.name == 'sa-2' && sa.description == 'sa-1 test'})?.apiKeyServerSide != null
  }

  def "i can reset the key for a service account"() {
    given: "i have a service account"
      def sa = new ServiceAccount().name("sa-reset").description("sa-1 test")
        .permissions([
          new ServiceAccountPermission()
            .permissions([RoleType.READ])
            .environmentId(environment1.id)
        ])
    when: "i save it"
      def created = sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    and: "i reset the key"
      sapi.resetApiKey(created.id, true, true)
    and: "i find the updated api"
      def search = sapi.search(portfolio1Id, "sa-reset", application1.id, superPerson,
        Opts.opts(FillOpts.Permissions, FillOpts.SdkURL))
    print("search is $search")
      def resetSa = search.find({it.name == 'sa-reset' && sa.description == 'sa-1 test'})
    then:
      created.apiKeyClientSide != null
      created.apiKeyServerSide != null
      search != null
      resetSa != null
      resetSa?.apiKeyClientSide != created.apiKeyClientSide
      resetSa?.apiKeyServerSide != created.apiKeyServerSide
      !resetSa.permissions.isEmpty()
      resetSa.permissions.count(p -> p.sdkUrlClientEval.contains(resetSa.apiKeyClientSide)) == resetSa.permissions.size()
      resetSa.permissions.count(p -> p.sdkUrlServerEval.contains(resetSa.apiKeyServerSide)) == resetSa.permissions.size()
  }

  def "i can create then delete a service account"() {
    given: "i have a service account"
      def sa = new ServiceAccount().name("sa-delete").description("sa-1 test").permissions(
        [new ServiceAccountPermission()
           .permissions([RoleType.READ, RoleType.CHANGE_VALUE])
           .environmentId(environment1.id),
        ]
      )
    and: "i create it"
      def created = sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    when: "i delete it"
      def result = sapi.delete(superPerson, created.id)
    and: "try and find it"
      def search = sapi.search(portfolio1Id, 'sa-delete', null, superPerson, Opts.empty())
    then:
      created != null
      result
      search.size() == 0
  }

  def "i cannot create two service accounts with the same name"() {
    given: "i have a service account"
      def sa = new ServiceAccount().name("sa-dupe").description("sa-1 test")
    when: "i create it"
      sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    and: "do it again"
      sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    then:
      thrown ServiceAccountApi.DuplicateServiceAccountException
  }

  def "i can create two service accounts for an environment"() {
    given: "i have an environment"
      def app1 = convertUtils.toApplication(application1, Opts.empty())
      def env1 = environmentApi.create(new Environment().description('app-twosa-env1').name('app-twosa-env1'), app1, superPerson)
    and: "i have a service account"
      def sa1 = sapi.create(portfolio1Id, superPerson, new ServiceAccount().name("twosa-sa-1").description("sa-1 test").permissions(
        [new ServiceAccountPermission()
           .permissions([RoleType.READ, RoleType.CHANGE_VALUE])
           .environmentId(env1.id),
        ]
      ), Opts.empty())
    and: "i have another service account"
      def sa2 = sapi.create(portfolio1Id, superPerson, new ServiceAccount().name("twosa-sa-2").description("sa-2 test").permissions(
        [new ServiceAccountPermission()
           .permissions([RoleType.LOCK])
           .environmentId(env1.id),
        ]
      ), Opts.empty())
    when: "i ask for the environment with its permissions"
      def env1Details = environmentApi.get(env1.id, Opts.opts(FillOpts.ServiceAccounts), superPerson)
    then: "i will see it has two sets of service accounts and permissions attached"
      env1Details.serviceAccountPermission.size() == 2
      env1Details.serviceAccountPermission.find({it.serviceAccount.id == sa1.id})?.environmentId == env1Details.id
      env1Details.serviceAccountPermission.find({it.serviceAccount.id == sa1.id})?.permissions?.containsAll([RoleType.READ, RoleType.CHANGE_VALUE])
      env1Details.serviceAccountPermission.find({it.serviceAccount.id == sa2.id})?.environmentId == env1Details.id
      env1Details.serviceAccountPermission.find({it.serviceAccount.id == sa2.id})?.permissions?.containsAll([RoleType.READ, RoleType.LOCK])
  }

  def "i can create a service account with permissions in two environments, then update it to remove one environment and change permission on other"() {
    given: "i have three environments"
      def app1 = convertUtils.toApplication(application1, Opts.empty())
      def env1 = environmentApi.create(new Environment().description('app-sa1-env1').name('app-sa1-env1'), app1, superPerson)
      def env2 = environmentApi.create(new Environment().description('app-sa1-env2').name('app-sa1-env2'), app1, superPerson)
      def env3 = environmentApi.create(new Environment().description('app-sa1-env3').name('app-sa1-env3'), app1, superPerson)
    and: "i have a service account with two environments"
      def sa = new ServiceAccount().name("sa-1").description("sa-1 test").permissions(
        [new ServiceAccountPermission()
           .permissions([RoleType.READ, RoleType.CHANGE_VALUE])
          .environmentId(env1.id),
          new ServiceAccountPermission()
          .permissions([RoleType.LOCK, RoleType.UNLOCK])
          .environmentId(env2.id)
        ]
      )
    when: "i save it"
      def createdServiceAccount = sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    and: "i get the result"
      // compare by name to ensure the SA was saved with the right name and desc
      def result = sapi.search(portfolio1Id, "sa-1", app1.id, superPerson, Opts.opts(FillOpts.Permissions)).find({it.name == 'sa-1' && sa.description == 'sa-1 test'})
    and: "i re-get the two environments with extra data"
      def newEnv1 = environmentApi.get(env1.id, Opts.opts(FillOpts.ServiceAccounts, FillOpts.SdkURL), superPerson)
      def newEnv2 = environmentApi.get(env2.id, Opts.opts(FillOpts.ServiceAccounts, FillOpts.SdkURL), superPerson)
    and: "i check the result for the two environments"
      def permE1 = result.permissions.find({it.environmentId == env1.id})
      def permE2 = result.permissions.find({it.environmentId == env2.id})
    and: "then update the service account to remove environment 2 and remove toggle-enabled"
      def updated = new ServiceAccount()
        .version(createdServiceAccount.version)
        .description('sa-2 test')
        .permissions(
        [new ServiceAccountPermission()
           .permissions([RoleType.CHANGE_VALUE])
           .environmentId(env1.id),
         new ServiceAccountPermission()
           .environmentId(env2.id),
         new ServiceAccountPermission()
           .permissions([RoleType.LOCK, RoleType.UNLOCK])
           .environmentId(env3.id)
        ]
      )

      def secondUpdate = sapi.update(createdServiceAccount.id, superPerson, updated, Opts.opts(FillOpts.Permissions))
    and: "search for the result"
      def updatedResult = sapi.search(portfolio1Id, "sa-1", application1.id, superPerson, Opts.opts(FillOpts.Permissions)).find({it.id == createdServiceAccount.id})

      def upd1 = updatedResult.permissions.find({it.environmentId == env1.id})
      def upd2 = updatedResult.permissions.find({it.environmentId == env2.id})
      def upd3 = updatedResult.permissions.find({it.environmentId == env3.id})
      def getted = sapi.get(updatedResult.id, Opts.opts(FillOpts.Permissions))
    then:
      updatedResult.id == createdServiceAccount.id
      result.permissions.size() == 2
      permE1.permissions.containsAll([RoleType.CHANGE_VALUE, RoleType.READ])
      permE1.environmentId == env1.id
      permE2.permissions.containsAll([RoleType.LOCK, RoleType.UNLOCK, RoleType.READ]) // READ should be implicit
      permE2.environmentId == env2.id
      upd1?.permissions?.containsAll([RoleType.CHANGE_VALUE, RoleType.READ])
      upd2 == null
      upd3?.permissions?.containsAll([RoleType.LOCK, RoleType.UNLOCK, RoleType.READ])
      getted.permissions.each { p -> assert updatedResult.permissions.find(p1 -> p1.id == p.id) == p}
      updatedResult.description == 'sa-2 test'
    // now check it from the environment perspective
      newEnv1.serviceAccountPermission.size() == 1
      newEnv2.serviceAccountPermission.size() == 1
      newEnv1.serviceAccountPermission.find({ it.serviceAccount.name == 'sa-1'})
      newEnv2.serviceAccountPermission.find({ it.serviceAccount.name == 'sa-1'})
      newEnv1.serviceAccountPermission.find({ it.serviceAccount.name == 'sa-1'}).permissions.containsAll([RoleType.READ, RoleType.CHANGE_VALUE])
      newEnv2.serviceAccountPermission.find({ it.serviceAccount.name == 'sa-1'}).permissions.containsAll([RoleType.LOCK, RoleType.UNLOCK, RoleType.READ])
      newEnv1.serviceAccountPermission.find({ it.serviceAccount.name == 'sa-1'}).sdkUrlClientEval.contains("/" + newEnv1.serviceAccountPermission.find({ it.serviceAccount.name == 'sa-1'}).serviceAccount.apiKeyClientSide)
      newEnv1.serviceAccountPermission.find({ it.serviceAccount.name == 'sa-1'}).sdkUrlServerEval.contains("/" + newEnv1.serviceAccountPermission.find({ it.serviceAccount.name == 'sa-1'}).serviceAccount.apiKeyServerSide)
  }

  def "I cannot request or update an unknown service account"() {
    when:
      def x = sapi.update(UUID.randomUUID(), superPerson, new ServiceAccount(), Opts.empty())
      def y = sapi.get(UUID.randomUUID(), Opts.empty())
    then:
      x == null
      y == null
  }
}
