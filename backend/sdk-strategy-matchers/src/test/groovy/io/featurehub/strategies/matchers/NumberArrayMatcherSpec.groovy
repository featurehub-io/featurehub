package io.featurehub.strategies.matchers


import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute
import spock.lang.Specification
import spock.lang.Unroll

class NumberArrayMatcherSpec extends Specification {
  NumberArrayMatcher matcher

  def setup() {
    matcher = new NumberArrayMatcher()
  }

  @Unroll
  def 'number array matcher works as expected'() {
    given: 'i create a rollout strategy'
      def rsi = new FeatureRolloutStrategyAttribute().conditional(conditional).values(val)
    when:
      def theResult = matcher.match(suppliedValue.toString(), rsi)
    then:
      theResult == result
    where:
      suppliedValue | conditional                                        | val          || result
      5             | RolloutStrategyAttributeConditional.EQUALS         | [10, 5]      || true
      5             | RolloutStrategyAttributeConditional.EQUALS         | [5]          || true
      5             | RolloutStrategyAttributeConditional.EQUALS         | [4]          || false
      5             | RolloutStrategyAttributeConditional.EQUALS         | [4, 7]       || false
      5             | RolloutStrategyAttributeConditional.INCLUDES       | [4, 7]       || false
      5             | RolloutStrategyAttributeConditional.NOT_EQUALS     | [23, 100923] || true
      5             | RolloutStrategyAttributeConditional.EXCLUDES       | [23, 100923] || true
      5             | RolloutStrategyAttributeConditional.NOT_EQUALS     | [5]          || false
      5             | RolloutStrategyAttributeConditional.GREATER        | [2, 4]       || true
      5             | RolloutStrategyAttributeConditional.GREATER_EQUALS | [2, 5]       || true
      5             | RolloutStrategyAttributeConditional.LESS_EQUALS    | [2, 5]       || true
      5             | RolloutStrategyAttributeConditional.LESS           | [7, 6]       || true

      5             | RolloutStrategyAttributeConditional.GREATER        | [7, 10]      || false
      5             | RolloutStrategyAttributeConditional.GREATER_EQUALS | [6, 7]       || false
      5             | RolloutStrategyAttributeConditional.LESS_EQUALS    | [2, 3]       || false
      5             | RolloutStrategyAttributeConditional.LESS           | [1, -1]      || false
  }
}
