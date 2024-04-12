package io.featurehub.strategies.matchers


import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute
import spock.lang.Specification
import spock.lang.Unroll

class StringArrayMatcherSpec extends Specification {
  StringArrayMatcher matcher

  def setup() {
    matcher = new StringArrayMatcher()
  }

  @Unroll
  def 'string array matcher works as expected'() {
    given: 'i create a rollout strategy'
      def rsi = new FeatureRolloutStrategyAttribute().conditional(conditional).values(val)
    when:
      def theResult = matcher.match(suppliedValue.toString(), rsi)
    then:
      theResult == result
    where:
        suppliedValue    | conditional                                        | val                          || result
        'Android'        | RolloutStrategyAttributeConditional.EQUALS         | ['Android']                  || true
        'Android'        | RolloutStrategyAttributeConditional.EQUALS         | ['android']                  || false
        'Android'        | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['Android']                  || false
        'Windows'        | RolloutStrategyAttributeConditional.EXCLUDES     | ['Android', 'ios']                  || true
        'Android'        | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['android']                  || true
        'ios'            | RolloutStrategyAttributeConditional.INCLUDES       | ['android', 'ios', 'chrome'] || true
        'linux'          | RolloutStrategyAttributeConditional.INCLUDES       | ['android', 'ios', 'chrome'] || false
        'ios'            | RolloutStrategyAttributeConditional.EXCLUDES       | ['android', 'ios', 'chrome'] || false // val does not contain "ios" == false, it does
        'linux'          | RolloutStrategyAttributeConditional.EXCLUDES       | ['android', 'ios', 'chrome'] || true // val does not contain "linux" == true, it doesn't
        'peaches'        | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['peaches']                  || true
        'peaches'        | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['zebras']                   || false
        'peaches'        | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['cream', 'zebras']          || true
        'actapus (gold)' | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['mammal']                   || true
        'actapus (gold)' | RolloutStrategyAttributeConditional.LESS           | ['mammal']                   || true
        'actapus (gold)' | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['aardvark']                 || false
        '1'              | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['a', 'b']                   || true
        'c'              | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['a', 'b']                   || false
        'a'              | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['a', 'b']                   || true
        'b'              | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['a', 'b']                   || true
        'actapus (gold)' | RolloutStrategyAttributeConditional.LESS           | ['aardvark', 'aardshark']    || false
        'actapus (gold)' | RolloutStrategyAttributeConditional.REGEX          | ['(.*)gold(.*)']              | true
        'actapus (gold)' | RolloutStrategyAttributeConditional.REGEX          | ['(.*)purple(.*)']            | false
  }
}
