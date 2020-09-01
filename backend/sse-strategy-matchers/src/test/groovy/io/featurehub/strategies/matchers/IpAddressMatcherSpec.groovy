package io.featurehub.strategies.matchers

import io.featurehub.sse.model.RolloutStrategyAttribute
import io.featurehub.sse.model.RolloutStrategyAttributeConditional
import spock.lang.Specification
import spock.lang.Unroll

class IpAddressMatcherSpec extends Specification {
  IpAddressMatcher matcher

  def setup() {
    matcher = new IpAddressMatcher()
  }

  @Unroll
  def 'ip address matching works as expected'() {
    given: 'i create a rollout strategy'
      def rsi = new RolloutStrategyAttribute().conditional(conditional).value(val)
    when:
      def theResult = matcher.match(suppliedValue, rsi)
    then:
      theResult == result
    where:
      suppliedValue   | conditional                                    | val                               || result
      '192.168.86.75' | RolloutStrategyAttributeConditional.EQUALS | '192.168.86.75' || true
      '192.168.86.75' | RolloutStrategyAttributeConditional.EQUALS     | '192.168.086.075'               || true
      '192.168.86.75' | RolloutStrategyAttributeConditional.EQUALS     | '192.168.0.0/16'                || true
      '192.168.86.75' | RolloutStrategyAttributeConditional.EQUALS     | '192.168.0.0/16' || true
      '192.168.86.72' | RolloutStrategyAttributeConditional.EQUALS     | '192.168.86.75'                 || false
      '192.168.86.72' | RolloutStrategyAttributeConditional.EQUALS     | '192.168.086.075'               || false
      '10.168.86.75'  | RolloutStrategyAttributeConditional.EQUALS     | '192.168.0.0/16'                || false
      '202.14.217.1'  | RolloutStrategyAttributeConditional.EQUALS     | '192.168.0.0/16'  || false
      '202.14.217.1'  | RolloutStrategyAttributeConditional.INCLUDES   | '10.7.4.8'     || false
      '192.168.86.75' | RolloutStrategyAttributeConditional.NOT_EQUALS | '192.168.86.75'                 || false
      '192.168.86.75' | RolloutStrategyAttributeConditional.NOT_EQUALS | '192.168.086.075'               || false
      '192.168.86.75' | RolloutStrategyAttributeConditional.NOT_EQUALS | '192.168.0.0/16'                || false
      '192.168.86.75' | RolloutStrategyAttributeConditional.NOT_EQUALS | '192.168.86.75'     || false
      '192.168.86.72' | RolloutStrategyAttributeConditional.NOT_EQUALS | '192.168.86.75'                 || true
      '192.168.86.72' | RolloutStrategyAttributeConditional.NOT_EQUALS | '192.168.086.075'               || true
      '10.168.86.75'  | RolloutStrategyAttributeConditional.NOT_EQUALS | '192.168.0.0/16'                || true
      '202.14.217.1'  | RolloutStrategyAttributeConditional.NOT_EQUALS | '192.168.0.0/16'  || true
      '202.14.217.1'  | RolloutStrategyAttributeConditional.NOT_EQUALS | '192.168.86.75'     || true
      '202.14.217.1'  | RolloutStrategyAttributeConditional.EXCLUDES   | '192.168.86.75'     || true
  }
}
