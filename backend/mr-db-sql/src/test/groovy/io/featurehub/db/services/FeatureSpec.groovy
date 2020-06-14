package io.featurehub.db.services

import io.ebean.DB
import io.ebean.Database
import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.db.publish.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.ApplicationFeatureValues
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.EnvironmentGroupRole
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureEnvironment
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.ServiceAccount
import io.featurehub.mr.model.ServiceAccountPermission
import spock.lang.Shared
import spock.lang.Specification

class FeatureSpec extends Specification {
  @Shared Database database
  @Shared ConvertUtils convertUtils
  @Shared PersonSqlApi personSqlApi
  @Shared UUID superuser
  @Shared Person superPerson
  @Shared DbPerson dbSuperPerson
  @Shared DbPortfolio portfolio1
  @Shared DbApplication app1
  @Shared DbApplication app2
  @Shared ApplicationSqlApi appApi
  @Shared FeatureSqlApi featureSqlApi
  @Shared EnvironmentSqlApi environmentSqlApi
  @Shared GroupSqlApi groupSqlApi
  @Shared ServiceAccountSqlApi serviceAccountSqlApi
  @Shared String envIdApp1
  @Shared String appId
  @Shared String app2Id
  @Shared Person memberOfPortfolio1
  @Shared Person portfolioAdminOfPortfolio1
  @Shared Group groupInPortfolio1
  @Shared Group adminGroupInPortfolio1

  def setupSpec() {
    System.setProperty("ebean.ddl.generate", "true")
    System.setProperty("ebean.ddl.run", "true")
    database = DB.getDefault()
    convertUtils = new ConvertUtils(database)
    def archiveStrategy = new DbArchiveStrategy(database, convertUtils, Mock(CacheSource))
    personSqlApi = new PersonSqlApi(database, convertUtils, archiveStrategy)
    groupSqlApi = new GroupSqlApi(database, convertUtils, archiveStrategy)
    serviceAccountSqlApi = new ServiceAccountSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
    def organizationSqlApi = new OrganizationSqlApi(database, convertUtils)

    dbSuperPerson = Finder.findByEmail("irina@featurehub.io")
    if (dbSuperPerson == null) {
      dbSuperPerson = new DbPerson.Builder().email("irina@featurehub.io").name("Irina").build();
      database.save(dbSuperPerson);
    }
    database.save(dbSuperPerson);
    superuser = dbSuperPerson.getId()
    superPerson = convertUtils.toPerson(dbSuperPerson, Opts.empty())

    appApi = new ApplicationSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

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
    portfolio1 = new DbPortfolio.Builder().name("p1-app-feature").whoCreated(dbSuperPerson).organization(new QDbOrganization().findOne()).build()
    database.save(portfolio1)
    app1 = new DbApplication.Builder().whoCreated(dbSuperPerson).portfolio(portfolio1).name("feature-app-1").build()
    database.save(app1)
    appId = app1.id.toString()
    app2 = new DbApplication.Builder().whoCreated(dbSuperPerson).portfolio(portfolio1).name("feature-app-2").build()
    database.save(app2)
    app2Id = app2.id.toString()

    environmentSqlApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
    envIdApp1 = environmentSqlApi.create(new Environment().name("feature-app-1-env-1"), new Application().id(appId), superPerson).id

    featureSqlApi = new FeatureSqlApi(database, convertUtils, Mock(CacheSource))

    def averageJoe = new DbPerson.Builder().email("averagejoe-fvs@featurehub.io").name("Average Joe").build()
    database.save(averageJoe)
    memberOfPortfolio1 = convertUtils.toPerson(averageJoe)
    groupInPortfolio1 = groupSqlApi.createPortfolioGroup(portfolio1.id.toString(), new Group().name("fsspec-1-p1"), superPerson)
    groupSqlApi.addPersonToGroup(groupInPortfolio1.id, memberOfPortfolio1.id.id, Opts.empty())

    def portfolioAdmin = new DbPerson.Builder().email("pee-admin-fvs@featurehub.io").name("Portfolio Admin p1 fvs").build()
    database.save(portfolioAdmin)
    portfolioAdminOfPortfolio1 = convertUtils.toPerson(portfolioAdmin)

    adminGroupInPortfolio1 = groupSqlApi.createPortfolioGroup(portfolio1.id.toString(), new Group().admin(true).name("fsspec-admin-1-p1"), superPerson)
    groupSqlApi.addPersonToGroup(adminGroupInPortfolio1.id, portfolioAdminOfPortfolio1.id.id, Opts.empty())
  }

  def "when i save a new feature in an application i receive it back as part of the list"() {
    given: "i create a new feature"
      def features = appApi.createApplicationFeature(appId, new Feature().key("FEATURE_ONE").name("The neo feature"), superPerson)
    when:
      def foundFeatures = appApi.getApplicationFeatures(appId)
    then:
      features.find({ it -> it.key == 'FEATURE_ONE'}) != null
      foundFeatures.find({ it -> it.key == 'FEATURE_ONE'}) != null
  }

  def "i can't create an application feature with the same name in the same application"() {
    when:
      appApi.createApplicationFeature(appId, new Feature().key("FEATURE_TWO").name("The duo feature"), superPerson)
      appApi.createApplicationFeature(appId, new Feature().key("FEATURE_TWO").name("The duo feature"), superPerson)
    then:
      thrown ApplicationApi.DuplicateFeatureException
  }

  def "i can create the same named feature toggle in two different applications"() {
    when:
      def app1Features = appApi.createApplicationFeature(appId, new Feature().key("FEATURE_THREE"), superPerson)
      def app2Features = appApi.createApplicationFeature(app2Id, new Feature().key("FEATURE_THREE"), superPerson)
    then:
      app1Features.find({it -> it.key == 'FEATURE_THREE'}) != null
      app2Features.find({it -> it.key == 'FEATURE_THREE'}) != null
  }

  def "if i try and update without passing the version i am updating, i will get a optimistic locking exception"() {
    when:
      appApi.createApplicationFeature(appId, new Feature().key("FEATURE_UPD_LOCKX"), superPerson)
      appApi.updateApplicationFeature(appId, "FEATURE_UPD_LOCKX", new Feature().key("FEATURE_UPD_LOCKX"))
    then:
      thrown OptimisticLockingException
  }

  def "i can update an existing feature toggle to a new name"() {
    when:
      def app1Features = appApi.createApplicationFeature(appId, new Feature().key("FEATURE_UPD1"), superPerson)
      def feature = app1Features.find({it -> it.key == 'FEATURE_UPD1'}).copy()
      def updatedFeatures = appApi.updateApplicationFeature(appId, "FEATURE_UPD1",
        feature.name("Drunks trying to be Quiet").alias("ssssshhhh"))
    then:
      app1Features.find({it -> it.key == 'FEATURE_UPD1'}).alias == null
      updatedFeatures.find({it -> it.key == 'FEATURE_UPD1'}).alias == 'ssssshhhh'
      updatedFeatures.find({it -> it.key == 'FEATURE_UPD1'}).name == 'Drunks trying to be Quiet'
  }

  def "i cannot overwrite another feature with the same name when i update"() {
    given: "i have two features"
      appApi.createApplicationFeature(appId, new Feature().key("FEATURE_UPD2"), superPerson)
      Feature f2 = appApi.createApplicationFeature(appId, new Feature().key("FEATURE_UPD3"), superPerson).find({ it -> it.key == 'FEATURE_UPD3'})
    when: "i update the second to the same name as the first"
      appApi.updateApplicationFeature(appId, 'FEATURE_UPD3', f2.key('FEATURE_UPD2'))
    then:
      thrown ApplicationApi.DuplicateFeatureException
  }

  def "i cannot delete a non existent feature"() {
    when: "i try and delete a feature that doesn't exist"
      def result = appApi.deleteApplicationFeature(appId, 'FRED')
    then:
      result == null
  }

  def "i can delete an existing feature"() {
    given: "i have a feature"
      def features = appApi.createApplicationFeature(appId, new Feature().key("FEATURE_DELUROLO"), superPerson)
    when: "i delete it"
      def deletedList = appApi.deleteApplicationFeature(appId, 'FEATURE_DELUROLO')
      def getList = appApi.getApplicationFeatures(appId)
    then:
      features.find({it.key  == 'FEATURE_DELUROLO'}) != null
      deletedList == getList
      getList.find({it.key  == 'FEATURE_DELUROLO'}) == null

  }

  def "i can use basic crud for feature values for an application"() {
    given: "i have a feature"
      String k = "FEATURE_FV1"
      def features = appApi.createApplicationFeature(appId, new Feature().key(k).valueType(FeatureValueType.BOOLEAN), superPerson)
      def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
    when: "i set the feature value"
      def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, k);
      // it already exists, so we have  to unlock it
      f = featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, k, f.locked(false), pers)
      assert(!f.locked && !f.valueBoolean);
      def f2 = featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, k, f.valueBoolean(true).locked(true), pers)
      assert(f2.valueBoolean && f2.locked);
    and: "i get the FV"
      def fvEnv1 = featureSqlApi.getAllFeatureValuesForEnvironment(envIdApp1).featureValues
    and: "i update the feature value"
      def fv = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, k)
      fv.valueBoolean(false)
      fv.valueString("string val")
      fv.locked(false)
      featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, k, fv, pers)
      def fv2 = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, k)
    then:
      fvEnv1.find({it.key == "FEATURE_FV1"})
      fvEnv1.find({it.key == "FEATURE_FV1"}).valueBoolean
      fvEnv1.find({it.key == "FEATURE_FV1"}).locked
      !fv2.locked
      !fv2.valueBoolean
      fv2.valueString == null
  }

  def "if i only have unlock permission i cannot lock or change a feature value"() {
    given: "i have a feature"
      String k = "FEATURE_FV_UNLOCK1"
      def features = appApi.createApplicationFeature(appId, new Feature().key(k).valueType(FeatureValueType.BOOLEAN), superPerson)
      def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
    when: "i set the feature value"
      def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, k);
    // unlock it so we can change it in  the next step
      f = featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, k, f.locked(false), pers)
      featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, k, f.valueBoolean(true).locked(true), pers)
    and: "i update the feature value as unlock permission only"
      def fv = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, k)
      fv.valueBoolean(false)
      fv.locked(false)
      featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, k, fv, new PersonFeaturePermission(superPerson, [RoleType.UNLOCK] as Set<RoleType>))
      def fv2 = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, k)
    then:
      !fv2.locked
      fv2.valueBoolean // my permission was unlock only, so i can't change its value
  }

  def "if i only have unlock permission i get an exception if i try and lock"() {
    given: "i have a feature"
      String k = "FEATURE_FV_UNLOCK2"
      def features = appApi.createApplicationFeature(appId, new Feature().key(k).valueType(FeatureValueType.BOOLEAN), superPerson)
      def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
    when: "i set the feature value"
      def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, k);
      featureSqlApi.createFeatureValueForEnvironment(envIdApp1, k, f.valueBoolean(false).locked(false), pers)
    and: "i update the feature value as unlock permission only"
      def fv = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, k)
      fv.valueBoolean(false)
      fv.locked(true)
      featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, k, fv, new PersonFeaturePermission(superPerson, [RoleType.UNLOCK] as Set<RoleType>))
    then:
      thrown(FeatureApi.NoAppropriateRole)
  }


  def "i can block update a bunch of features for an environment"() {
    given: "i have a list of features"
      String[] names = ['FEATURE_FVU_1', 'FEATURE_FVU_2', 'FEATURE_FVU_3', 'FEATURE_FVU_4', 'FEATURE_FVU_5']
      names.each { k -> appApi.createApplicationFeature(appId, new Feature().key(k).valueType(FeatureValueType.BOOLEAN), superPerson) }
      def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
    when: "i set two of those values"
      def updatesForCreate = [featureSqlApi.getFeatureValueForEnvironment(envIdApp1, 'FEATURE_FVU_1').valueBoolean(true).locked(true),
                              featureSqlApi.getFeatureValueForEnvironment(envIdApp1, 'FEATURE_FVU_2').valueBoolean(true).locked(true)]
      featureSqlApi.updateAllFeatureValuesForEnvironment(envIdApp1, updatesForCreate, pers)
    and:
      List<FeatureValue> found = featureSqlApi.getAllFeatureValuesForEnvironment(envIdApp1).featureValues.findAll({ fv -> fv.key.startsWith('FEATURE_FVU')})
    and:
      def updating = new ArrayList<>(found.findAll({k -> k.key == 'FEATURE_FVU_1'}).collect({it.copy().locked(false).valueBoolean(false)}))
      updating.addAll([new FeatureValue().key('FEATURE_FVU_3').valueBoolean(true).locked(true),
                       new FeatureValue().key('FEATURE_FVU_4').valueBoolean(true).locked(true)])
      featureSqlApi.updateAllFeatureValuesForEnvironment(envIdApp1, updating, pers)
      def foundUpdating = featureSqlApi.getAllFeatureValuesForEnvironment(envIdApp1).featureValues.findAll({ fv -> fv.key.startsWith('FEATURE_FVU')})
    then:
      found.size() == 2
      foundUpdating.size() == 3
      !foundUpdating.find({fv -> fv.key == 'FEATURE_FVU_1'}).locked
      !foundUpdating.find({fv -> fv.key == 'FEATURE_FVU_1'}).valueBoolean
      foundUpdating.find({fv -> fv.key == 'FEATURE_FVU_3'}).valueBoolean
      foundUpdating.find({fv -> fv.key == 'FEATURE_FVU_4'}).valueBoolean
      foundUpdating.find({fv -> fv.key == 'FEATURE_FVU_4'}).locked
      foundUpdating.find({fv -> fv.key == 'FEATURE_FVU_3'}).locked
  }

  def "i can block update a bunch of feature values for an application"() {
    given: "i create 3 environments"
      def env1 = environmentSqlApi.create(new Environment().name("app2-dev-f1"), new Application().id(app2Id), superPerson)
      def env2 = environmentSqlApi.create(new Environment().name("app2-test-f1"), new Application().id(app2Id), superPerson)
      def env3 = environmentSqlApi.create(new Environment().name("app2-staging-f1"), new Application().id(app2Id), superPerson)
      def env4 = environmentSqlApi.create(new Environment().name("app2-production-f1"), new Application().id(app2Id), superPerson)
    and: "i create a service account"
      def serviceA1 = serviceAccountSqlApi.create(portfolio1.id.toString(), superPerson,
        new ServiceAccount()
          .description("the dragon").name("wilbur")
          .permissions([new ServiceAccountPermission().environmentId(env1.id).permissions([RoleType.READ])]),
        Opts.empty())
    and: "i allow superperson access to two of the three environments"
      Group g1 = groupSqlApi.createPortfolioGroup(portfolio1.id.toString(), new Group().name("app2-f1-test"), superPerson)
      g1.environmentRoles([
        new EnvironmentGroupRole().roles([RoleType.CHANGE_VALUE, RoleType.LOCK, RoleType.UNLOCK]).environmentId(env1.id),
        new EnvironmentGroupRole().roles([RoleType.CHANGE_VALUE, RoleType.LOCK]).environmentId(env3.id),
        new EnvironmentGroupRole().roles([RoleType.READ]).environmentId(env2.id)
      ])
      g1.members = [superPerson, memberOfPortfolio1]
      groupSqlApi.updateGroup(g1.id, g1, true, true, true, Opts.empty());
    and: "i create a feature value"
      String k = 'FEATURE_BUNCH'
      appApi.createApplicationFeature(app2Id, new Feature().key(k).valueType(FeatureValueType.BOOLEAN), superPerson)
      def perm = new PersonFeaturePermission.Builder().roles([RoleType.UNLOCK] as Set<RoleType>).person(superPerson).appRoles([] as Set<ApplicationRoleType>).build()
      [env1.id, env2.id, env3.id, env4.id].each { envId ->
        featureSqlApi.updateFeatureValueForEnvironment(envId, k,
          featureSqlApi.getFeatureValueForEnvironment(envId, k).locked(false),
          perm)
      }
    when: "i update the feature value"
      featureSqlApi.updateAllFeatureValuesByApplicationForKey(app2Id, k, [
        featureSqlApi.getFeatureValueForEnvironment(env1.id, k).valueBoolean(true).locked(true),
        featureSqlApi.getFeatureValueForEnvironment(env3.id, k).valueBoolean(null).locked(true),
      ], superPerson, true)

    and: "i ask for irina's api"
      ApplicationFeatureValues afv = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson)
      ApplicationFeatureValues afvAverageJoe = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, memberOfPortfolio1)
      ApplicationFeatureValues afvPortfolioAdminOfPortfolio1 = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, portfolioAdminOfPortfolio1)
    and:
      List<FeatureEnvironment> envs = featureSqlApi.getFeatureValuesForApplicationForKeyForPerson(app2Id, k, superPerson)
    and:
      featureSqlApi.updateAllFeatureValuesByApplicationForKey(app2Id, k, [envs.find({ e -> e.featureValue?.environmentId == env1.id }).featureValue.copy().locked(false)], superPerson, true)
    and:
      List<FeatureEnvironment> envs1 = featureSqlApi.getFeatureValuesForApplicationForKeyForPerson(app2Id, k, superPerson)
      List<FeatureEnvironment> envsAverageJoe = featureSqlApi.getFeatureValuesForApplicationForKeyForPerson(app2Id, k, memberOfPortfolio1)

        //
    then:
      envs.size() == 4
      envs.find({e -> e.environment.id == env1.id}).featureValue.locked
      envs.find({e -> e.environment.id == env1.id}).featureValue.valueBoolean
      envs.find({e -> e.environment.id == env1.id}).serviceAccounts[0].id == serviceA1.id
      envs.find({e -> e.environment.id == env2.id}).featureValue == null
      envs.find({e -> e.environment.id == env3.id}).featureValue.locked
      envs.find({e -> e.environment.id == env3.id}).featureValue.valueBoolean == false
      envs1.size() == 4
      envsAverageJoe.size() == 4
      envsAverageJoe.find({ FeatureEnvironment e -> e.environment.id == env4.id }).roles.size() == 0
      envsAverageJoe.find({ FeatureEnvironment e -> e.environment.id == env1.id }).roles.size() == 3
      envs1.find({e -> e.environment.id == env3.id}).featureValue == null
      !envs1.find({ e -> e.environment.id == env1.id }).featureValue.locked
      afv.features.find({it.key == k}).valueType == FeatureValueType.BOOLEAN
      afv.environments.size() == 4
      afv.environments.find({it.environmentName == 'app2-dev-f1'}).roles == [RoleType.CHANGE_VALUE, RoleType.LOCK, RoleType.UNLOCK]
      afv.environments.find({it.environmentName == 'app2-dev-f1'}).features[0].locked
      afv.environments.find({it.environmentName == 'app2-staging-f1'}).features[0].locked
      afv.environments.find({it.environmentName == 'app2-staging-f1'}).roles == [RoleType.CHANGE_VALUE, RoleType.LOCK]
      afv.environments.find({it.environmentName == 'app2-test-f1'}).roles == [RoleType.READ]
      afv.environments.find({it.environmentName == 'app2-test-f1'}).features.size() == 0
      afv.environments.find({it.environmentName == 'app2-production-f1'}).features.size() == 1
      afv.environments.find({it.environmentName == 'app2-production-f1'}).roles == Arrays.asList(RoleType.values()) // because superuser, otherwise would have no access
      afvAverageJoe.environments.size() == 4
      afvAverageJoe.environments.find({it.environmentName == 'app2-production-f1'}).roles.size() == 0
    // portfolio admin can read in every environment
      afvPortfolioAdminOfPortfolio1.environments.size() == 4
      afvPortfolioAdminOfPortfolio1.environments.roles.each { it -> assert it == Arrays.asList(RoleType.values()) }
      afvPortfolioAdminOfPortfolio1.environments.find({it.environmentName == 'app2-dev-f1'}).features[0].locked
  }
}
