package io.featurehub.strategies.matchers

import io.featurehub.sse.model.RolloutStrategyAttribute
import io.featurehub.sse.model.RolloutStrategyAttributeConditional
import spock.lang.Specification
import spock.lang.Unroll

class SemanticVersionArrayMatcherSpec extends Specification {
  SemanticVersionArrayMatcher matcher

  def setup() {
    matcher = new SemanticVersionArrayMatcher()
  }

  @Unroll
  def 'number array matcher works as expected'() {
    given: 'i create a rollout strategy'
      def rsi = new RolloutStrategyAttribute().conditional(conditional).values(val)
    when:
      def theResult = matcher.match(suppliedValue.toString(), rsi)
    then:
      theResult == result
    where:
      suppliedValue | conditional                                        | val     || result
      '2.0.3'       | RolloutStrategyAttributeConditional.EQUALS | ['2.0.3'] || true
      '2.0.3'       | RolloutStrategyAttributeConditional.EQUALS         | ['2.0.1'] || false
      '2.0.3'       | RolloutStrategyAttributeConditional.EQUALS         | ['2.0.1', '2.0.3'] || true
      '2.0.3'       | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['2.0.3'] || false
      '2.0.3'       | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['2.0.1'] || true
      '2.1'         | RolloutStrategyAttributeConditional.GREATER        | ['2.0']   || true
      '7.1.6'       | RolloutStrategyAttributeConditional.GREATER        | ['7.0']   || true
      '7.1.6'       | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['7.1.0'] || true
      '7.1.6'       | RolloutStrategyAttributeConditional.GREATER        | ['8.1.0'] || false
      '7.1.6'       | RolloutStrategyAttributeConditional.LESS           | ['8.1.0'] || true
      '7.1.6'       | RolloutStrategyAttributeConditional.LESS           | ['7.1.7'] || true
      '7.1.6'       | RolloutStrategyAttributeConditional.LESS           | ['3.1.7'] || false
      '7.1.6'       | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['7.1.6'] || true
      '7.1.6'       | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['6.1.6'] || false
  }
}
