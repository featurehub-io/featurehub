package io.featurehub.strategies.matchers


import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute
import spock.lang.Specification
import spock.lang.Unroll

class DateTimeArrayMatcherSpec extends Specification {
  DateTimeArrayMatcher matcher

  def setup() {
    matcher = new DateTimeArrayMatcher()
  }

  @Unroll
  def 'date array matcher matches as expected'() {
    given: 'i create a rollout strategy'
      def rsi = new FeatureRolloutStrategyAttribute().conditional(conditional).values(val)
    when:
      def theResult = matcher.match(suppliedValue, rsi)
    then:
      theResult == result
    where:
      suppliedValue               | conditional                                        | val                                                   || result
      '2019-02-01T01:01:01Z'      | RolloutStrategyAttributeConditional.EQUALS         | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || true
      '2019-02-01T01:01:01Z'      | RolloutStrategyAttributeConditional.INCLUDES       | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || true
      '2019-02-01T01:01:01Z'      | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || false
      '2019-02-01T01:01:01Z'      | RolloutStrategyAttributeConditional.EXCLUDES       | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || false
      '2019-02-07T01:01:01Z'      | RolloutStrategyAttributeConditional.EQUALS         | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || false
      '2019-02-07T01:01:01Z'      | RolloutStrategyAttributeConditional.INCLUDES       | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || false
      '2019-02-07T01:01:01Z'      | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || true
      '2019-02-07T01:01:01Z'      | RolloutStrategyAttributeConditional.EXCLUDES       | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || true
      '2019-02-07T01:01:01Z'      | RolloutStrategyAttributeConditional.GREATER        | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || true
      '2019-02-07T01:01:01Z'      | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || true
      '2019-02-01T01:01:01Z'      | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || true
      '2013-02-07T01:01:01Z'      | RolloutStrategyAttributeConditional.GREATER        | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || false
      '2019-02-01T01:01:01-01:00' | RolloutStrategyAttributeConditional.GREATER        | ['2019-02-01T01:01:01Z', '2019-02-01T01:01:01-00:30'] || true
      '2019-01-01T01:01:01Z'      | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['2019-01-02T01:01:01Z', '2019-02-01T01:01:01Z']      || false
      '2014-02-01T01:01:01Z'      | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || false
      '2019-02-07T01:01:01Z'      | RolloutStrategyAttributeConditional.LESS           | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || false
      '2018-02-07T01:01:01Z'      | RolloutStrategyAttributeConditional.LESS           | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || true
      '2019-02-07T01:01:01Z'      | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || false
      '2019-02-01T01:01:01Z'      | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['2019-01-01T01:01:01Z', '2019-02-01T01:01:01Z']      || true
      '2019-07-06T01:01:01Z'      | RolloutStrategyAttributeConditional.REGEX          | ['2019-.*']                                            | true
      '2017-07-06T01:01:01Z'      | RolloutStrategyAttributeConditional.REGEX          | ['2019-.*']                                            | false
      '2017-03-06T01:01:01Z'      | RolloutStrategyAttributeConditional.REGEX          | ['2019-.*', '(.*)-03-(.*)']                            | true
      '2017-03-06T01:01:01Z'      | RolloutStrategyAttributeConditional.STARTS_WITH    | ['2019', '2017']                                       | true
      '2017-03-06T01:01:01Z'      | RolloutStrategyAttributeConditional.STARTS_WITH    | ['2019']                                               | false
      '2017-03-06T01:01:01Z'      | RolloutStrategyAttributeConditional.ENDS_WITH      | [':01Z']                                               | true
      '2017-03-06T01:01:01Z'      | RolloutStrategyAttributeConditional.ENDS_WITH      | ['03', '2017', '01:01']                                | false
      '2017-03-06T01:01:01Z'      | RolloutStrategyAttributeConditional.ENDS_WITH      | ['rubbish']                                            | false
  }
}
