package io.featurehub.strategies.matchers

import io.featurehub.sse.model.RolloutStrategyAttribute
import io.featurehub.sse.model.RolloutStrategyAttributeConditional
import spock.lang.Specification
import spock.lang.Unroll

class NumberMatcherSpec extends Specification {
  NumberMatcher matcher

  def setup() {
    matcher = new NumberMatcher()
  }

  @Unroll
  def 'number array matcher works as expected'() {
    given: 'i create a rollout strategy'
      def rsi = new RolloutStrategyAttribute().conditional(conditional).value(val)
    when:
      def theResult = matcher.match(suppliedValue.toString(), rsi)
    then:
      theResult == result
    where:
      suppliedValue | conditional                                        | val || result
      5             | RolloutStrategyAttributeConditional.EQUALS         | 5   || true
      5             | RolloutStrategyAttributeConditional.EQUALS         | 5   || true
      5             | RolloutStrategyAttributeConditional.EQUALS         | 4   || false
      5             | RolloutStrategyAttributeConditional.INCLUDES       | 4   || false
      5             | RolloutStrategyAttributeConditional.INCLUDES       | 5   || true
      5             | RolloutStrategyAttributeConditional.NOT_EQUALS     | 23  || true
      5             | RolloutStrategyAttributeConditional.EXCLUDES       | 23  || true
      5             | RolloutStrategyAttributeConditional.NOT_EQUALS     | 5   || false
      5             | RolloutStrategyAttributeConditional.GREATER        | 2   || true
      5             | RolloutStrategyAttributeConditional.GREATER_EQUALS | 5   || true
      5             | RolloutStrategyAttributeConditional.LESS_EQUALS    | 5   || true
      5             | RolloutStrategyAttributeConditional.LESS_EQUALS    | 2   || false
      5             | RolloutStrategyAttributeConditional.LESS           | 7   || true

      5             | RolloutStrategyAttributeConditional.GREATER        | 7   || false
      5             | RolloutStrategyAttributeConditional.GREATER_EQUALS | 6   || false
      5             | RolloutStrategyAttributeConditional.LESS_EQUALS    | 2   || false
      5             | RolloutStrategyAttributeConditional.LESS           | -1  || false

  }
}
