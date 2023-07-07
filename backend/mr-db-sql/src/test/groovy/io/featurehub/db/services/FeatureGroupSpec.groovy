package io.featurehub.db.services

import io.featurehub.db.api.Opts
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureGroupCreate
import io.featurehub.mr.model.FeatureGroupStrategy
import io.featurehub.mr.model.FeatureGroupUpdate
import io.featurehub.mr.model.FeatureGroupUpdateFeature
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.SortOrder

class FeatureGroupSpec extends Base3Spec {
  FeatureGroupSqlApi fgApi

  def setup() {
    fgApi = new FeatureGroupSqlApi(convertUtils)
  }

  def "i can create a couple of groups and they have the right ordering and the listGroups comes back with them"() {
    given:
      def feature =
          applicationSqlApi.createApplicationFeature(app1.id,
            new Feature().name("sample_1").key("sample_1").valueType(FeatureValueType.BOOLEAN),
            superPerson, Opts.empty()).first()
    when:
      def created = fgApi.createGroup(app1.id, superPerson,
        new FeatureGroupCreate().name("name").environmentId(env1.id).features(
          [new FeatureGroupUpdateFeature().id(feature.id)]
      ))
    then:
      created.version == 1
      created.features[0].locked
      created.features[0].key == 'sample_1'
      !created.features[0].value
    when:
      def created2 = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name("name1").environmentId(env1.id).features(
        [new FeatureGroupUpdateFeature().id(feature.id)]).strategies([new FeatureGroupStrategy().percentage(20).name("fred")]))
    and:
      def all = fgApi.listGroups(app1.id, 20, null, 0, SortOrder.ASC, null)
    then:
      all.count == 2
      all.featureGroups.size() == 2
      all.featureGroups[1].order == 2
      all.featureGroups[0].id == created.id
      !all.featureGroups[0].hasStrategy
      all.featureGroups[1].id == created2.id
      all.featureGroups[1].name == created2.name
      all.featureGroups[1].hasStrategy
      !fgApi.getGroup(app1.id, superPerson, created2.id).features[0].value
      fgApi.getGroup(app1.id, superPerson, created2.id).strategies[0].name == "fred"
  }

  def "i can create a feature group and then update it"() {
    given: "i delete all existing features"
      applicationSqlApi.getApplicationFeatures(app1.id, Opts.empty()).each {
        applicationSqlApi.deleteApplicationFeature(app1.id, it.key)
      }
    and:
      def feature =
        applicationSqlApi.createApplicationFeature(app1.id,
          new Feature().name("sample_2").key("sample_2").valueType(FeatureValueType.BOOLEAN),
          superPerson, Opts.empty()).find { it.key == 'sample_2' }
      def feature2 =
        applicationSqlApi.createApplicationFeature(app1.id,
          new Feature().name("sample_3").key("sample_3").valueType(FeatureValueType.NUMBER),
          superPerson, Opts.empty()).find { it.key == 'sample_3' }
    when:
      def created = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name("name").environmentId(env1.id).features(
        [new FeatureGroupUpdateFeature().id(feature.id)]
      ))
    and:
      def getit = fgApi.getGroup(app1.id, superPerson, created.id)
    and:
      def featureValues = fgApi.getFeaturesForEnvironment(app1.id, env1.id)
    then:
      getit.id == created.id
      getit.name == "name"
      getit.features.size() == 1
      getit.features[0].id == feature.id
      !getit.features[0].value
      getit.features[0].key == "sample_2"
      featureValues.size() == 2
      with(featureValues.find({it.key == 'sample_2'})) {
        it.value == false
        it.locked == true
        it.id == feature.id
      }
      with(featureValues.find({it.key == 'sample_3'})) {
        it.value == null
        it.locked == false
        it.id == feature2.id
      }
    when:
      fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate()
        .version(created.version)
        .name("fred")
        .features([
        new FeatureGroupUpdateFeature().id(feature.id),
        new FeatureGroupUpdateFeature().id(feature2.id)]))
    and:
      getit = fgApi.getGroup(app1.id, superPerson, created.id)
    then:
      getit.name == "fred"
      getit.features.size() == 2
//      !getit.features[0].value
      getit.features[0].key == "sample_2"
//      getit.features[1].value == 123.67
      getit.features[1].key == "sample_3"
    when:
      def updated = fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate()
        .version(getit.version)
        .name("fred")
        .features([
          new FeatureGroupUpdateFeature().id(feature2.id)]))
    and:
      getit = fgApi.getGroup(app1.id, superPerson, created.id)
    then:
      getit.features.size() == 1
//      getit.features[0].value == 121.67
      getit.features[0].key == "sample_3"
    when: "i update the description"
      def updated2 = fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate().version(updated.version).description("hello"))
    then:
      updated2.version != updated.version
      updated2.description == 'hello'
      fgApi.getGroup(app1.id, superPerson, created.id).description == 'hello'
      fgApi.listGroups(app1.id, 20, null, 0, SortOrder.ASC, env1.id).featureGroups.size() > 1
      fgApi.listGroups(app1.id, 20, null, 0, SortOrder.ASC, UUID.randomUUID()).count == 0
  }
}
