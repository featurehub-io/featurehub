package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.FeatureRolloutStrategyAttribute;

import java.util.List;
import java.util.stream.Collectors;

public class StringArrayMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, FeatureRolloutStrategyAttribute attr) {
    List<String> vals = attr.getValues().stream()
      .map(Object::toString).collect(Collectors.toList());

    switch(attr.getConditional()) {
      case EQUALS:
        return vals.stream().anyMatch(v -> v.equals(suppliedValue));
      case ENDS_WITH:
        return vals.stream().anyMatch(suppliedValue::endsWith);
      case STARTS_WITH:
        return vals.stream().anyMatch(suppliedValue::startsWith);
      case GREATER:
        return vals.stream().anyMatch(v -> suppliedValue.compareTo(v) > 0);
      case GREATER_EQUALS:
        return vals.stream().anyMatch(v -> suppliedValue.compareTo(v) >= 0);
      case LESS:
        return vals.stream().anyMatch(v -> suppliedValue.compareTo(v) < 0);
      case LESS_EQUALS:
        return vals.stream().anyMatch(v -> suppliedValue.compareTo(v) <= 0);
      case NOT_EQUALS:
        return vals.stream().anyMatch(v -> suppliedValue.compareTo(v) != 0);
      case INCLUDES:
        return vals.stream().anyMatch(suppliedValue::contains);
      case EXCLUDES:
        return vals.stream().noneMatch(suppliedValue::contains);
      case REGEX:
        return vals.stream().anyMatch(suppliedValue::matches);
    }

    return false;
  }
}
