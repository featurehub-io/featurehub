package io.featurehub.strategies.matchers


import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute
import spock.lang.Specification

class IpAddressArrayMatcherSpec extends Specification {
  IpAddressArrayMatcher matcher

  def setup() {
    matcher = new IpAddressArrayMatcher()
  }

  def "ip address array match returns expected results"() {
    given: 'i create a rollout strategy'
      def rsi = new FeatureRolloutStrategyAttribute().conditional(conditional).values(val)
    when:
      def theResult = matcher.match(suppliedValue, rsi)
    then:
      theResult == result
    where:
      suppliedValue   | conditional                                    | val                               || result
      '192.168.86.75' | RolloutStrategyAttributeConditional.EQUALS     | ['192.168.86.75']                 || true
      '192.168.86.75' | RolloutStrategyAttributeConditional.EQUALS     | ['192.168.086.075']               || true
      '192.168.86.75' | RolloutStrategyAttributeConditional.EQUALS     | ['192.168.0.0/16']                || true
      '192.168.86.75' | RolloutStrategyAttributeConditional.EQUALS     | ['10.0.0.0/24', '192.168.0.0/16'] || true
      '192.168.86.75' | RolloutStrategyAttributeConditional.EQUALS     | ['10.7.4.8', '192.168.86.75']     || true
      '192.168.86.72' | RolloutStrategyAttributeConditional.EQUALS     | ['192.168.86.75']                 || false
      '192.168.86.72' | RolloutStrategyAttributeConditional.EQUALS     | ['192.168.086.075']               || false
      '10.168.86.75'  | RolloutStrategyAttributeConditional.EQUALS     | ['192.168.0.0/16']                || false
      '202.14.217.1'  | RolloutStrategyAttributeConditional.EQUALS     | ['10.0.0.0/8', '192.168.0.0/16']  || false
      '202.14.217.1'  | RolloutStrategyAttributeConditional.EQUALS     | ['10.7.4.8', '192.168.86.75']     || false
      '202.14.217.1'  | RolloutStrategyAttributeConditional.INCLUDES   | ['10.7.4.8', '192.168.86.75']     || false
      '192.168.86.75' | RolloutStrategyAttributeConditional.NOT_EQUALS | ['192.168.86.75']                 || false
      '192.168.86.75' | RolloutStrategyAttributeConditional.NOT_EQUALS | ['192.168.086.075']               || false
      '192.168.86.75' | RolloutStrategyAttributeConditional.NOT_EQUALS | ['192.168.0.0/16']                || false
      '192.168.86.75' | RolloutStrategyAttributeConditional.NOT_EQUALS | ['10.0.0.0/24', '192.168.0.0/16'] || false
      '192.168.86.75' | RolloutStrategyAttributeConditional.NOT_EQUALS | ['10.7.4.8', '192.168.86.75']     || false
      '192.168.86.72' | RolloutStrategyAttributeConditional.NOT_EQUALS | ['192.168.86.75']                 || true
      '192.168.86.72' | RolloutStrategyAttributeConditional.NOT_EQUALS | ['192.168.086.075']               || true
      '10.168.86.75'  | RolloutStrategyAttributeConditional.NOT_EQUALS | ['192.168.0.0/16']                || true
      '202.14.217.1'  | RolloutStrategyAttributeConditional.NOT_EQUALS | ['10.0.0.0/8', '192.168.0.0/16']  || true
      '202.14.217.1'  | RolloutStrategyAttributeConditional.NOT_EQUALS | ['10.7.4.8', '192.168.86.75']     || true
      '202.14.217.1'  | RolloutStrategyAttributeConditional.EXCLUDES   | ['10.7.4.8', '192.168.86.75']     || true
  }
}
