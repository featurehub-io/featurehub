package io.featurehub.edge.strategies

import io.featurehub.edge.strategies.matchers.MatcherRegistry
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueCacheItem
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategyInstance
import spock.lang.Specification
import spock.lang.Unroll

class ApplyFeatureSpec extends Specification {
  ApplyFeature applyFeature
  PercentageCalculator percentageCalculator
  Integer pc

  def setup() {
    // mocking doesn't work for "where" clauses
    percentageCalculator =  {String u, String id -> pc } as PercentageCalculator
    applyFeature = new ApplyFeature(percentageCalculator, new MatcherRegistry())
  }

  @Unroll
  def "if the user's percent is calculated x% and we have a rollout strategy at 20%"() {
    given: "we have rollout strategies set"
      def rollout = [
        new RolloutStrategyInstance()
          .percentage(20)
          .valueString("blue")
      ]
    and:
      def cac = new ClientAttributeCollection()
      cac.attributes.put(ClientAttributeCollection.USERKEY, ['mary@mary.com'])
    and: "a feature cache item"
      def fci = new FeatureValueCacheItem()
        .value(new FeatureValue().valueString("yellow").id('1').rolloutStrategyInstances(rollout))
        .feature(new Feature().valueType(FeatureValueType.STRING))
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(fci, cac)
    then:
      val == expected
    where:
      percent || expected
      22      || "yellow"
      15      || "blue"
      20      || "blue"
  }

  @Unroll
  def "if the user's percent is calculated x% and we have no rollout strategies"() {
    given: "we have rollout strategies set"
      def rollout = []
    and:
      def cac = new ClientAttributeCollection()
      cac.attributes.put(ClientAttributeCollection.USERKEY, ['mary@mary.com'])
    and: "a feature cache item"
      def fci = new FeatureValueCacheItem()
        .value(new FeatureValue().valueString("yellow").id('1').rolloutStrategyInstances(rollout))
        .feature(new Feature().valueType(FeatureValueType.STRING))
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(fci, cac)
    then:
      val == expected
    where:
      percent || expected
      22      || "yellow"
      15      || "yellow"
      20      || "yellow"
  }

  @Unroll
  def "We have rollout strategies but no user key"() {
    given: "we have rollout strategies set"
      def rollout = [
        new RolloutStrategyInstance()
          .percentage(20)
          .valueString("blue")
      ]
    and:
      def cac = new ClientAttributeCollection()
    and: "a feature cache item"
      def fci = new FeatureValueCacheItem()
        .value(new FeatureValue().valueString("yellow").id('1').rolloutStrategyInstances(rollout))
        .feature(new Feature().valueType(FeatureValueType.STRING))
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(fci, cac)
    then:
      val == expected
    where:
      percent || expected
      22      || "yellow"
      15      || "yellow"
      20      || "yellow"
  }

  def "a null ClientAttributeConnection generates the default value"() {
    given: "we have rollout strategies set"
      def rollout = [
        new RolloutStrategyInstance()
          .percentage(20)
          .valueString("blue")
      ]
    and:
      pc = null
    and: "a feature cache item"
      def fci = new FeatureValueCacheItem()
        .value(new FeatureValue().valueString("yellow").id('1').rolloutStrategyInstances(rollout))
        .feature(new Feature().valueType(FeatureValueType.STRING))
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(fci, null)
    then:
      val == "yellow"
  }
}


