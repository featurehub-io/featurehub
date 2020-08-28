package io.featurehub.db.services

import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.RolloutStrategy
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
}
