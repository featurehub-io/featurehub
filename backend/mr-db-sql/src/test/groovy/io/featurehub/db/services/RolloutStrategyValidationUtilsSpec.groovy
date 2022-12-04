package io.featurehub.db.services


import io.featurehub.db.services.strategies.RolloutStrategyValidationUtils
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyCollectionViolationType
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.mr.model.RolloutStrategyViolationType
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
      def validations = validator.validateStrategies(FeatureValueType.STRING, fv, [])
    then:
      validations.isInvalid()
      validations.customStrategyViolations.isEmpty()
      validations.collectionViolationType.contains(RolloutStrategyCollectionViolationType.PERCENTAGE_ADDS_OVER_100_PERCENT)
  }


  def "updating all environment features in an application by a specific feature will fail if we have negative percentages"() {
    given: "i have a feature value with a negative percentage"
      def fv = [new RolloutStrategy().name('neg %').percentage(-7654)]
    when: "i attempt to update"
      def validations = validator.validateStrategies(FeatureValueType.STRING, fv, [])
    then:
      validations.isInvalid()
      !validations.customStrategyViolations.isEmpty()
      validations.customStrategyViolations[fv[0]]*.violation.contains(RolloutStrategyViolationType.NEGATIVE_PERCENTAGE)
  }

  def "when we specify all attributes is ok"() {
    when: "attr has everything field"
      def validations = validator.validateStrategies(FeatureValueType.STRING,[new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute().values(['x'])
          .type(RolloutStrategyFieldType.STRING)
          .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
          .fieldName("fred")
      ])], [])
    then: "validation is successful"
      !validations.isInvalid()
  }

  def "we specify a attr with no field"() {
    when: "attr has no field name"
      def id = UUID.randomUUID().toString()
      def fv = [new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute().id(id).values(['x'])
      ])]
      def validations = validator.validateStrategies(FeatureValueType.STRING,fv, [])
    then:
      validations.isInvalid()
      !validations.customStrategyViolations.isEmpty()
      validations.customStrategyViolations[fv[0]].size() == 3
      validations.customStrategyViolations[fv[0]]*.id == [id, id, id]
      validations.customStrategyViolations[fv[0]]*.violation.contains(RolloutStrategyViolationType.ATTR_MISSING_FIELD_NAME)
      validations.customStrategyViolations[fv[0]]*.violation.contains(RolloutStrategyViolationType.ATTR_MISSING_FIELD_TYPE)
      validations.customStrategyViolations[fv[0]]*.violation.contains(RolloutStrategyViolationType.ATTR_MISSING_CONDITIONAL)
  }

  private RolloutStrategyViolationType violationFromFieldType(RolloutStrategyFieldType fieldType) {
    RolloutStrategyViolationType violationType = null

    if (fieldType == RolloutStrategyFieldType.IP_ADDRESS) {
      violationType = RolloutStrategyViolationType.ATTR_VAL_NOT_CIDR
    } else if (fieldType == RolloutStrategyFieldType.SEMANTIC_VERSION) {
      violationType = RolloutStrategyViolationType.ATTR_VAL_NOT_SEMANTIC_VERSION
    } else if (fieldType == RolloutStrategyFieldType.NUMBER) {
      violationType = RolloutStrategyViolationType.ATTR_VAL_NOT_NUMBER
    } else if (fieldType == RolloutStrategyFieldType.DATE) {
      violationType = RolloutStrategyViolationType.ATTR_VAL_NOT_DATE
    } else if (fieldType == RolloutStrategyFieldType.DATETIME) {
      violationType = RolloutStrategyViolationType.ATTR_VAL_NOT_DATE_TIME
    }

    return violationType
  }

  def "we specify a value that isn't valid and this is picked up"() {
    given: "we know expected error"
      RolloutStrategyViolationType violationType = violationFromFieldType(fieldType)
    when: "attr has everything field"
      def fv = [new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute()
          .type(fieldType)
          .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
          .fieldName("fred")
          .values([value])
      ])]
      def validations = validator.validateStrategies(FeatureValueType.STRING,fv, [])
    then:
      validations.isInvalid()
      !validations.customStrategyViolations.isEmpty()
      validations.customStrategyViolations[fv[0]]*.violation.contains(violationType)
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
      RolloutStrategyViolationType violationType = violationFromFieldType(fieldType)

      def fv = [new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute()
          .type(fieldType)
          .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
          .fieldName("fred")
          .values(values)
      ])]

      def validations = validator.validateStrategies(FeatureValueType.STRING,fv, [])
    then:
      validations.isInvalid()
      !validations.customStrategyViolations.isEmpty()
      validations.customStrategyViolations[fv[0]]*.violation.contains(violationType)
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
      def fv = [new RolloutStrategy().name("fred").attributes([
        new RolloutStrategyAttribute()
          .type(fieldType)
          .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
          .fieldName("fred")
          .values([value])
      ])]
      def validations = validator.validateStrategies(FeatureValueType.STRING,fv, [])
    then:
      !validations.isInvalid()
    where:
      fieldType                                 | value
      RolloutStrategyFieldType.IP_ADDRESS       | '10.0.0.1/0'
      RolloutStrategyFieldType.IP_ADDRESS       | '192.168.0.0/16'
      RolloutStrategyFieldType.IP_ADDRESS       | '1:2:3:4:5:6:7::/0'
      RolloutStrategyFieldType.IP_ADDRESS       | '1:2:3:4:5:6:7:8/64'
      RolloutStrategyFieldType.SEMANTIC_VERSION | '1.0.2'
      RolloutStrategyFieldType.SEMANTIC_VERSION | '1.2.6'
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
        def validations = validator.validateStrategies(FeatureValueType.STRING,[new RolloutStrategy().name("fred").attributes([
          new RolloutStrategyAttribute()
            .type(fieldType)
            .conditional(RolloutStrategyAttributeConditional.LESS_EQUALS)
            .fieldName("fred")
            .values(values)
        ])], [])
      then:
        !validations.isInvalid()
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
