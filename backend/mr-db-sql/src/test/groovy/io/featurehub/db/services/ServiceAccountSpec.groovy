package io.featurehub.db.services

import io.ebean.DB
import io.ebean.Database
import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.db.publish.CacheSource
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.ServiceAccount
import io.featurehub.mr.model.ServiceAccountPermission
import io.featurehub.mr.model.ServiceAccountPermissionType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification

class ServiceAccountSpec extends Specification {
  private static final Logger log = LoggerFactory.getLogger(ServiceAccountSpec.class)
  @Shared Database database
  @Shared ConvertUtils convertUtils
  @Shared PersonSqlApi personSqlApi
  @Shared UUID superuser
  @Shared Person superPerson
  @Shared ServiceAccountSqlApi sapi
  @Shared DbPerson dbSuperPerson
  @Shared DbPortfolio portfolio1
  @Shared DbApplication application1
  @Shared DbEnvironment environment1
  @Shared DbEnvironment environment2
  @Shared DbEnvironment environment3
  @Shared String portfolio1Id
  @Shared EnvironmentApi environmentApi

  def setupSpec() {
    System.setProperty("ebean.ddl.generate", "true")
    System.setProperty("ebean.ddl.run", "true")
    database = DB.getDefault()
    convertUtils = new ConvertUtils(database)
    def archiveStrategy = new DbArchiveStrategy(database, convertUtils, Mock(CacheSource))
    personSqlApi = new PersonSqlApi(database, convertUtils, archiveStrategy)
    def groupSqlApi = new GroupSqlApi(database, convertUtils, archiveStrategy)
    def organizationSqlApi = new OrganizationSqlApi(database, convertUtils)

    dbSuperPerson = Finder.findByEmail("irina@featurehub.io")
    if (dbSuperPerson == null) {
      dbSuperPerson = new DbPerson.Builder().email("irina@featurehub.io").name("Irina").build();
      database.save(dbSuperPerson);
    }

    superuser = dbSuperPerson.getId()
    superPerson = convertUtils.toPerson(dbSuperPerson, Opts.empty())

    sapi = new ServiceAccountSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    // ensure the org is created and we have an admin user in an admin group
    Organization org = organizationSqlApi.get()
    Group adminGroup
    if (org == null) {
      org = organizationSqlApi.save(new Organization())
      adminGroup = groupSqlApi.createOrgAdminGroup(org.id, 'admin group', superPerson)
    } else {
      adminGroup = groupSqlApi.findOrganizationAdminGroup(org.id, Opts.empty())
    }
    groupSqlApi.addPersonToGroup(adminGroup.id, superuser.toString(), Opts.empty())

    // now set up the environments we need
    UUID orgUUID = ConvertUtils.ifUuid(org.id)
    DbOrganization organization = new QDbOrganization().findList().stream().filter(o -> o.id == orgUUID).findFirst().get()
    portfolio1 = new DbPortfolio.Builder().name("p1-env-1").whoCreated(dbSuperPerson).organization(organization).build()
    database.save(portfolio1)
    portfolio1Id = portfolio1.id.toString()
    application1 = new DbApplication.Builder().name("app-env-1").portfolio(portfolio1).whoCreated(dbSuperPerson).build()
    database.save(application1)
    environment1 = new DbEnvironment.Builder().whoCreated(dbSuperPerson).name("e1").parentApplication(application1).build();
    database.save(environment1)
    environment2 = new DbEnvironment.Builder().whoCreated(dbSuperPerson).name("e2").parentApplication(application1).build();
    database.save(environment2)
    environment3 = new DbEnvironment.Builder().whoCreated(dbSuperPerson).name("e3").parentApplication(application1).build();
    database.save(environment3)

    environmentApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
  }

  def "i can create a service account with no environments"() {
    given: "i have a service account"
      def sa = new ServiceAccount().name("sa-2").description("sa-1 test")
    when: "i save it"
      sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    then:
      sapi.search(portfolio1Id, "sa-2", null, Opts.empty()).find({it.name == 'sa-2' && sa.description == 'sa-1 test'}).apiKey != null
  }

  def "i can reset the key for a service account"() {
    given: "i have a service account"
      def sa = new ServiceAccount().name("sa-reset").description("sa-1 test")
    when: "i save it"
      def created = sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    and: "i reset the key"
      sapi.resetApiKey(created.id)
    then:
      created.apiKey != null
      sapi.search(portfolio1Id, "sa-reset", null, Opts.empty()).find({it.name == 'sa-reset' && sa.description == 'sa-1 test'}).apiKey != created.apiKey
  }

  def "i can create then delete a service account"() {
    given: "i have a service account"
      def sa = new ServiceAccount().name("sa-delete").description("sa-1 test").permissions(
        [new ServiceAccountPermission()
           .permissions([ServiceAccountPermissionType.READ, ServiceAccountPermissionType.TOGGLE_ENABLED])
           .environmentId(environment1.id.toString()),
        ]
      )
    and: "i create it"
      def created = sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    when: "i delete it"
      def result = sapi.delete(superPerson, created.id)
    and: "try and find it"
      def search = sapi.search(portfolio1Id, 'sa-delete', null, Opts.empty())
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

  def "i can create a service account with permissions in two environments, then update it to remove one environment and change permission on other"() {
    given: "i have a service account with two environments"
      def sa = new ServiceAccount().name("sa-1").description("sa-1 test").permissions(
        [new ServiceAccountPermission()
           .permissions([ServiceAccountPermissionType.READ, ServiceAccountPermissionType.TOGGLE_ENABLED])
          .environmentId(environment1.id.toString()),
          new ServiceAccountPermission()
          .permissions([ServiceAccountPermissionType.TOGGLE_LOCK])
          .environmentId(environment2.id.toString())
        ]
      )
    when: "i save it"
      sapi.create(portfolio1Id, superPerson, sa, Opts.empty())
    and: "i get the result"
      def result = sapi.search(portfolio1Id, "sa-1", null, Opts.opts(FillOpts.Permissions)).find({it.name == 'sa-1' && sa.description == 'sa-1 test'})
    and: "i re-get the two environments with extra data"
      def newEnv1 = environmentApi.get(environment1.id.toString(), Opts.opts(FillOpts.ServiceAccounts, FillOpts.SdkURL), superPerson)
      def newEnv2 = environmentApi.get(environment2.id.toString(), Opts.opts(FillOpts.ServiceAccounts, FillOpts.SdkURL), superPerson)
      log.info("env 1 sdk url is {}", newEnv1.serviceAccountPermission.first().sdkUrl)
    and: "i check the result for the two environments"
      def permE1 = result.permissions.find({it.environmentId == environment1.id.toString()})
      def permE2 = result.permissions.find({it.environmentId == environment2.id.toString()})
    and: "then update the service account to remove environment 2 and remove toggle-enabled"
      def updated = result.copy()
      updated.description("sa-2 test")
      updated.permissions.remove(updated.permissions.find({it.environmentId == environment2.id.toString()}))
      updated.permissions[0].permissions = [ServiceAccountPermissionType.TOGGLE_ENABLED]
      updated.permissions.add(new ServiceAccountPermission()
        .environmentId(environment3.id.toString())
        .permissions([ServiceAccountPermissionType.TOGGLE_LOCK]))
      sapi.update(updated.id, superPerson, updated, Opts.empty())
    and: "search for the result"
      def updatedResult = sapi.search(portfolio1Id, "sa-1", null, Opts.opts(FillOpts.Permissions)).find({it.name == 'sa-1'})
      def upd1 = updatedResult.permissions.find({it.environmentId == environment1.id.toString()})
      def upd2 = updatedResult.permissions.find({it.environmentId == environment3.id.toString()})
    then:
      result.permissions.size() == 2
      permE1.permissions.intersect([ServiceAccountPermissionType.READ, ServiceAccountPermissionType.TOGGLE_ENABLED]).size() == 2
      permE1.environmentId == environment1.id.toString()
      permE2.permissions == [ServiceAccountPermissionType.TOGGLE_LOCK]
      permE2.environmentId == environment2.id.toString()
      upd1 != null
      upd2 != null
      upd1.permissions == [ServiceAccountPermissionType.TOGGLE_ENABLED]
      upd2.permissions == [ServiceAccountPermissionType.TOGGLE_LOCK]
      updatedResult.equals(sapi.get(updatedResult.id, Opts.opts(FillOpts.Permissions)))
      updatedResult.description == 'sa-2 test'
      newEnv1.serviceAccountPermission.size() == 1
      newEnv2.serviceAccountPermission.size() == 1
      newEnv1.serviceAccountPermission.first().getServiceAccount().getName() == 'sa-1'
      newEnv2.serviceAccountPermission.first().getServiceAccount().getName() == 'sa-1'
      newEnv1.serviceAccountPermission.first().permissions.containsAll([ServiceAccountPermissionType.READ, ServiceAccountPermissionType.TOGGLE_ENABLED])
      newEnv2.serviceAccountPermission.first().permissions.containsAll([ServiceAccountPermissionType.TOGGLE_LOCK])
      newEnv1.serviceAccountPermission.first().sdkUrl.contains("/" + newEnv1.serviceAccountPermission.first().serviceAccount.apiKey)
  }

  def "I cannot request or update an unknown service account"() {
    when:
      def x = sapi.update("x", superPerson, new ServiceAccount(), Opts.empty())
      def y = sapi.get("p", Opts.empty())
    then:
      x == null
      y == null
  }
}
