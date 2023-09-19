package io.featurehub.db.services

import groovy.transform.CompileStatic
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbFeatureGroup
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.ApplicationPermissions
import io.featurehub.mr.model.EnvironmentPermission
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureGroupCreate
import io.featurehub.mr.model.FeatureGroupStrategy
import io.featurehub.mr.model.FeatureGroupUpdate
import io.featurehub.mr.model.FeatureGroupUpdateFeature
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.mr.model.SortOrder
import org.apache.commons.lang3.RandomStringUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class FeatureGroupSpec extends Base3Spec {
  FeatureGroupSqlApi fgApi
  ApplicationPermissions permsToEnv1
  CacheSource cacheSource

  def setup() {
    cacheSource = Mock()
    fgApi = new FeatureGroupSqlApi(convertUtils, cacheSource, new InternalFeatureSqlApi(), archiveStrategy)
    permsToEnv1 = new ApplicationPermissions().environments([new EnvironmentPermission().id(env1.id)])
  }


  @CompileStatic
  @Nullable DbFeatureValue fv(UUID envId, UUID featureId) {
    return new QDbFeatureValue().environment.id.eq(envId).feature.id.eq(featureId).findOne()
  }

  @CompileStatic
  @NotNull Feature createFeature(FeatureValueType type = FeatureValueType.BOOLEAN) {
    def key = RandomStringUtils.randomAlphabetic(10)

    return applicationSqlApi.createApplicationFeature(app1.id,
      new Feature().name(key).key(key).valueType(type),
      superPerson, Opts.empty()).find { it.key == key }
  }

  @NotNull FeatureGroupStrategy sally() {
    return new FeatureGroupStrategy().name("sally").attributes([
      new RolloutStrategyAttribute().conditional(RolloutStrategyAttributeConditional.EQUALS)
        .fieldName("name").values(["mary"]).type(RolloutStrategyFieldType.STRING)
    ])
  }

  def "i expect that the archive strategy got a listener attached"() {
    given: "i have a mock archive strategy"
      def myArchive = Mock(ArchiveStrategy)
    when: "when i create the group sql api"
      fgApi = new FeatureGroupSqlApi(convertUtils, cacheSource, new InternalFeatureSqlApi(), myArchive)
    then: "the it self registers with the archive strategy"
      1 * myArchive.environmentArchiveListener(_)
  }

  def "i can create a couple of groups and they have the right ordering and the listGroups comes back with them"() {
    given:
      def feature =  createFeature()
    and: "i have permissions to the environment"
    when:
      def created = fgApi.createGroup(app1.id, superPerson,
        new FeatureGroupCreate().name("name").environmentId(env1.id).features(
          [new FeatureGroupUpdateFeature().id(feature.id)]
      ))
    then:
      created.version == 1
      created.features[0].locked
      created.features[0].key == feature.key
      !created.features[0].value
    when:
      def created2 = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name("name1").environmentId(env1.id).features(
        [new FeatureGroupUpdateFeature().id(feature.id)]).strategies([new FeatureGroupStrategy().percentage(20).name("fred")]))
    and:
      def all = fgApi.listGroups(app1.id, 20, null, 0, SortOrder.ASC, null, permsToEnv1)
    then:
      all.count == 2
      all.featureGroups.size() == 2
      all.featureGroups[1].order > all.featureGroups[0].order
      all.featureGroups[0].id == created.id
      !all.featureGroups[0].hasStrategy
      all.featureGroups[1].id == created2.id
      all.featureGroups[1].name == created2.name
      all.featureGroups[1].hasStrategy
      !fgApi.getGroup(app1.id, created2.id).features[0].value
      fgApi.getGroup(app1.id, created2.id).strategies[0].name == "fred"
      1 * cacheSource.publishFeatureChange(fv(env1.id, feature.id))
  }

  def "i can create a feature group and then update it"() {
    given: "i delete all existing features"
      applicationSqlApi.getApplicationFeatures(app1.id, Opts.empty()).each {
        applicationSqlApi.deleteApplicationFeature(app1.id, it.key)
      }
    and:
      def feature = createFeature()
      def feature2 = createFeature(FeatureValueType.NUMBER)
    when:
      def created = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name("name").environmentId(env1.id).features(
        [new FeatureGroupUpdateFeature().id(feature.id)]
      ))
    and:
      def getit = fgApi.getGroup(app1.id, created.id)
    and:
      def featureValues = fgApi.getFeaturesForEnvironment(app1.id, env1.id)
    then:
      getit.id == created.id
      getit.name == "name"
      getit.features.size() == 1
      getit.features[0].id == feature.id
      !getit.features[0].value
      getit.features[0].key == feature.key
      getit.features[0].name == feature.name
      featureValues.size() == 2
      with(featureValues.find({it.key == feature.key})) {
//        it.value == false
        it.locked == true
        it.id == feature.id
      }
      with(featureValues.find({it.key == feature2.key})) {
//        it.value == null
        it.locked == false
        it.id == feature2.id
      }
    when:
      fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate()
        .version(created.version)
        .name("fred")
        .features([
        new FeatureGroupUpdateFeature().id(feature.id),
        new FeatureGroupUpdateFeature().id(feature2.id).value(123.67)]))
    and:
      getit = fgApi.getGroup(app1.id, created.id)
      def f1 = getit.features.find({it.id == feature.id})
      def f2 = getit.features.find({it.id == feature2.id})
    then:
      getit.name == "fred"
      getit.features.size() == 2
      !f1.value
      f1.key == feature.key
      f2.value == 123.67
      f2.key == feature2.key
      f2.name == feature2.name
    when:
      def updated = fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate()
        .version(getit.version)
        .name("fred")
        .features([
          new FeatureGroupUpdateFeature().id(feature2.id).value(121.67)]))
    and:
      getit = fgApi.getGroup(app1.id, created.id)
    then:
      getit.features.size() == 1
      getit.features[0].value == 121.67
      getit.features[0].key == feature2.key
    when: "i update the description"
      def updated2 = fgApi.updateGroup(app1.id, superPerson, created.id,
        new FeatureGroupUpdate().version(updated.version).description("hello").strategies([
          new FeatureGroupStrategy().percentage(20).name("fred")
        ]))
    then:
      updated2.version != updated.version
      updated2.description == 'hello'
      fgApi.getGroup(app1.id, created.id).description == 'hello'
      fgApi.listGroups(app1.id, 20, null, 0, SortOrder.ASC, env1.id, permsToEnv1).featureGroups.find({it.name == "fred"})
      fgApi.listGroups(app1.id, 20, null, 0, SortOrder.ASC, UUID.randomUUID(), permsToEnv1).count == 0
      1 * cacheSource.publishFeatureChange(fv(env1.id, feature2.id))
      updated2.strategies[0].id.startsWith("!")
      updated2.strategies[0].id.length() == 4
  }

  def "modifications to a feature group cause expected published features"() {
    given: "i create 2 features"
      List<Feature> features = []
      (1..2).each {
        features.add(createFeature(FeatureValueType.NUMBER))
      }
    and: "i have a feature group with a strategy"
      def created = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name(RandomStringUtils.randomAlphabetic(10))
        .environmentId(env1.id).strategies([new FeatureGroupStrategy().percentage(20).name("fred")])
      )
    and: "i have the cache source reader for feature groups"
      def fgSource = new CacheSourceFeatureGroupSqlApi()
    when: "i update the group adding one "
      def updated = fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate()
        .version(created.version)
        .features([
          new FeatureGroupUpdateFeature().id(features[0].id).value(121.67)]))
      def strategies1 = fgSource.collectStrategiesFromGroupsForEnvironmentFeature(env1.id, features[0].id)
    then:
      1 * cacheSource.publishFeatureChange( { DbFeatureValue f ->
        f.environment.id == env1.id && f.feature.id == features[0].id
      })
      strategies1.size() == 1
      strategies1[0].value == 121.67
      strategies1[0].name == 'fred'
      strategies1[0].percentage == 20
      strategies1[0].attributes.size() == 0
    when: "i remove the first feature and add a second feature"
      def updatedAddedRemoved = fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate()
        .version(updated.version)
        .features([
          new FeatureGroupUpdateFeature().id(features[1].id).value(12.2)]))
      def strategiesEnv = fgSource.collectStrategiesFromGroupsForEnvironment(env1.id)
    then:
      1 * cacheSource.publishFeatureChange({ DbFeatureValue f ->
        f.feature.id == features[1].id || f.feature.id == features[0].id
      })
      1 * cacheSource.publishFeatureChange({ DbFeatureValue f ->
        f.feature.id == features[1].id || f.feature.id == features[0].id
      })
      strategiesEnv[features[1].id]
      strategiesEnv[features[1].id].size() == 1
      strategiesEnv[features[1].id][0].value == 12.2
      strategiesEnv[features[1].id][0].percentage == 20
    when: "i update an existing value, the feature is published"
      def updatedFeature = fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate()
        .version(updatedAddedRemoved.version)
        .features([
          new FeatureGroupUpdateFeature().id(features[1].id).value(167.88)]))
    then:
      1 * cacheSource.publishFeatureChange({ DbFeatureValue f ->
        f.feature.id == features[1].id
      })
    when: "i update the strategy, the feature publishes"
      def updatedStrategy = fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate()
        .version(updatedFeature.version)
        .strategies([new FeatureGroupStrategy().percentage(25).name("fred")]))
      strategiesEnv = fgSource.collectStrategiesFromGroupsForEnvironment(env1.id)
    then:
      1 * cacheSource.publishFeatureChange({ DbFeatureValue f ->
        f.feature.id == features[1].id
      })
      strategiesEnv[features[1].id][0].percentage == 25
  }

  def "two groups, same features in each but with different values and strategies, creating"() {
    given: "i create 2 features"
      def key = RandomStringUtils.randomAlphabetic(10)
      def feature1 =
        applicationSqlApi.createApplicationFeature(app1.id,
          new Feature().name(key).key(key).valueType(FeatureValueType.NUMBER),
          superPerson, Opts.empty()).find { it.key == key }
      key = RandomStringUtils.randomAlphabetic(10)
      def feature2 =
        applicationSqlApi.createApplicationFeature(app1.id,
          new Feature().name(key).key(key).valueType(FeatureValueType.BOOLEAN),
          superPerson, Opts.empty()).find { it.key == key }
    when: "i have a feature group with a percentage strategy"
      def group1 = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name(RandomStringUtils.randomAlphabetic(10))
        .environmentId(env1.id).strategies([new FeatureGroupStrategy().percentage(20).name("fred")])
        .features([
          new FeatureGroupUpdateFeature().value(6).id(feature1.id),
          new FeatureGroupUpdateFeature().value(false).id(feature2.id)
        ])
      )
    then: "both of the features get republished"
      1 * cacheSource.publishFeatureChange({ DbFeatureValue fv -> fv.feature.id == feature1.id })
      1 * cacheSource.publishFeatureChange({ DbFeatureValue fv -> fv.feature.id == feature2.id })
    when: "i have a feature group with an attribute strategy"
      def group2 = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name(RandomStringUtils.randomAlphabetic(10))
        .environmentId(env1.id)
        .strategies([sally()])
        .features([
          new FeatureGroupUpdateFeature().value(12).id(feature1.id),
          new FeatureGroupUpdateFeature().value(true).id(feature2.id)
        ])
      )
    then: "both of the features get republished"
      1 * cacheSource.publishFeatureChange({ DbFeatureValue fv -> fv.feature.id == feature1.id })
      1 * cacheSource.publishFeatureChange({ DbFeatureValue fv -> fv.feature.id == feature2.id })
    when: "i set the second group to have an order BEFORE the first group"
      group2 = fgApi.updateGroup(app1.id, superPerson, group2.id, new FeatureGroupUpdate().version(group2.version).order(group1.order-1))
    and: "i have the cache source reader for feature groups"
      def fgSource = new CacheSourceFeatureGroupSqlApi()
    and: "i ask for environment strategies"
      def envStrategies = fgSource.collectStrategiesFromGroupsForEnvironment(env1.id)
      def f1Strategies = envStrategies[feature1.id]
      def f2Strategies = envStrategies[feature2.id]
    then: "the strategies allocated to both features should have two strategies in them"
      f2Strategies
      f1Strategies
      f1Strategies.size() == 2
      f2Strategies.size() == 2
    and: "the second group should have the first strategy"
      f1Strategies[0].id == group2.strategies[0].id
      f1Strategies[1].id == group1.strategies[0].id
      f2Strategies[0].id == group2.strategies[0].id
      f2Strategies[1].id == group1.strategies[0].id
    and: "and feature1 has numeric values correct to the stategy and ordering"
      f1Strategies[0].value == 12
      f1Strategies[1].value == 6
    and: "and feature2 has numeric values correct to the stategy and ordering"
      f2Strategies[0].value == true
      f2Strategies[1].value == false
    when: "i delete the second group"
      fgApi.deleteGroup(app1.id, superPerson, group2.id)
    then: "both of the features get republished"
      1 * cacheSource.publishFeatureChange({ DbFeatureValue fv -> fv.feature.id == feature1.id })
      1 * cacheSource.publishFeatureChange({ DbFeatureValue fv -> fv.feature.id == feature2.id })
  }

  def "no publishing when no strategy"() {
    given: "i create have a feature"
      def feature = createFeature()
    when: "i create a feature group without a strategy"
      def group2 = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name(RandomStringUtils.randomAlphabetic(10))
        .environmentId(env1.id)
        .features([
          new FeatureGroupUpdateFeature().value(12).id(feature.id),
        ])
      )
    then: "nothing publishes"
      0 * cacheSource.publishFeatureChange(_)
    when: "i delete that group"
      fgApi.deleteGroup(app1.id, superPerson, group2.id)
    then: "nothing publishes and the group is deleted"
      0 * cacheSource.publishFeatureChange(_)
      0 * fgApi.getGroup(app1.id, superPerson, group2.id)
  }

  @CompileStatic
  int featureGroupCount(UUID envId) {
    return new QDbFeatureGroup().select(QDbFeatureGroup.Alias.id).whenArchived.isNull().environment.id.eq(envId).findCount()
  }

  @CompileStatic
  DbEnvironment env(UUID envId) {
    return new QDbEnvironment().id.eq(envId).whenArchived.isNull().findOne()
  }

  def "when i get a request from the archiving of envrironments, it removes all feature groups"() {
    given: "i create a feature group without a strategy"
      def group = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name(RandomStringUtils.randomAlphabetic(10))
        .environmentId(env1.id)
      )
    when: "i count feature groups"
      def count = featureGroupCount(env1.id)
    then: "there should be at least one"
      count > 0
    when: "i archive the environment"
      fgApi.archiveEnvironment(env(env1.id))
    then: "there are no feature groups for this environment"
      featureGroupCount(env1.id) == 0
  }

  def "when i delete a feature, it is removed from the feature groups"() {
    given: "i have a feature"
      def feature = createFeature()
    and: "i create a couple of groups with that feature"
      def group1 = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name(RandomStringUtils.randomAlphabetic(10))
        .environmentId(env1.id)
        .features([new FeatureGroupUpdateFeature().value(true).id(feature.id),])
      )
      def group2 = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name(RandomStringUtils.randomAlphabetic(10))
        .environmentId(env1.id)
        .features([new FeatureGroupUpdateFeature().value(true).id(feature.id),])
      )
    when: "i get the two groups"
      def getGroup1 = fgApi.getGroup(app1.id, group1.id)
      def getGroup2 = fgApi.getGroup(app1.id, group2.id)
    then:
      getGroup1.features[0].id == feature.id
      getGroup2.features[0].id == feature.id
    when: "i delete the feature"
      applicationSqlApi.deleteApplicationFeature(app1.id, feature.key)
    and: "refresh the two groups"
      getGroup1 = fgApi.getGroup(app1.id, group1.id)
      getGroup2 = fgApi.getGroup(app1.id, group2.id)
    then:
      getGroup1.features.size() == 0
      getGroup2.features.size() == 0
  }
}
