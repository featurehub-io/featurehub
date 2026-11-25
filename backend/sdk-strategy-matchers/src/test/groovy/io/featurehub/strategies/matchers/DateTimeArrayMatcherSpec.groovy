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

  @Unroll
  def 'supplied date-times are converted to UTC and compared to the second: #suppliedValue #conditional #val'() {
    given:
      def rsi = new FeatureRolloutStrategyAttribute().conditional(conditional).values(val)
    expect:
      matcher.match(suppliedValue, rsi) == result
    where:
      suppliedValue               | conditional                                        | val                                            || result
      // spec matcher_vectors
      '2019-01-01T01:01:01.123Z'  | RolloutStrategyAttributeConditional.EQUALS         | ['2019-01-01T01:01:01Z']                       || true
      '2021-05-05T10:00:00+02:00' | RolloutStrategyAttributeConditional.EQUALS         | ['2021-05-05T10:00:00Z', '2022-01-01T00:00:00Z'] || false
      '2021-05-05T10:00:00+02:00' | RolloutStrategyAttributeConditional.EQUALS         | ['2021-05-05T08:00:00Z']                       || true
      // no offset is taken as UTC (what Edge used to generate for "now")
      '2021-05-05T08:00:00'       | RolloutStrategyAttributeConditional.EQUALS         | ['2021-05-05T08:00:00Z']                       || true
      '2021-05-05T08:00:00.987654'| RolloutStrategyAttributeConditional.GREATER_EQUALS | ['2021-05-05T08:00:00Z']                       || true
      '2021-05-05T07:59:59'       | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['2021-05-05T08:00:00Z']                       || false
      // a date alone is midnight UTC
      '2021-05-05'                | RolloutStrategyAttributeConditional.EQUALS         | ['2021-05-05T00:00:00Z']                       || true
      'keep silent'               | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['2021-05-05T00:00:00Z']                       || false
  }
}
