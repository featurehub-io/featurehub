package io.featurehub.db.services

import io.featurehub.db.api.Opts
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureGroupCreate
import io.featurehub.mr.model.FeatureGroupStrategy
import io.featurehub.mr.model.FeatureGroupUpdateFeature
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.SortOrder

class FeatureGroupSpec extends Base3Spec {
  FeatureGroupSqlApi fgApi

  def setup() {
    fgApi = new FeatureGroupSqlApi(convertUtils)
  }

  def "i can create, then find then update then find the feature group"() {
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
        [new FeatureGroupUpdateFeature().id(feature.id).value(true)]).strategy(new FeatureGroupStrategy().percentage(20)))
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
  }
}
