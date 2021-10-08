package io.featurehub.strategies.matchers

import io.featurehub.sse.model.RolloutStrategyAttribute
import io.featurehub.sse.model.RolloutStrategyAttributeConditional
import spock.lang.Specification
import spock.lang.Unroll

class BooleanArrayMatcherSpec extends Specification {
  BooleanArrayMatcher matcher

  def setup() {
    matcher = new BooleanArrayMatcher()
  }

  @Unroll
  def "all the support variants work as expected"() {
    given: "I create a RolloutStrategyAttribute representing this"
      def rsa = new RolloutStrategyAttribute().values([val]).conditional(conditional)
    when:
      def theresult = matcher.match(suppliedValue, rsa)
    then:
      theresult == result
    where:
      suppliedValue | conditional                                        | val   || result
      "true"        | RolloutStrategyAttributeConditional.EQUALS         | true  || true
      "true"        | RolloutStrategyAttributeConditional.EQUALS         | false || false
      "false"       | RolloutStrategyAttributeConditional.EQUALS         | false || true
      "false"       | RolloutStrategyAttributeConditional.EQUALS         | true  || false
      "true"        | RolloutStrategyAttributeConditional.NOT_EQUALS     | true  || false
      "true"        | RolloutStrategyAttributeConditional.NOT_EQUALS     | false || true
      "false"       | RolloutStrategyAttributeConditional.NOT_EQUALS     | false || false
      "false"       | RolloutStrategyAttributeConditional.NOT_EQUALS     | true  || true
      "false"       | RolloutStrategyAttributeConditional.INCLUDES       | true  || false
      "false"       | RolloutStrategyAttributeConditional.EXCLUDES       | true  || false
      "false"       | RolloutStrategyAttributeConditional.REGEX          | true  || false
      "false"       | RolloutStrategyAttributeConditional.ENDS_WITH      | true  || false
      "false"       | RolloutStrategyAttributeConditional.STARTS_WITH    | true  || false
      "false"       | RolloutStrategyAttributeConditional.GREATER        | true  || false
      "false"       | RolloutStrategyAttributeConditional.GREATER_EQUALS | true  || false
      "false"       | RolloutStrategyAttributeConditional.LESS_EQUALS    | true  || false
      "false"       | RolloutStrategyAttributeConditional.LESS           | true  || false
  }
}
