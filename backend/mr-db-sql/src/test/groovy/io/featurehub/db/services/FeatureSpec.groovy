package io.featurehub.db.services

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.mr.events.common.CacheSource
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
import org.apache.commons.lang3.RandomStringUtils
import io.featurehub.mr.model.SortOrder

class FeatureSpec extends Base2Spec {
  PersonSqlApi personSqlApi
  DbPortfolio portfolio1
  DbApplication app1
  DbApplication app2
  ApplicationSqlApi appApi
  FeatureSqlApi featureSqlApi
  EnvironmentSqlApi environmentSqlApi
  ServiceAccountSqlApi serviceAccountSqlApi
  UUID envIdApp1
  UUID appId
  UUID app2Id
  Person averageIrinaNotPortfolioMember
  Person averageJoeMemberOfPortfolio1
  Person portfolioAdminOfPortfolio1
  Group groupInPortfolio1
  Group adminGroupInPortfolio1
  RolloutStrategyValidator rsv

  def setup() {
    db.commitTransaction()
    personSqlApi = new PersonSqlApi(db, convertUtils, archiveStrategy, Mock(InternalGroupSqlApi))
    serviceAccountSqlApi = new ServiceAccountSqlApi(db, convertUtils, Mock(CacheSource), archiveStrategy)

    rsv = Mock(RolloutStrategyValidator)
    rsv.validateStrategies(_, _, _) >> new RolloutStrategyValidator.ValidationFailure()

    //  these ones generally assume auditing will be off
    ThreadLocalConfigurationSource.createContext(['auditing.enable': 'false'])

    featureSqlApi = new FeatureSqlApi(db, convertUtils, Mock(CacheSource), rsv)
    appApi = new ApplicationSqlApi(db, convertUtils, Mock(CacheSource), archiveStrategy, featureSqlApi)

    // now set up the environments we need
    portfolio1 = new DbPortfolio.Builder().name("p1-app-feature" + RandomStringUtils.randomAlphabetic(8) ).whoCreated(dbSuperPerson).organization(new QDbOrganization().findOne()).build()
    db.save(portfolio1)
    app1 = new DbApplication.Builder().whoCreated(dbSuperPerson).portfolio(portfolio1).name("feature-app-1").build()
    db.save(app1)
    appId = app1.id
    app2 = new DbApplication.Builder().whoCreated(dbSuperPerson).portfolio(portfolio1).name("feature-app-2").build()
    db.save(app2)
    app2Id = app2.id

    environmentSqlApi = new EnvironmentSqlApi(db, convertUtils, Mock(CacheSource), archiveStrategy)
    envIdApp1 = environmentSqlApi.create(new Environment().name("feature-app-1-env-1"), new Application().id(appId), superPerson).id

    def averageJoe = new DbPerson.Builder().email(RandomStringUtils.randomAlphabetic(8) + "averagejoe-fvs@featurehub.io").name("Average Joe").build()
    db.save(averageJoe)
    averageJoeMemberOfPortfolio1 = convertUtils.toPerson(averageJoe)
    groupInPortfolio1 = groupSqlApi.createGroup(portfolio1.id, new Group().name("fsspec-1-p1"), superPerson)
    groupSqlApi.addPersonToGroup(groupInPortfolio1.id, averageJoeMemberOfPortfolio1.id.id, Opts.empty())

    def averageIrina = new DbPerson.Builder().email(RandomStringUtils.randomAlphabetic(8) +"averageirina@featurehub.io").name("Average Irina").build()
    db.save(averageIrina)
    averageIrinaNotPortfolioMember = convertUtils.toPerson(averageIrina)

    def portfolioAdmin = new DbPerson.Builder().email(RandomStringUtils.randomAlphabetic(8) +"pee-admin-fvs@featurehub.io").name("Portfolio Admin p1 fvs").build()
    db.save(portfolioAdmin)
    portfolioAdminOfPortfolio1 = convertUtils.toPerson(portfolioAdmin)

    adminGroupInPortfolio1 = groupSqlApi.createGroup(portfolio1.id, new Group().admin(true).name("fsspec-admin-1-p1"), superPerson)
    groupSqlApi.addPersonToGroup(adminGroupInPortfolio1.id, portfolioAdminOfPortfolio1.id.id, Opts.empty())

    if (db.currentTransaction() != null && db.currentTransaction().active) {
      db.commitTransaction()
    }
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "when i save a new feature in an application i receive it back as part of the list"() {
    given: "i create a new feature"
      def features = appApi.createApplicationFeature(appId, new Feature().key("FEATURE_ONE").name("The neo feature"), superPerson, Opts.empty())
    when:
      def foundFeatures = appApi.getApplicationFeatures(appId, Opts.empty())
    then:
      features.find({ it -> it.key == 'FEATURE_ONE'}) != null
      foundFeatures.find({ it -> it.key == 'FEATURE_ONE'}) != null
  }

  def "i can't create an application feature with the same name in the same application"() {
    when:
      appApi.createApplicationFeature(appId, new Feature().key("FEATURE_TWO").name("The duo feature"), superPerson, Opts.empty())
      appApi.createApplicationFeature(appId, new Feature().key("FEATURE_TWO").name("The duo feature"), superPerson, Opts.empty())
    then:
      thrown ApplicationApi.DuplicateFeatureException
  }

  def "i can create the same named feature toggle in two different applications"() {
    when:
      def app1Features = appApi.createApplicationFeature(appId, new Feature().name("x").key("FEATURE_THREE"), superPerson, Opts.empty())
      def app2Features = appApi.createApplicationFeature(app2Id, new Feature().name("y").key("FEATURE_THREE"), superPerson, Opts.empty())
    then:
      app1Features.find({it -> it.key == 'FEATURE_THREE'}) != null
      app2Features.find({it -> it.key == 'FEATURE_THREE'}) != null
  }

  def "if i try and update without passing the version i am updating, i will get a optimistic locking exception"() {
    when:
      appApi.createApplicationFeature(appId, new Feature().name('x').key("FEATURE_UPD_LOCKX"), superPerson, Opts.empty())
      appApi.updateApplicationFeature(appId, "FEATURE_UPD_LOCKX", new Feature().key("FEATURE_UPD_LOCKX"), Opts.empty())
    then:
      thrown OptimisticLockingException
  }

  def "i can update an existing feature toggle to a new name"() {
    when:
      def app1Features = appApi.createApplicationFeature(appId, new Feature().name("x").key("FEATURE_UPD1"), superPerson, Opts.empty())
      def feature = app1Features.find({it -> it.key == 'FEATURE_UPD1'}).copy()
      def updatedFeatures = appApi.updateApplicationFeature(appId, "FEATURE_UPD1",
        feature.name("Drunks trying to be Quiet").alias("ssssshhhh"), Opts.empty())
    then:
      app1Features.find({it -> it.key == 'FEATURE_UPD1'}).alias == null
      updatedFeatures.find({it -> it.key == 'FEATURE_UPD1'}).alias == 'ssssshhhh'
      updatedFeatures.find({it -> it.key == 'FEATURE_UPD1'}).name == 'Drunks trying to be Quiet'
  }


  def "i cannot delete a non existent feature"() {
    when: "i try and delete a feature that doesn't exist"
      def result = appApi.deleteApplicationFeature(appId, 'FRED')
    then:
      result == null
  }

  def "i can delete an existing feature"() {
    given: "i have a feature"
      def features = appApi.createApplicationFeature(appId, new Feature().name("x").key("FEATURE_DELUROLO"), superPerson, Opts.empty())
    when: "i delete it"
      def deletedList = appApi.deleteApplicationFeature(appId, 'FEATURE_DELUROLO')
      def getList = appApi.getApplicationFeatures(appId, Opts.empty())
    then:
      features.find({it.key  == 'FEATURE_DELUROLO'}) != null
      deletedList == getList
      getList.find({it.key  == 'FEATURE_DELUROLO'}) == null
  }

  def "i can set and retrieve meta-data on a feature"() {
    when: "i create the feature with meta-data"
      def features = appApi.createApplicationFeature(appId, new Feature().name("m-people").key("m-people").metaData("yaml:"), superPerson,
          Opts.opts(FillOpts.MetaData))
    then:
      features[0].metaData == 'yaml:'
      appApi.getApplicationFeatures(appId, Opts.opts(FillOpts.MetaData))[0].metaData == "yaml:"
  }

  def "i can set and retrieve a description on a feature"() {
    when: "i create the feature with meta-data"
      def features = appApi.createApplicationFeature(appId, new Feature().name("m-people").key("m-people").description("the voice"), superPerson, Opts.empty())
    then:
      features[0].description == 'the voice'
      appApi.getApplicationFeatures(appId, Opts.empty())[0].description == 'the voice'
  }

  def "if a description on a feature is null, i can update it"() {
    when: "i create the feature without description"
      def features = appApi.createApplicationFeature(appId, new Feature().name("m-people").key("m-people"), superPerson, Opts.empty())
    then:
      appApi.updateApplicationFeature(appId, features[0].key, features[0].description("new desc"), Opts.empty())[0].description == 'new desc'
  }

  def "if a metadata on a feature is null, i can update it"() {
    when: "i create the feature without meta-data"
      def features = appApi.createApplicationFeature(appId, new Feature().name("m-people").key("m-people"), superPerson, Opts.empty())
    then:
      appApi.updateApplicationFeature(appId, features[0].key, features[0].metaData("new data"), Opts.opts(FillOpts.MetaData))[0].metaData == 'new data'
  }

  def "i can use basic crud for feature values for an application"() {
    given: "i have a feature"
      String k = "FEATURE_FV1"
      def features = appApi.createApplicationFeature(appId, new Feature().name("x").key(k).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)
    when: "i set the feature value"
      def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, k)
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
      def features = appApi.createApplicationFeature(appId, new Feature().name("x").key(k).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
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
      def features = appApi.createApplicationFeature(appId, new Feature().name("x").key(k).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
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
      names.each { k -> appApi.createApplicationFeature(appId, new Feature().name(k).key(k).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty()) }
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
      names.each { k -> appApi.createApplicationFeature(appId, new Feature().name(k).key(k).valueType(FeatureValueType.STRING), superPerson, Opts.empty()) }
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

  def "as admin i can filter what features I want for an application"() {
    given: "i create 1 environment"
      def env1 = environmentSqlApi.create(new Environment().name("production"), new Application().id(app2Id), superPerson)
    and: "i create some features"
      appApi.createApplicationFeature(app2Id, new Feature().name('Some description').key('FEATURE_IRINA').description('Some description').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      appApi.createApplicationFeature(app2Id, new Feature().name('Some description').key('FEATURE_RICHARD').description('Some description').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      appApi.createApplicationFeature(app2Id, new Feature().name('not desc').key('FEATURE_VIDYA').description('not desc').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      appApi.createApplicationFeature(app2Id, new Feature().name('not').key('FEATURE_ALEX').description('not').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    when: "i request application features for 'desc'"
      ApplicationFeatureValues withDesc = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, 'desc', null, null, null, null)
    and: 'application features with null'
      ApplicationFeatureValues withNull = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, null, null, null, null, null)
    and: 'with not'
      ApplicationFeatureValues withNot = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, 'not', null, null, null, null)
    and: 'with description'
      ApplicationFeatureValues withDescription = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, 'description', null, null, null, null)
    and: 'with IRINA'
      ApplicationFeatureValues withIrina = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, 'IRINA', null, null, null, null)
    then:
      withDesc.features.size() == 3
      withDesc.environments[0].features.size() == 3
      withNull.features.size() == 4
      withNull.environments[0].features.size() == 4
      withNot.features.size() == 2
      withNot.environments[0].features.size() == 2
      withDescription.features.size() == 2
      withDescription.environments[0].features.size() == 2
      withIrina.features.size() == 1
      withIrina.environments[0].features.size() == 1
  }

  // the features themselves are irrespective of access, a superuser and a user with access gets the same structure of keys
  def "i should be able to paginate through the results"() {
    given: "i create 1 environment"
      def env1 = environmentSqlApi.create(new Environment().name("production"), new Application().id(app2Id), superPerson)
    and: "i create some features"
      appApi.createApplicationFeature(app2Id, new Feature().name('FEATURE_IRINA').key('FEATURE_IRINA').description('Some description').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      appApi.createApplicationFeature(app2Id, new Feature().name('FEATURE_RICHARD').key('FEATURE_RICHARD').description('description2').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      appApi.createApplicationFeature(app2Id, new Feature().name('FEATURE_VIDYA').key('FEATURE_VIDYA').description('not desc').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      appApi.createApplicationFeature(app2Id, new Feature().name('FEATURE_ALEX').key('FEATURE_ALEX').description('not').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    when: "i request pages of 2"
      def pageOne = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, null,
          1, 0, null, null)
      def pageTwo = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, null,
        1, 1, null, null)
      def pageOneReverse = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, null,
        1, 0, null, SortOrder.DESC)
    and: "i request only number features"
      def onlyNumber = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, null,
        null, null, [FeatureValueType.NUMBER], null)
    and: "i request number and boolean features"
      def numAndBool = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, null,
        null, null, [FeatureValueType.NUMBER, FeatureValueType.BOOLEAN], null)
    then:
      pageOne.maxFeatures == 4
      pageOne.features.size() == 1
      pageOne.features[0].name == 'FEATURE_ALEX'
      pageTwo.maxFeatures == 4
      pageTwo.features.size() == 1
      pageTwo.features[0].name == 'FEATURE_IRINA'
      pageOneReverse.maxFeatures == 4
      pageOneReverse.features.size() == 1
      pageOneReverse.features[0].name == 'FEATURE_VIDYA'
      onlyNumber.maxFeatures == 0
      onlyNumber.features.size() == 0
      numAndBool.maxFeatures == 4
      numAndBool.features.size() == 4
  }

  def "as a user who has no access to the portfolio, i cannot see any features"() {
    given: "i create 1 environment"
      def env1 = environmentSqlApi.create(new Environment().name("production"), new Application().id(app2Id), superPerson)
    and: "i create some features"
      appApi.createApplicationFeature(app2Id, new Feature().name('FEATURE_IRINA').key('FEATURE_IRINA').description('Some description').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    when: 'application features with null'
      ApplicationFeatureValues withNull = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, averageIrinaNotPortfolioMember, null, null, null, null, null)
    then:
      withNull == null
  }

  def "as a user who has access to a group but not this application, i cannot see any features"() {
    given: "i create 1 environment"
      def env1 = environmentSqlApi.create(new Environment().name("production"), new Application().id(app2Id), superPerson)
    and: "i create some features"
      appApi.createApplicationFeature(app2Id, new Feature().name('FEATURE_IRINA').key('FEATURE_IRINA').description('Some description').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    when: 'application features with null filter'
      ApplicationFeatureValues withNull = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, averageJoeMemberOfPortfolio1, null, null, null, null, null)
    then:
      withNull == null
  }

  def "i have access to the environment but there are no features, so i should see that"() {
    given: "i create 1 environment"
      def env1 = environmentSqlApi.create(new Environment().name("production"), new Application().id(app2Id), superPerson)
    and: "i allow average joe access to access the environment"
      Group g1 = groupSqlApi.createGroup(portfolio1.id, new Group().name("app2-f1-test"), superPerson)
      g1.environmentRoles([
        new EnvironmentGroupRole().roles([RoleType.CHANGE_VALUE, RoleType.LOCK, RoleType.UNLOCK]).environmentId(env1.id),
      ])
      g1.members = [averageJoeMemberOfPortfolio1]
      groupSqlApi.updateGroup(g1.id, g1, null, true, true, true, Opts.empty());
    when: 'application features with null filter'
      ApplicationFeatureValues withNull = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, averageJoeMemberOfPortfolio1, null, null, null, null, null)
    then:
      withNull.environments.size() == 1
      withNull.environments[0].features.isEmpty()
      withNull.environments[0].priorEnvironmentId == null
      withNull.environments[0].roles.size() == 3
      withNull.features.isEmpty()
  }

  def "as a group user in an application, i can filter for features i want in an application"() {
    given: "i create 1 environment"
      def env1 = environmentSqlApi.create(new Environment().name("production"), new Application().id(app2Id), superPerson)
    and: "i create some features"
      appApi.createApplicationFeature(app2Id, new Feature().name('Some description').key('FEATURE_IRINA').description('Some description').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      appApi.createApplicationFeature(app2Id, new Feature().name('description2').key('FEATURE_RICHARD').description('description2').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      appApi.createApplicationFeature(app2Id, new Feature().name('not desc').key('FEATURE_VIDYA').description('not desc').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      appApi.createApplicationFeature(app2Id, new Feature().name('not').key('FEATURE_ALEX').description('not').valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    and: "i allow average joe access to access the environment"
      Group g1 = groupSqlApi.createGroup(portfolio1.id, new Group().name("app2-f1-test"), superPerson)
      g1.environmentRoles([
        new EnvironmentGroupRole().roles([RoleType.CHANGE_VALUE, RoleType.LOCK, RoleType.UNLOCK]).environmentId(env1.id),
      ])
      g1.members = [averageJoeMemberOfPortfolio1]
      groupSqlApi.updateGroup(g1.id, g1, null, true, true, true, Opts.empty());
    when: "i request application features for 'desc'"
      ApplicationFeatureValues withDesc = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, averageJoeMemberOfPortfolio1, 'desc', null, null, null, null)
    and: 'application features with null'
      ApplicationFeatureValues withNull = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, averageJoeMemberOfPortfolio1, null, null, null, null, null)
    and: 'with not'
      ApplicationFeatureValues withNot = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, averageJoeMemberOfPortfolio1, 'not', null, null, null, null)
    and: 'with description'
      ApplicationFeatureValues withDescription = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, averageJoeMemberOfPortfolio1, 'description', null, null, null, null)
    and: 'with IRINA'
      ApplicationFeatureValues withIrina = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, averageJoeMemberOfPortfolio1, 'IRINA', null, null, null, null)
    then:
      withDesc.features.size() == 3
      withDesc.environments[0].features.size() == 3
      withNull.features.size() == 4
      withNull.environments[0].features.size() == 4
      withNot.features.size() == 2
      withNot.environments[0].features.size() == 2
      withDescription.features.size() == 2
      withDescription.environments[0].features.size() == 2
      withIrina.features.size() == 1
      withIrina.environments[0].features.size() == 1
  }

  def "i can block update a bunch of feature values for an application"() {
    given: "i create 4 environments"
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
      Group g1 = groupSqlApi.createGroup(portfolio1.id, new Group().name("app2-f1-test"), superPerson)
      g1.environmentRoles([
        new EnvironmentGroupRole().roles([RoleType.CHANGE_VALUE, RoleType.LOCK, RoleType.UNLOCK]).environmentId(env1.id),
        new EnvironmentGroupRole().roles([RoleType.CHANGE_VALUE, RoleType.LOCK]).environmentId(env3.id),
        new EnvironmentGroupRole().roles([RoleType.READ]).environmentId(env2.id)
      ])
      g1.members = [averageJoeMemberOfPortfolio1]
      groupSqlApi.updateGroup(g1.id, g1, null, true, true, true, Opts.empty());
    and: "i create a feature value called FEATURE_BUNCH and unlock the feature in all branches"
      String k = 'FEATURE_BUNCH'
      appApi.createApplicationFeature(app2Id, new Feature().name(k).key(k).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
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
      ApplicationFeatureValues afv = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, superPerson, null, null, null, null, null)
      ApplicationFeatureValues afvAverageJoe = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, averageJoeMemberOfPortfolio1, null, null, null, null, null)
      ApplicationFeatureValues afvPortfolioAdminOfPortfolio1 = featureSqlApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app2Id, portfolioAdminOfPortfolio1, null, null, null, null, null)
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
      !afvAverageJoe.environments.find({it.environmentName == 'app2-dev-f1'}).roles.disjoint([RoleType.CHANGE_VALUE, RoleType.LOCK, RoleType.UNLOCK])
      afvAverageJoe.environments.find({it.environmentName == 'app2-production-f1'}).roles.isEmpty()
    // portfolio admin can read in every environment
      afvPortfolioAdminOfPortfolio1.environments.size() == 4
      afvPortfolioAdminOfPortfolio1.environments.roles.each { it -> assert it == Arrays.asList(RoleType.values()) }
      afvPortfolioAdminOfPortfolio1.environments.find({it.environmentName == 'app2-dev-f1'}).features[0].locked
  }


  def "updates to custom rollout strategies are persisted as expected"() {
    setup:
      ThreadLocalConfigurationSource.createContext(['auditing.enable': 'true'])
      featureSqlApi = new FeatureSqlApi(db, convertUtils, Mock(CacheSource), rsv)
    when: "i update the fv with the custom strategy"
      def env1 = environmentSqlApi.create(new Environment().name("rstrat-test-env1"), new Application().id(app2Id), superPerson)
      def key = 'FEATURE_MISINTERPRET'
      appApi.createApplicationFeature(app2Id, new Feature().name(key).key(key).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
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
    and:
      def deleted = appApi.deleteApplicationFeature(app2Id, key)
    then:
      stored.rolloutStrategies.size() == 1
      updated.rolloutStrategies.size() == 1
      stored.rolloutStrategies[0] == strat
    cleanup:
      ThreadLocalConfigurationSource.clearContext()
  }

  def "if a feature is locked the custom strategies will not update"() {
    given: "i create an environment (in app2)"
      ThreadLocalConfigurationSource.createContext(['auditing.enable': 'true'])
      def env1 = environmentSqlApi.create(new Environment().name("rstrat-test-env2"), new Application().id(app2Id), superPerson)
    and: "i have a boolean feature (which will automatically create a feature value in each environment)"
      def key = 'FEATURE_NOT_WHEN_LOCKED'
      appApi.createApplicationFeature(app2Id, new Feature().name(key).key(key).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
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
      stored.rolloutStrategies.isEmpty()
  }

  def "a plain user in a group that has lock/unlock/read but not change is able to lock and unlock"() {
    given: "i have an environment"
      def env1 = environmentSqlApi.create(new Environment().name("lock-unlock-env"), new Application().id(app2Id), superPerson)
    and: "i have a string feature (which will automatically create a feature value in each environment) - which has no feature value created"
      def key = 'FEATURE_LOCK_UNLOCK_NOT_CHANGE'
      appApi.createApplicationFeature(app2Id, new Feature().name(key).key(key).valueType(FeatureValueType.STRING), superPerson, Opts.empty())
    and: "i have a person"
      def person = personSqlApi.createPerson("lockunlock@mailinator.com", "Locky McLockface", "password123", superuser, Opts.empty())
    and: "a group in the current portfolio"
      def group = groupSqlApi.createGroup(portfolio1.id, new Group().name("lockunlock group"), superPerson)
    and: "i add permissions and members to the group"
      group.environmentRoles([new EnvironmentGroupRole().environmentId(env1.id).roles([RoleType.LOCK, RoleType.UNLOCK, RoleType.READ])])
      group.members([person])
      group = groupSqlApi.updateGroup(group.id, group, null, true, false, true, Opts.empty())
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
