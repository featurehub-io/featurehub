package io.featurehub.edge.strategies

import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueCacheItem
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.sse.model.RolloutStrategy
import io.featurehub.strategies.matchers.MatcherRegistry
import io.featurehub.strategies.percentage.PercentageCalculator
import spock.lang.Specification
import spock.lang.Unroll

class ApplyFeatureSpec extends Specification {
  ApplyFeature applyFeature
  PercentageCalculator percentageCalculator
  Integer pc

  def setup() {
    // mocking doesn't work for "where" clauses
    pc = 21
    percentageCalculator =  {String u, String id -> pc } as PercentageCalculator
//    percentageCalculator = new PC()
    applyFeature = new ApplyFeature(percentageCalculator, new MatcherRegistry())
  }

  @Unroll
  def "if the user's percent is calculated x% and we have a rollout strategy at 20%"() {
    given: "we have rollout strategies set"
      def rollout = [
        new RolloutStrategy()
          .percentage(underPercent)
          .value("blue")
      ]
    and:
      def cac = new ClientContext(isClientEvaluation)
      cac.attributes.put(ClientContext.USERKEY, ['mary@mary.com'])
    and: "a feature cache item"
      def fci = new FeatureValueCacheItem()
        .value(new FeatureValue().valueString("yellow").id('1'))
        .feature(new Feature().valueType(FeatureValueType.STRING))
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, fci.feature.key, fci.value.id, cac)
    then:
      val.matched == matched
      val.value == expected
    where:
      underPercent || expected | matched
      22      || "blue"     | true
      75      || "blue"     | true
      15      || null   | false
      20      || null   | false
  }

  @Unroll
  def "if the user's percent is calculated x% and we have no rollout strategies"() {
    given: "we have rollout strategies set"
      def rollout = new ArrayList<RolloutStrategy>()
    and:
      def cac = new ClientContext(isClientEvaluation)
      cac.attributes.put(ClientContext.USERKEY, ['mary@mary.com'])
    and: "a feature cache item"
      def fci = new FeatureValueCacheItem()
        .value(new FeatureValue().valueString("yellow").id('1'))
        .feature(new Feature().valueType(FeatureValueType.STRING))
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, fci.feature.key, fci.value.id, cac)
    then:
      val.matched == matched
      val.value == expected
    where:
      percent || expected | matched
      22      || null     | false
      15      || null     | false
      20      || null     | false
  }

  @Unroll
  def "We have rollout strategies but no user key"() {
    given: "we have rollout strategies set"
      def rollout = [
        new RolloutStrategy()
          .percentage(20)
          .value("blue")
      ]
    and:
      def cac = new ClientContext(isClientEvaluation)
    and: "a feature cache item"
      def fci = new FeatureValueCacheItem()
        .value(new FeatureValue().valueString("yellow").id('1'))
        .feature(new Feature().valueType(FeatureValueType.STRING))
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, fci.feature.key, fci.value.id, cac)
    then:
      val.matched == matched
      val.value == expected
    where:
      percent || expected | matched
      22      || null     | false
      15      || null     | false
      20      || null     | false
  }

  def "a null ClientAttributeConnection generates the default value"() {
    given: "we have rollout strategies set"
      def rollout = [
        new RolloutStrategy()
          .percentage(20)
          .value("blue")
      ]
    and:
      pc = null
    and: "a feature cache item"
      def fci = new FeatureValueCacheItem()
        .value(new FeatureValue().valueString("yellow").id('1'))
        .feature(new Feature().valueType(FeatureValueType.STRING))
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, fci.feature.key, fci.value.id, null)
    then:
      !val.matched
  }
}


