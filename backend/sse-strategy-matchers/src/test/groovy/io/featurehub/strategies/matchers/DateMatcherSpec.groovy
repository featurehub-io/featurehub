package io.featurehub.strategies.matchers

import io.featurehub.sse.model.RolloutStrategyAttribute
import io.featurehub.sse.model.RolloutStrategyAttributeConditional
import spock.lang.Specification
import spock.lang.Unroll

class DateMatcherSpec extends Specification {
  DateMatcher matcher

  def setup() {
    matcher = new DateMatcher()
  }

  @Unroll
  def 'date matcher matches as expected'() {
    given: 'i create a rollout strategy'
      def rsi = new RolloutStrategyAttribute().conditional(conditional).value(val)
    when:
      def theResult = matcher.match(suppliedValue, rsi)
    then:
      theResult == result
    where:
      suppliedValue | conditional                                        | val           || result
      '2019-02-01'  | RolloutStrategyAttributeConditional.EQUALS         | '2019-02-01'  || true
      '2019-02-01'  | RolloutStrategyAttributeConditional.INCLUDES       | '2019-02-01'  || true
      '2019-02-01'  | RolloutStrategyAttributeConditional.NOT_EQUALS     | '2019-02-01'  || false
      '2019-02-01'  | RolloutStrategyAttributeConditional.EXCLUDES       | '2019-02-01'  || false
      '2019-02-07'  | RolloutStrategyAttributeConditional.EQUALS         | '2019-01-01'  || false
      '2019-02-07'  | RolloutStrategyAttributeConditional.INCLUDES       | '2019-01-01'  || false
      '2019-02-07'  | RolloutStrategyAttributeConditional.NOT_EQUALS     | '2019-01-01'  || true
      '2019-02-07'  | RolloutStrategyAttributeConditional.EXCLUDES       | '2019-01-01'  || true
      '2019-02-07'  | RolloutStrategyAttributeConditional.GREATER        | '2019-01-01'  || true
      '2019-02-07'  | RolloutStrategyAttributeConditional.GREATER_EQUALS | '2019-02-01'  || true
      '2019-02-01'  | RolloutStrategyAttributeConditional.GREATER_EQUALS | '2019-02-01'  || true
      '2013-02-07'  | RolloutStrategyAttributeConditional.GREATER        | '2019-01-01'  || false
      '2019-01-01'  | RolloutStrategyAttributeConditional.GREATER_EQUALS | '2019-01-02'  || false
      '2014-02-01'  | RolloutStrategyAttributeConditional.GREATER_EQUALS | '2019-01-01'  || false
      '2019-02-07'  | RolloutStrategyAttributeConditional.LESS           | '2019-02-01'  || false
      '2018-02-07'  | RolloutStrategyAttributeConditional.LESS           | '2019-02-01'  || true
      '2019-02-07'  | RolloutStrategyAttributeConditional.LESS_EQUALS    | '2019-02-01'  || false
      '2019-02-01'  | RolloutStrategyAttributeConditional.LESS_EQUALS    | '2019-02-01'  || true
      '2019-07-06'  | RolloutStrategyAttributeConditional.REGEX          | '2019-.*'      | true
      '2017-07-06'  | RolloutStrategyAttributeConditional.REGEX          | '2019-.*'      | false
      '2017-03-06'  | RolloutStrategyAttributeConditional.REGEX          | '(.*)-03-(.*)' | true
      '2017-03-06'  | RolloutStrategyAttributeConditional.STARTS_WITH    | '2017'         | true
      '2017-03-06'  | RolloutStrategyAttributeConditional.STARTS_WITH    | '2019'         | false
      '2017-03-06'  | RolloutStrategyAttributeConditional.ENDS_WITH      | '06'           | true
      '2017-03-06'  | RolloutStrategyAttributeConditional.ENDS_WITH      | '03'           | false
      '2017-03-06'  | RolloutStrategyAttributeConditional.ENDS_WITH      | '2017'         | false
  }
}
