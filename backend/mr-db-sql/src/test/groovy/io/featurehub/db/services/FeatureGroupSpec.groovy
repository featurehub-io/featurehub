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
      def created = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name("name").environmentId(env1.id).features(
          [new FeatureGroupUpdateFeature().id(feature.id).value(true)]
      ))
    then:
      created.version == 1
    when:
      def created2 = fgApi.createGroup(app1.id, superPerson, new FeatureGroupCreate().name("name1").environmentId(env1.id).features(
        [new FeatureGroupUpdateFeature().id(feature.id)]).strategy(new FeatureGroupStrategy().percentage(20)))
    and:
      def all = fgApi.listGroups(app1.id, 20, null, 0, SortOrder.ASC)
    then:
      all.count == 2
      all.featureGroups.size() == 2
      all.featureGroups[1].order == 2
      all.featureGroups[0].id == created.id
      all.featureGroups[0].name == created.name
      all.featureGroups[1].id == created2.id
      all.featureGroups[1].name == created2.name
      !fgApi.getGroup(app1.id, superPerson, created2.id).features[0].value
  }

  def "i can create a feature group and then update it"() {
    given:
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
        [new FeatureGroupUpdateFeature().id(feature.id).value(true)]
      ))
    and:
      def getit = fgApi.getGroup(app1.id, superPerson, created.id)
    then:
      getit.id == created.id
      getit.name == "name"
      getit.features.size() == 1
      getit.features[0].id == feature.id
      getit.features[0].value
      getit.features[0].key == "sample_2"
    when:
      fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate()
        .version(created.version)
        .name("fred")
        .features([
        new FeatureGroupUpdateFeature().id(feature.id).value(false),
        new FeatureGroupUpdateFeature().id(feature2.id).value(123.67)]))
    and:
      getit = fgApi.getGroup(app1.id, superPerson, created.id)
    then:
      getit.name == "fred"
      getit.features.size() == 2
      !getit.features[0].value
      getit.features[0].key == "sample_2"
      getit.features[1].value == 123.67
      getit.features[1].key == "sample_3"
    when:
      def updated = fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate()
        .version(getit.version)
        .name("fred")
        .features([
          new FeatureGroupUpdateFeature().id(feature2.id).value(121.67)]))
    and:
      getit = fgApi.getGroup(app1.id, superPerson, created.id)
    then:
      getit.features.size() == 1
      getit.features[0].value == 121.67
      getit.features[0].key == "sample_3"
    when: "i update the description"
      def updated2 = fgApi.updateGroup(app1.id, superPerson, created.id, new FeatureGroupUpdate().version(updated.version).description("hello"))
    then:
      updated2.version != updated.version
      updated2.description == 'hello'
      fgApi.getGroup(app1.id, superPerson, created.id).description == 'hello'
  }
}
