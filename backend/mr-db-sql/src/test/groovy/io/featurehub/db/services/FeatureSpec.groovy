package io.featurehub.db.services


import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.db.publish.CacheSource
import io.featurehub.db.services.strategies.StrategyDiffer
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
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.mr.model.ServiceAccount
import io.featurehub.mr.model.ServiceAccountPermission
import spock.lang.Shared

class FeatureSpec extends BaseSpec {
  @Shared PersonSqlApi personSqlApi
  @Shared DbPortfolio portfolio1
  @Shared DbApplication app1
  @Shared DbApplication app2
  @Shared ApplicationSqlApi appApi
  @Shared FeatureSqlApi featureSqlApi
  @Shared EnvironmentSqlApi environmentSqlApi
  @Shared ServiceAccountSqlApi serviceAccountSqlApi
  @Shared UUID envIdApp1
  @Shared UUID appId
  @Shared UUID app2Id
  @Shared Person averageJoeMemberOfPortfolio1
  @Shared Person portfolioAdminOfPortfolio1
  @Shared Group groupInPortfolio1
  @Shared Group adminGroupInPortfolio1

  def setupSpec() {
    baseSetupSpec()

    personSqlApi = new PersonSqlApi(database, convertUtils, archiveStrategy)
    serviceAccountSqlApi = new ServiceAccountSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)

    appApi = new ApplicationSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)


    // now set up the environments we need
    portfolio1 = new DbPortfolio.Builder().name("p1-app-feature").whoCreated(dbSuperPerson).organization(new QDbOrganization().findOne()).build()
    database.save(portfolio1)
    app1 = new DbApplication.Builder().whoCreated(dbSuperPerson).portfolio(portfolio1).name("feature-app-1").build()
    database.save(app1)
    appId = app1.id
    app2 = new DbApplication.Builder().whoCreated(dbSuperPerson).portfolio(portfolio1).name("feature-app-2").build()
    database.save(app2)
    app2Id = app2.id

    environmentSqlApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy)
    envIdApp1 = environmentSqlApi.create(new Environment().name("feature-app-1-env-1"), new Application().id(appId), superPerson).id

    def rsv = Mock(RolloutStrategyValidator)
    rsv.validateStrategies(_, _) >> new RolloutStrategyValidator.ValidationFailure()

    featureSqlApi = new FeatureSqlApi(database, convertUtils, Mock(CacheSource), rsv, Mock(StrategyDiffer))

    def averageJoe = new DbPerson.Builder().email("averagejoe-fvs@featurehub.io").name("Average Joe").build()
    database.save(averageJoe)
    averageJoeMemberOfPortfolio1 = convertUtils.toPerson(averageJoe)
    groupInPortfolio1 = groupSqlApi.createPortfolioGroup(portfolio1.id, new Group().name("fsspec-1-p1"), superPerson)
    groupSqlApi.addPersonToGroup(groupInPortfolio1.id, averageJoeMemberOfPortfolio1.id.id, Opts.empty())

    def portfolioAdmin = new DbPerson.Builder().email("pee-admin-fvs@featurehub.io").name("Portfolio Admin p1 fvs").build()
    database.save(portfolioAdmin)
    portfolioAdminOfPortfolio1 = convertUtils.toPerson(portfolioAdmin)

    adminGroupInPortfolio1 = groupSqlApi.createPortfolioGroup(portfolio1.id, new Group().admin(true).name("fsspec-admin-1-p1"), superPerson)
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

  def 'boolean features cannot be removed when a update all feature values for environment happens'() {
    given: "i have a list of features"
      String[] names = ['FEATURE_FBU_1', 'FEATURE_FBU_2', 'FEATURE_FBU_3', 'FEATURE_FBU_4', 'FEATURE_FBU_5']
      names.each { k -> appApi.createApplicationFeature(appId, new Feature().key(k).valueType(FeatureValueType.BOOLEAN), superPerson) }
      def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
    when: "i get all the features"
      List<FeatureValue> found = featureSqlApi.getAllFeatureValuesForEnvironment(envIdApp1).featureValues.findAll({ fv -> fv.key.startsWith('FEATURE_FBU')})
    and: "remove update none"
      List<FeatureValue> remaining = featureSqlApi.updateAllFeatureValuesForEnvironment(envIdApp1, [], pers)
    then:
      found.findAll({ fv -> fv.key.startsWith('FEATURE_FBU')}).size() == 5
      remaining.findAll({ fv -> fv.key.startsWith('FEATURE_FBU')}).size() == 5
  }

  def "i can block update a bunch of features for an environment"() {
    given: "i have a list of features"
      String[] names = ['FEATURE_FVU_1', 'FEATURE_FVU_2', 'FEATURE_FVU_3', 'FEATURE_FVU_4', 'FEATURE_FVU_5']
      names.each { k -> appApi.createApplicationFeature(appId, new Feature().key(k).valueType(FeatureValueType.STRING), superPerson) }
      def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
    when: "i set two of those values"
      def updatesForCreate = [new FeatureValue().key('FEATURE_FVU_1').valueString('h').locked(true),
                              new FeatureValue().key( 'FEATURE_FVU_2').valueString('h').locked(true)]
      featureSqlApi.updateAllFeatureValuesForEnvironment(envIdApp1, updatesForCreate, pers)
    and:
      List<FeatureValue> found = featureSqlApi.getAllFeatureValuesForEnvironment(envIdApp1).featureValues.findAll({ fv -> fv.key.startsWith('FEATURE_FVU')})
    and:
      def updating = new ArrayList<>(found.findAll({k -> k.key == 'FEATURE_FVU_1'}).collect({it.copy().locked(false).valueString('z')}))
//      updating.add(found.find({it.key == 'FEATURE_FVU_3'}).valueBoolean(true).locked(true))
//      updating.add(found.find({it.key == 'FEATURE_FVU_4'}).valueBoolean(true).locked(true))
      updating.addAll([new FeatureValue().key('FEATURE_FVU_3').valueString('h').locked(true),
                       new FeatureValue().key('FEATURE_FVU_4').valueString('h').locked(true)])
      featureSqlApi.updateAllFeatureValuesForEnvironment(envIdApp1, updating, pers)
      def foundUpdating = featureSqlApi.getAllFeatureValuesForEnvironment(envIdApp1).featureValues.findAll({ fv -> fv.key.startsWith('FEATURE_FVU')})
    then:
      found.size() == 2
      foundUpdating.size() == 3
      !foundUpdating.find({fv -> fv.key == 'FEATURE_FVU_1'}).locked
      foundUpdating.find({fv -> fv.key == 'FEATURE_FVU_1'}).valueString == 'z'
      foundUpdating.find({fv -> fv.key == 'FEATURE_FVU_3'}).valueString == 'h'
      foundUpdating.find({fv -> fv.key == 'FEATURE_FVU_4'}).valueString == 'h'
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
      def serviceA1 = serviceAccountSqlApi.create(portfolio1.id, superPerson,
        new ServiceAccount()
          .description("the dragon").name("wilbur")
          .permissions([new ServiceAccountPermission().environmentId(env1.id).permissions([RoleType.READ])]),
        Opts.empty())
    and: "i allow average joe access to two of the three environments"
      Group g1 = groupSqlApi.createPortfolioGroup(portfolio1.id, new Group().name("app2-f1-test"), superPerson)
      g1.environmentRoles([
        new EnvironmentGroupRole().roles([RoleType.CHANGE_VALUE, RoleType.LOCK, RoleType.UNLOCK]).environmentId(env1.id),
        new EnvironmentGroupRole().roles([RoleType.CHANGE_VALUE, RoleType.LOCK]).environmentId(env3.id),
        new EnvironmentGroupRole().roles([RoleType.READ]).environmentId(env2.id)
      ])
      g1.members = [averageJoeMemberOfPortfolio1]
      groupSqlApi.updateGroup(g1.id, g1, true, true, true, Opts.empty());
    and: "i create a feature value called FEATURE_BUNCH and unlock the feature in all branches"
      String k = 'FEATURE_BUNCH'
      appApi.createApplicationFeature(app2Id, new Feature().key(k).valueType(FeatureValueType.BOOLEAN), superPerson)
      def perm = new PersonFeaturePermission.Builder().roles([RoleType.UNLOCK] as Set<RoleType>).person(superPerson).appRoles([] as Set<ApplicationRoleType>).build()
      [env1.id, env2.id, env3.id, env4.id].each { envId ->
        featureSqlApi.updateFeatureValueForEnvironment(envId, k,
          featureSqlApi.getFeatureValueForEnvironment(envId, k).locked(false),
          perm)
      }
    when: "i update the feature value in environments 1 and 3 using average joe and then relock them"
      featureSqlApi.updateAllFeatureValuesByApplicationForKey(app2Id, k, [
        featureSqlApi.getFeatureValueForEnvironment(env1.id, k).valueBoolean(true).locked(true),
        featureSqlApi.getFeatureValueForEnvironment(env3.id, k).valueBoolean(null).locked(true),
      ], averageJoeMemberOfPortfolio1, true)

    and: "i ask for irina's api"
      ApplicationFeatureValues afv = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson)
      ApplicationFeatureValues afvAverageJoe = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, averageJoeMemberOfPortfolio1)
      ApplicationFeatureValues afvPortfolioAdminOfPortfolio1 = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, portfolioAdminOfPortfolio1)
    and:
      List<FeatureEnvironment> envs = featureSqlApi.getFeatureValuesForApplicationForKeyForPerson(app2Id, k, superPerson)
    and:
      featureSqlApi.updateAllFeatureValuesByApplicationForKey(app2Id, k, [envs.find({ e -> e.featureValue?.environmentId == env1.id }).featureValue.copy().locked(false)], superPerson, true)
    and:
      List<FeatureEnvironment> envs1 = featureSqlApi.getFeatureValuesForApplicationForKeyForPerson(app2Id, k, superPerson)
      List<FeatureEnvironment> envsAverageJoe = featureSqlApi.getFeatureValuesForApplicationForKeyForPerson(app2Id, k, averageJoeMemberOfPortfolio1)

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
      afv.environments.find({it.environmentName == 'app2-dev-f1'}).roles == Arrays.asList(RoleType.values())
      afv.environments.find({it.environmentName == 'app2-dev-f1'}).features[0].locked
      afv.environments.find({it.environmentName == 'app2-staging-f1'}).features[0].locked
      afv.environments.find({it.environmentName == 'app2-staging-f1'}).roles == Arrays.asList(RoleType.values())
      afv.environments.find({it.environmentName == 'app2-test-f1'}).roles == Arrays.asList(RoleType.values())
      afv.environments.find({it.environmentName == 'app2-test-f1'}).features.size() == 0
      afv.environments.find({it.environmentName == 'app2-production-f1'}).features.size() == 1
      afv.environments.find({it.environmentName == 'app2-production-f1'}).roles == Arrays.asList(RoleType.values()) // because superuser, otherwise would have no access
      afvAverageJoe.environments.size() == 4
      afvAverageJoe.environments.find({it.environmentName == 'app2-dev-f1'}).roles == [RoleType.CHANGE_VALUE, RoleType.LOCK, RoleType.UNLOCK]
      afvAverageJoe.environments.find({it.environmentName == 'app2-production-f1'}).roles.isEmpty()
    // portfolio admin can read in every environment
      afvPortfolioAdminOfPortfolio1.environments.size() == 4
      afvPortfolioAdminOfPortfolio1.environments.roles.each { it -> assert it == Arrays.asList(RoleType.values()) }
      afvPortfolioAdminOfPortfolio1.environments.find({it.environmentName == 'app2-dev-f1'}).features[0].locked
  }


  def "updates to custom rollout strategies are persisted as expected"() {
    given: "i create an environment (in app2)"
      def env1 = environmentSqlApi.create(new Environment().name("rstrat-test-env1"), new Application().id(app2Id), superPerson)
    and: "i have a boolean feature (which will automatically create a feature value in each environment)"
      def key = 'FEATURE_MISINTERPRET'
      appApi.createApplicationFeature(app2Id, new Feature().key(key).valueType(FeatureValueType.BOOLEAN), superPerson)
    when: "i update the fv with the custom strategy"
      def fv = featureSqlApi.getFeatureValueForEnvironment(env1.id, key)
      def strat = new RolloutStrategy().name('freddy').percentage(20).percentageAttributes(['company'])
        .value(Boolean.FALSE).attributes([
          new RolloutStrategyAttribute()
              .values(['ios'])
              .fieldName('platform')
              .conditional(RolloutStrategyAttributeConditional.EQUALS)
              .type(RolloutStrategyFieldType.STRING)
        ])
      fv.locked(false)
      fv.rolloutStrategies([strat])
      def perms = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
      def updated = featureSqlApi.updateFeatureValueForEnvironment(env1.id, key, fv, perms)
    and:
      def stored = featureSqlApi.getFeatureValueForEnvironment(env1.id, key)
    then:
      stored.rolloutStrategies.size() == 1
      updated.rolloutStrategies.size() == 1
      stored.rolloutStrategies[0] == strat
  }

  def "if a feature is locked the custom strategies will not update"() {
    given: "i create an environment (in app2)"
      def env1 = environmentSqlApi.create(new Environment().name("rstrat-test-env2"), new Application().id(app2Id), superPerson)
    and: "i have a boolean feature (which will automatically create a feature value in each environment)"
      def key = 'FEATURE_NOT_WHEN_LOCKED'
      appApi.createApplicationFeature(app2Id, new Feature().key(key).valueType(FeatureValueType.BOOLEAN), superPerson)
    when: "i update the fv with the custom strategy"
      def fv = featureSqlApi.getFeatureValueForEnvironment(env1.id, key)
      def strat = new RolloutStrategy().name('freddy').percentage(20).percentageAttributes(['company'])
        .value(Boolean.FALSE).attributes([
        new RolloutStrategyAttribute()
          .values(['ios'])
          .fieldName('platform')
          .conditional(RolloutStrategyAttributeConditional.EQUALS)
          .type(RolloutStrategyFieldType.STRING)
      ])
      fv.locked(true)
      fv.rolloutStrategies([strat])
      def perms = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
      def updated = featureSqlApi.updateFeatureValueForEnvironment(env1.id, key, fv, perms)
    and:
      def stored = featureSqlApi.getFeatureValueForEnvironment(env1.id, key)
    then:
      stored.rolloutStrategies == null
  }

  def "a plain user in a group that has lock/unlock/read but not change is able to lock and unlock"() {
    given: "i have an environment"
      def env1 = environmentSqlApi.create(new Environment().name("lock-unlock-env"), new Application().id(app2Id), superPerson)
    and: "i have a string feature (which will automatically create a feature value in each environment) - which has no feature value created"
      def key = 'FEATURE_LOCK_UNLOCK_NOT_CHANGE'
      appApi.createApplicationFeature(app2Id, new Feature().key(key).valueType(FeatureValueType.STRING), superPerson)
    and: "i have a person"
      def person = personSqlApi.createPerson("lockunlock@mailinator.com", "Locky McLockface", "password123", superuser, Opts.empty())
    and: "a group in the current portfolio"
      def group = groupSqlApi.createPortfolioGroup(portfolio1.id, new Group().name("lockunlock group"), superPerson)
    and: "i add permissions and members to the group"
      group.environmentRoles([new EnvironmentGroupRole().environmentId(env1.id).roles([RoleType.LOCK, RoleType.UNLOCK, RoleType.READ])])
      group.members([person])
      group = groupSqlApi.updateGroup(group.id, group, true, false, true, Opts.empty())
    when: "i try and unlock the feature with the person, it will let me"
      def fv = featureSqlApi.getFeatureValueForEnvironment(env1.id, key)
      if (fv == null) {
        fv = new FeatureValue().environmentId(env1.id).key(key)
      }
      featureSqlApi.updateAllFeatureValuesByApplicationForKey(app2Id, key, [fv.locked(false).valueString("sausage")], person, false)
      fv = featureSqlApi.getFeatureValueForEnvironment(env1.id, key)
    then:
      !fv.locked
      fv.valueString == null
  }
}
