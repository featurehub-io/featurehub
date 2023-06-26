package io.featurehub.db.services

import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RoleType
import org.apache.commons.lang3.RandomStringUtils

class FeatureHistorySpec extends Base3Spec {

  def "i an create a history of features and query it different ways"() {
    given: "i have multiple environments"
      def env2 = environmentSqlApi.create(new Environment().description(RandomStringUtils.randomAlphabetic(10)).name(RandomStringUtils.randomAlphabetic(10)), app1, superPerson)
      def env3 = environmentSqlApi.create(new Environment().description("env3").name(RandomStringUtils.randomAlphabetic(10)), app1, superPerson)
    and: "i have multiple features"
      List<DbApplicationFeature> features = []
      def c = { FeatureValueType type ->
        def feat = new Feature().name(RandomStringUtils.randomAlphabetic(10)).key(RandomStringUtils.randomAlphabetic(10)).description("desc")
          .valueType(type)
        def f = applicationSqlApi.createApplicationLevelFeature(app1.id, feat, superPerson, Opts.empty())
        features.add(f)
      }
      3.times { c(FeatureValueType.BOOLEAN) }
      3.times { c(FeatureValueType.NUMBER) }
    and:
      def historyApi = new FeatureHistorySqlApi()
    when: "i ask for version history for all environments"
      def result = historyApi.listHistory(app1.id, [env1.id, env2.id, env3.id], [], [], [], null, null)
    then:
      result.max == 9
      result.items.size() == 9
      result.items.collect { it.envId }.unique().sort() == [env1.id, env2.id, env3.id].sort()
      result.items.collect { it.featureId }.unique() == features.findAll({it.valueType == FeatureValueType.BOOLEAN}).collect { it.id}
    when: "i update the second boolean it will return two versions"
      def fv = featureSqlApi.getFeatureValueForEnvironment(env1.id, features[1].key)
      featureSqlApi.updateFeatureValueForEnvironment(env1.id, features[1].key, fv.valueBoolean(true).locked(false), new PersonFeaturePermission(superPerson,
              [RoleType.CHANGE_VALUE, RoleType.UNLOCK] as Set<RoleType>))
      def result2 = historyApi.listHistory(app1.id, [env1.id], [], [], [features[1].id], null, null)
    then:
      result2.max == 2
      result2.items.size() == 1
      result2.items[0].history.size() == 2
      result2.items[0].history[0].locked
      !result2.items[0].history[0].value
      !result2.items[0].history[1].locked
      result2.items[0].history[1].value
      result2.items[0].history[1].who.id == superPerson.id.id
      result2.items[0].history[1].who.name == superPerson.name
    when: "i update the first number and I see a single number version"
      def num = featureSqlApi.getFeatureValueForEnvironment(env2.id, features[3].key)
      featureSqlApi.updateFeatureValueForEnvironment(env2.id, features[3].key, fv.valueNumber(45.32).locked(false), new PersonFeaturePermission(superPerson,
        [RoleType.CHANGE_VALUE, RoleType.UNLOCK] as Set<RoleType>))
      def result3 = historyApi.listHistory(app1.id, [env2.id], [], [features[3].key], [], null, null)
    then:
      result3.max == 1
      result3.items.size() == 1
      result3.items[0].history.size() == 1
      result3.items[0].history[0].value == 45.32
    when: "i use an app id that doesn't relate to the features"
      def result4 = historyApi.listHistory(UUID.randomUUID(), [env2.id], [], [features[3].key], [], null, null)
    then:
      result4.max == 0
      result4.items.size() == 0
  }
}
