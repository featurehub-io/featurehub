package io.featurehub.db.services

import groovy.transform.CompileStatic
import io.featurehub.db.FilterOptType
import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.DbServiceAccount
import io.featurehub.db.model.query.QDbPerson
import io.featurehub.db.model.query.QDbServiceAccount
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.CreateApplication
import io.featurehub.mr.model.CreateEnvironment
import io.featurehub.mr.model.CreateGroup
import io.featurehub.mr.model.CreateServiceAccount
import io.featurehub.mr.model.EnvironmentGroupRole
import io.featurehub.mr.model.PersonType
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.ServiceAccount
import io.featurehub.mr.model.ServiceAccountPermission
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ServiceAccount2Spec extends Base2Spec {
  private static final Logger log = LoggerFactory.getLogger(ServiceAccount2Spec.class)
  PersonSqlApi personSqlApi
  ServiceAccountSqlApi sapi
  ApplicationSqlApi applicationSqlApi
  EnvironmentSqlApi environmentSqlApi
  DbPortfolio portfolio1
  DbApplication application1
  DbEnvironment environment1
  DbEnvironment environment2
  DbEnvironment environment3
  UUID portfolio1Id
  EnvironmentApi environmentApi
  CacheSource cacheSource
  InternalGroupSqlApi internalGroupSqlApi

  def setup() {
    db.commitTransaction()
    internalGroupSqlApi = Mock()
    personSqlApi = new PersonSqlApi(db, convertUtils, archiveStrategy, internalGroupSqlApi)
    cacheSource = Mock(CacheSource)
    environmentSqlApi = new EnvironmentSqlApi(db, convertUtils, cacheSource, archiveStrategy)
    applicationSqlApi = new ApplicationSqlApi(convertUtils, cacheSource, archiveStrategy, Mock(InternalFeatureApi))
    sapi = new ServiceAccountSqlApi(convertUtils, cacheSource, archiveStrategy, personSqlApi)

    // now set up the environments we need
//    UUID orgUUID = org.id
    DbOrganization organization = Finder.findDbOrganization()
    portfolio1 = new DbPortfolio.Builder().name(RandomStringUtils.randomAlphabetic(8) + "p1-env-1").whoCreated(dbSuperPerson).organization(organization).build()
    db.save(portfolio1)
    portfolio1Id = portfolio1.id
    def portfolioGroup = groupSqlApi.createGroup(portfolio1Id, new CreateGroup().name("group1").admin(true), superPerson)
    application1 = new DbApplication.Builder().name("app-env-1").portfolio(portfolio1).whoCreated(dbSuperPerson).build()
    db.save(application1)
    environment1 = new DbEnvironment.Builder().whoCreated(dbSuperPerson).name("e1").parentApplication(application1).build()
    db.save(environment1)
    environment2 = new DbEnvironment.Builder().whoCreated(dbSuperPerson).name("e2").parentApplication(application1).build()
    db.save(environment2)
    environment3 = new DbEnvironment.Builder().whoCreated(dbSuperPerson).name("e3").parentApplication(application1).build()
    db.save(environment3)

    environmentApi = new EnvironmentSqlApi(db, convertUtils, Mock(CacheSource), archiveStrategy)

    groupSqlApi.updateGroup(portfolioGroup.id, portfolioGroup.environmentRoles(
      [
        new EnvironmentGroupRole().roles([io.featurehub.mr.model.RoleType.READ]).environmentId(environment1.id),
        new EnvironmentGroupRole().roles([io.featurehub.mr.model.RoleType.READ]).environmentId(environment2.id),
        new EnvironmentGroupRole().roles([io.featurehub.mr.model.RoleType.READ]).environmentId(environment3.id),
      ]
    ), null, false, false, true, Opts.empty())

    if (db.currentTransaction() != null && db.currentTransaction().active) {
      db.commitTransaction()
    }
  }

  @CompileStatic
  DbServiceAccount findServiceAccount(UUID id) {
    def found = new QDbServiceAccount().id.eq(id).findOne()
    if (found) {
      found.refresh()
    }
    return found
  }

  def "I create two service accounts and they both get unpublished when I unpublish all service accounts"() {
    given: "I have two service accounts"
      def sa1 = sapi.create(portfolio1Id, superPerson,
        new CreateServiceAccount().name("sa01").description("sa1111"), new Opts())
      def sa2 = sapi.create(portfolio1Id, superPerson,
        new CreateServiceAccount().name("sa02").description("sa222"), new Opts())
    when: "I unpublish service accounts"
      sapi.unpublishServiceAccounts(portfolio1Id, null)
    then:
      1 * cacheSource.deleteServiceAccount({ UUID id -> id == sa1.id })
      1 * cacheSource.deleteServiceAccount({ UUID id -> id == sa2.id })
      findServiceAccount(sa1.id).whenUnpublished != null
      findServiceAccount(sa2.id).whenUnpublished != null
  }

  def "when i create two service accounts but ask to unpublish only one, then only one is unpublished"() {
    given: "I have two service accounts"
      def sa1 = sapi.create(portfolio1Id, superPerson,
        new CreateServiceAccount().name("sa01").description("sa1111"), new Opts())
      def sa2 = sapi.create(portfolio1Id, superPerson,
        new CreateServiceAccount().name("sa02").description("sa222"), new Opts())
    when: "I unpublish service accounts"
      sapi.unpublishServiceAccounts(portfolio1Id, [sa2.id])
    then:
      0 * cacheSource.deleteServiceAccount({ UUID id -> id == sa1.id })
      1 * cacheSource.deleteServiceAccount({ UUID id -> id == sa2.id })
      findServiceAccount(sa1.id).whenUnpublished == null
      findServiceAccount(sa2.id).whenUnpublished != null
  }

  def "service ACL filtering works by application"() {
    given: "i have a second application"
      def app2 = applicationSqlApi.createApplication(portfolio1Id, new CreateApplication().name("acl-sa-test-filter").description("acl test filter"), superPerson)
    and: "i have an environment in the second application"
      def env2 = environmentSqlApi.create(new CreateEnvironment().name("acl-sa-test-filter-env").description("acl-test-filter-env"), app2.id, superPerson)
    and: "i create a new service account"
      def sa = new CreateServiceAccount().name('sa-acl-filter').description('sa-acl-filter')
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
      applicationSqlApi.getApplicationSummary(app2.id).serviceAccountsHavePermission
  }

  @CompileStatic
  int numberOfPeople() {
    return new QDbPerson().findCount()
  }

  def "i can create a service account with no environments"() {
    given: "i have a service account"
      def sa = new CreateServiceAccount()
        .name("sa-2").description("sa-1 test")
    and: "i know how many people there are"
      def personCount = numberOfPeople()
    when: "i save it"
      def account = sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    then:
      sapi.search(portfolio1Id, "sa-2", null, superPerson,
        Opts.empty()).find({it.name == 'sa-2' && sa.description == 'sa-1 test'})?.apiKeyClientSide != null
      sapi.search(portfolio1Id, "sa-2", null, superPerson,
        Opts.empty()).find({it.name == 'sa-2' && sa.description == 'sa-1 test'})?.apiKeyServerSide != null
    and: "there is 1 more person"
      numberOfPeople() - 1 == personCount
      findServiceAccount(account.id).sdkPerson.personType == PersonType.SDKSERVICEACCOUNT
  }

  def "i can reset the key for a service account"() {
    given: "i have a service account"
      def sa = new CreateServiceAccount().name("sa-reset").description("sa-1 test")
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
      def sa = new CreateServiceAccount().name("sa-delete").description("sa-1 test").permissions(
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
      def sa = new CreateServiceAccount().name("sa-dupe").description("sa-1 test")
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
      def env1 = environmentApi.create(new CreateEnvironment()
        .description('app-twosa-env1').name('app-twosa-env1'), app1.id, superPerson)
    and: "i have a service account"
      def sa1 = sapi.create(portfolio1Id, superPerson, new CreateServiceAccount().name("twosa-sa-1").description("sa-1 test").permissions(
        [new ServiceAccountPermission()
           .permissions([RoleType.READ, RoleType.CHANGE_VALUE])
           .environmentId(env1.id),
        ]
      ), Opts.empty())
    and: "i have another service account"
      def sa2 = sapi.create(portfolio1Id, superPerson, new CreateServiceAccount().name("twosa-sa-2").description("sa-2 test").permissions(
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
      def env1 = environmentApi.create(new CreateEnvironment().description('app-sa1-env1').name('app-sa1-env1'), app1.id, superPerson)
      def env2 = environmentApi.create(new CreateEnvironment().description('app-sa1-env2').name('app-sa1-env2'), app1.id, superPerson)
      def env3 = environmentApi.create(new CreateEnvironment().description('app-sa1-env3').name('app-sa1-env3'), app1.id, superPerson)
    and: "i have a service account with two environments"
      def sa = new CreateServiceAccount().name("sa-1").description("sa-1 test").permissions(
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
        .name(createdServiceAccount.name)
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

  @CompileStatic
  DbServiceAccount newServiceAccount(String newName) {
    def sa = new DbServiceAccount(dbSuperPerson, dbSuperPerson, newName, newName, "server-key-${newName}", "client-key-${newName}", portfolio1)
    sa.save()
    return sa
  }

  def "I can test the transition step that ensures that service accounts now have people attached"() {
    given: "i create 15 service accounts with no people attached"
      List<DbServiceAccount> accounts = []
      5.times { accounts.add(newServiceAccount("service-account-no-person-${it}"))}
      List<PersonType> foundSdkaccounts = accounts.collect { it.sdkPerson.personType }
//      db.currentTransaction()?.commit()
    when: "i trigger the migration process"
      sapi.ensure_service_accounts_have_person()
      def newFoundAccounts =       accounts.collect {
        it.refresh()
        it.sdkPerson.personType
      }
    then:
      1 * internalGroupSqlApi.superuserGroup(org.id) >> groupSqlApi.superuserGroup(org.id)
      foundSdkaccounts.size() == 5
      foundSdkaccounts.findAll { it == PersonType.PERSON }.size() == 5
      newFoundAccounts.findAll { it == PersonType.PERSON}.size() == 0
      newFoundAccounts.findAll { it == PersonType.SDKSERVICEACCOUNT }.size() == 5
  }
}
