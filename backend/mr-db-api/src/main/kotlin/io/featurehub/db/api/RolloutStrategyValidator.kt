package io.featurehub.db.api

import io.featurehub.mr.model.*

interface RolloutStrategyValidator {
  class ValidationFailure {
    val customStrategyViolations = mutableMapOf<RolloutStrategy, MutableSet<RolloutStrategyViolation>>()
    val sharedStrategyViolations = mutableMapOf<RolloutStrategyInstance, MutableSet<RolloutStrategyViolation>>()
    val collectionViolationType = mutableSetOf<RolloutStrategyCollectionViolationType>()

    fun add(failure: RolloutStrategyViolation, strategy: RolloutStrategy) {
      customStrategyViolations.computeIfAbsent(strategy) { mutableSetOf() }.add(failure)
    }

    fun add(failure: RolloutStrategyViolation, rsi: RolloutStrategyInstance) {
      sharedStrategyViolations.computeIfAbsent(rsi) { mutableSetOf() }.add(failure)
    }

    fun add(failure: RolloutStrategyCollectionViolationType) {
      collectionViolationType.add(failure)
    }

    @Throws(InvalidStrategyCombination::class)
    fun hasFailedValidation() {
      if (isInvalid) {
        throw InvalidStrategyCombination(this)
      }
    }

    val isInvalid: Boolean
      get() = !(customStrategyViolations.isEmpty() && sharedStrategyViolations.isEmpty() && collectionViolationType.isEmpty())
  }

  class InvalidStrategyCombination(val failure: ValidationFailure) : Exception()

  fun validateStrategies(
    featureValueType: FeatureValueType?, customStrategies: List<RolloutStrategy>,
    sharedStrategies: List<RolloutStrategyInstance>
  ): ValidationFailure

  fun validateStrategies(
    featureValueType: FeatureValueType?,
    customStrategies: List<RolloutStrategy>,
    sharedStrategies: List<RolloutStrategyInstance>, failure: ValidationFailure?): ValidationFailure
}
