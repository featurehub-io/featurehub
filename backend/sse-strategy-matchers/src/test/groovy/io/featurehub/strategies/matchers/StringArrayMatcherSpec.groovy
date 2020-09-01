package io.featurehub.strategies.matchers

import io.featurehub.sse.model.RolloutStrategyAttribute
import io.featurehub.sse.model.RolloutStrategyAttributeConditional
import spock.lang.Specification
import spock.lang.Unroll

class StringArrayMatcherSpec extends Specification {
  StringArrayMatcher matcher

  def setup() {
    matcher = new StringArrayMatcher()
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
      suppliedValue    | conditional                                        | val                          || result
      'Android'        | RolloutStrategyAttributeConditional.EQUALS         | ['Android']                  || true
      'Android'        | RolloutStrategyAttributeConditional.EQUALS         | ['android']                  || false
      'Android'        | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['Android']                  || false
      'Android'        | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['android']                  || true
      'ios'            | RolloutStrategyAttributeConditional.INCLUDES       | ['android', 'ios', 'chrome'] || true
      'linux'          | RolloutStrategyAttributeConditional.INCLUDES       | ['android', 'ios', 'chrome'] || false
      'ios'            | RolloutStrategyAttributeConditional.EXCLUDES       | ['android', 'ios', 'chrome'] || false
      'linux'          | RolloutStrategyAttributeConditional.EXCLUDES       | ['android', 'ios', 'chrome'] || true
      'peaches'        | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['peaches']                  || true
      'peaches'        | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['zebras']                   || false
      'peaches'        | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['cream', 'zebras']          || true
      'actapus (gold)' | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['mammal']                   || true
      'actapus (gold)' | RolloutStrategyAttributeConditional.LESS           | ['mammal']                   || true
      'actapus (gold)' | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['aardvark']                 || false
      'actapus (gold)' | RolloutStrategyAttributeConditional.LESS           | ['aardvark', 'aardshark']    || false
      'actapus (gold)' | RolloutStrategyAttributeConditional.REGEX | ['(.*)gold(.*)'] | true
      'actapus (gold)' | RolloutStrategyAttributeConditional.REGEX | ['(.*)purple(.*)'] | false
  }
}
