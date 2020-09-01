package io.featurehub.db.services


import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.services.strategies.RolloutStrategyValidationUtils
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import spock.lang.Specification

class RolloutStrategyValidationUtilsSpec extends Specification {
  RolloutStrategyValidationUtils validator


  def setup() {
    validator = new RolloutStrategyValidationUtils()
  }

  def "updating all environment features in an application by a specific feature will fail if the percentage strategy is > 100%"() {
    given: "i have a feature value with strategies greater than 100%"
      def fv =
        [new RolloutStrategy().name('too high').percentage(765400),
         new RolloutStrategy().name('2high2').percentage(653400)]
    when: "i attempt to update"
      validator.validateStrategies(fv)
    then:
      thrown RolloutStrategyValidator.PercentageStrategyGreaterThan100Percent
  }


  def "updating all environment features in an application by a specific feature will fail if we have negative percentages"() {
    given: "i have a feature value with a negative percentage"
      def fv = [new RolloutStrategy().name('neg %').percentage(-7654)]
    when: "i attempt to update"
      validator.validateStrategies(fv)
    then:
      thrown RolloutStrategyValidator.InvalidStrategyCombination
  }

  def "updating all environment features in an application by a specific feature will fail if we have attributes update with no attributes"() {
    given: "i have a feature value with no valid percentage configs"
      def fv = [new RolloutStrategy().name('empty')]
    when: "i attempt to update"
      validator.validateStrategies(fv)
    then:
      thrown RolloutStrategyValidator.InvalidStrategyCombination
  }

  def 'updating features and having a strategy with no name causes a failure'() {
    given: "i have a feature value with no valid configs"
      def fv =
        [new RolloutStrategy().percentage(3456).value(true)]
    when: "i attempt to update"
      validator.validateStrategies(fv)
    then:
      thrown RolloutStrategyValidator.InvalidStrategyCombination
  }

  def "we specify an array but the values aren't in the array"() {
    when: " rollout an array which is empty"
      validator.validateStrategies([new RolloutStrategy().name("fred").attributes([new RolloutStrategyAttribute().array(true)])])
    then:
      thrown RolloutStrategyValidator.InvalidStrategyCombination
  }

  def "when we specify all attributes is ok"() {
    when: "attr has everything field"
      validator.validateStrategies([new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute().value('x')
          .type(RolloutStrategyFieldType.STRING)
          .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
          .fieldName("fred")
      ])])
    then: "validation is successful"
      true
  }

  def "we specify a attr with no field"() {
    when: "attr has no field name"
      validator.validateStrategies([new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute().value('x')
          .type(RolloutStrategyFieldType.STRING)
          .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
      ])])
    then:
      thrown RolloutStrategyValidator.InvalidStrategyCombination
  }

  def "we specify no conditional"() {
    when: "attr has everything except conditional"
      validator.validateStrategies([new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute().value('x')
          .type(RolloutStrategyFieldType.STRING)
          .fieldName("fred")
      ])])
    then:
      thrown RolloutStrategyValidator.InvalidStrategyCombination
  }

  def "we specify no field type"() {
    when: "attr has everything except type"
      validator.validateStrategies([new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute().value('x')
          .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
          .fieldName("fred")
      ])])
    then:
      thrown RolloutStrategyValidator.InvalidStrategyCombination
  }

  def "we specify a value that isn't valid and this is picked up"() {
    when: "attr has everything field"
      validator.validateStrategies([new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute().value('x')
          .type(fieldType)
          .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
          .fieldName("fred")
          .value(value)
      ])])
    then:
      thrown RolloutStrategyValidator.InvalidStrategyCombination
    where:
      fieldType                                 | value
      RolloutStrategyFieldType.IP_ADDRESS       | 'fred'
      RolloutStrategyFieldType.IP_ADDRESS       | '192.168.2.1/76'
      RolloutStrategyFieldType.IP_ADDRESS       | '792.168.2.1'
      RolloutStrategyFieldType.IP_ADDRESS       | '192.256.2.1'
      RolloutStrategyFieldType.IP_ADDRESS       | '192.156.256.1'
      RolloutStrategyFieldType.IP_ADDRESS       | '192.156.250.256'
      RolloutStrategyFieldType.SEMANTIC_VERSION | 'fred'
      RolloutStrategyFieldType.SEMANTIC_VERSION | '1.1.1.1.noop'
      RolloutStrategyFieldType.NUMBER           | 'fred'
      RolloutStrategyFieldType.DATE             | 'fred'
      RolloutStrategyFieldType.DATETIME         | 'fred'
      RolloutStrategyFieldType.DATETIME         | '2017-08-01'
  }

  def "we specify array fields that are not valid and they are picked up"() {
    when: "attr has everything field"
      validator.validateStrategies([new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute().value('x')
          .type(fieldType)
          .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
          .fieldName("fred")
          .array(true)
          .values(values)
      ])])
    then:
      thrown RolloutStrategyValidator.InvalidStrategyCombination
    where:
      fieldType                                 | values
      RolloutStrategyFieldType.IP_ADDRESS       | ['fred']
      RolloutStrategyFieldType.IP_ADDRESS       | ['192.168.2.1/76']
      RolloutStrategyFieldType.IP_ADDRESS       | ['792.168.2.1']
      RolloutStrategyFieldType.IP_ADDRESS       | ['192.256.2.1']
      RolloutStrategyFieldType.IP_ADDRESS       | ['192.156.256.1']
      RolloutStrategyFieldType.IP_ADDRESS       | ['192.156.250.256']
      RolloutStrategyFieldType.SEMANTIC_VERSION | ['fred']
      RolloutStrategyFieldType.SEMANTIC_VERSION | ['1.1.1.1.noop']
      RolloutStrategyFieldType.NUMBER           | ['fred']
      RolloutStrategyFieldType.DATE             | ['fred']
      RolloutStrategyFieldType.DATETIME         | ['fred']
      RolloutStrategyFieldType.DATETIME         | ['2017-08-01']
  }

  def "we specify a bunch of valid values and they pass validation"() {
    when: "attr has everything field"
      validator.validateStrategies([new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute().value('x')
          .type(fieldType)
          .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
          .fieldName("fred")
          .value(value)
      ])])
    then:
      true
    where:
      fieldType                                 | value
      RolloutStrategyFieldType.IP_ADDRESS       | '10.0.0.1/0'
      RolloutStrategyFieldType.IP_ADDRESS       | '192.168.0.0/16'
      RolloutStrategyFieldType.IP_ADDRESS       | '1:2:3:4:5:6:7::/0'
      RolloutStrategyFieldType.IP_ADDRESS       | '1:2:3:4:5:6:7:8/64'
      RolloutStrategyFieldType.SEMANTIC_VERSION | '1.0.2'
      RolloutStrategyFieldType.SEMANTIC_VERSION | '1.2.6'
//      RolloutStrategyFieldType.NUMBER           | [5, (float)5.0, (double)5.0, new BigDecimal('5.0'), new BigInteger('9')]
      RolloutStrategyFieldType.NUMBER           | 5
      RolloutStrategyFieldType.NUMBER           | (float)5.0
      RolloutStrategyFieldType.NUMBER           | (double)5.0
      RolloutStrategyFieldType.NUMBER           | new BigDecimal('5.0')
      RolloutStrategyFieldType.NUMBER           | new BigInteger('9')
      RolloutStrategyFieldType.DATE             | '1984-12-12'
      RolloutStrategyFieldType.DATETIME         | '1984-12-12T14:32:26+03:00'
      RolloutStrategyFieldType.DATETIME         | '1984-12-11T11:32:26Z'
  }

  def "we define a bunch of valid array values and they pass validation"() {
      when: "attr has everything field"
        validator.validateStrategies([new RolloutStrategy().name("fred").attributes([
          new RolloutStrategyAttribute().value('x')
            .type(fieldType)
            .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
            .fieldName("fred")
            .array(true)
            .values(values)
        ])])
      then:
        true
    where:
      fieldType                                 | values
      RolloutStrategyFieldType.IP_ADDRESS       | ['10.0.0.1', '192.168.86.39/0']
      RolloutStrategyFieldType.IP_ADDRESS       | ['192.168.0.0/16']
      RolloutStrategyFieldType.IP_ADDRESS       | ['1:2:3:4:5:6:7::/0', '1:2:3:4:5:6:7:8']
      RolloutStrategyFieldType.IP_ADDRESS       | ['1:2:3:4:5:6:7:8/64']
      RolloutStrategyFieldType.SEMANTIC_VERSION | ['1.0.2']
      RolloutStrategyFieldType.SEMANTIC_VERSION | ['1.2.6']
      RolloutStrategyFieldType.NUMBER           | [5, (float) 5.0, (double) 5.0, new BigDecimal('5.0'), new BigInteger('9')]
      RolloutStrategyFieldType.DATE             | ['1984-12-12']
      RolloutStrategyFieldType.DATETIME         | ['1984-12-12T14:32:26+03:00']
      RolloutStrategyFieldType.DATETIME         | ['1984-12-11T11:32:26Z']
  }
}
