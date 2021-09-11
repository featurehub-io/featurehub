package io.featurehub.db.api;

import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyCollectionViolationType;
import io.featurehub.mr.model.RolloutStrategyInstance;
import io.featurehub.mr.model.RolloutStrategyViolation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RolloutStrategyValidator {

  class ValidationFailure {
    public Map<RolloutStrategy, Set<RolloutStrategyViolation>> customStrategyViolations = new HashMap<>();
    public Map<RolloutStrategyInstance, Set<RolloutStrategyViolation>> sharedStrategyViolations = new HashMap<>();
    public Set<RolloutStrategyCollectionViolationType> collectionViolationType = new HashSet<>();

    public void add(RolloutStrategyViolation failure, RolloutStrategy strategy) {
      customStrategyViolations.computeIfAbsent(strategy, (k) -> new HashSet<>()).add(failure);
    }

    public void add(RolloutStrategyViolation failure, RolloutStrategyInstance rsi) {
      sharedStrategyViolations.computeIfAbsent(rsi, (k) -> new HashSet<>()).add(failure);
    }

    public void add(RolloutStrategyCollectionViolationType failure) {
      collectionViolationType.add(failure);
    }

    public void hasFailedValidation() throws InvalidStrategyCombination {
      if (isInvalid()) {
        throw new InvalidStrategyCombination(this);
      }
    }

    public boolean isInvalid() {
      return !(customStrategyViolations.isEmpty() && sharedStrategyViolations.isEmpty() && collectionViolationType.isEmpty());
    }
  }

  class InvalidStrategyCombination extends Exception {
    public final ValidationFailure failure;

    public InvalidStrategyCombination(ValidationFailure failure) {
      this.failure = failure;
    }
  }

  ValidationFailure validateStrategies(List<RolloutStrategy> customStrategies,
                             List<RolloutStrategyInstance> sharedStrategies);
  ValidationFailure validateStrategies(List<RolloutStrategy> customStrategies,
                             List<RolloutStrategyInstance> sharedStrategies, ValidationFailure failure);
}
