package io.featurehub.edge.strategies

import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.sse.model.FeatureRolloutStrategy
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute
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
        new FeatureRolloutStrategy()
          .id("1234")
          .percentage(underPercent)
          .value("blue")
      ]
    and:
      def cac = new ClientContext(false)
      cac.attributes.put(ClientContext.USERKEY, ['mary@mary.com'])
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, "key", UUID.randomUUID().toString(), cac)
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
      def rollout = new ArrayList<FeatureRolloutStrategy>()
    and:
      def cac = new ClientContext(false)
      cac.attributes.put(ClientContext.USERKEY, ['mary@mary.com'])
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, "key", UUID.randomUUID().toString(), cac)
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
        new FeatureRolloutStrategy()
          .percentage(20)
          .value("blue")
      ]
    and:
      def cac = new ClientContext(false)
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, "key", UUID.randomUUID().toString(), cac)
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
        new FeatureRolloutStrategy()
          .percentage(20)
          .value("blue")
      ]
    and:
      def cac = new ClientContext(false)
    and:
      pc = null
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, "key", UUID.randomUUID().toString(), cac)
    then:
      !val.matched
  }

  def "we have a CAC with no percentage data at all but a match via attributes"() {
    given: "we have a strategy set"
      def rollout = [
        new FeatureRolloutStrategy().value("sausage")
          .id("1234")
          .percentage(0)
          .attributes([
            new FeatureRolloutStrategyAttribute()
              .fieldName("warehouseId")
              .conditional(RolloutStrategyAttributeConditional.EQUALS)
              .values(['ponsonby'])
              .type(RolloutStrategyFieldType.STRING)
          ])
      ]
    and:
      def calc = Mock(PercentageCalculator)
      def matcherRepo = new MatcherRegistry()
      def applier = new ApplyFeature(calc, matcherRepo)

    and:
      def cac = new ClientContext(true)
      cac.attributes[ClientContext.SESSIONKEY] = ['poorple']
      cac.attributes['warehouseId'] = ['ponsonby']
    when:
      def result = applier.applyFeature(rollout, 'feature', 'id', cac)
    then:
        result.matched
      result.value == 'sausage'
  }
}


