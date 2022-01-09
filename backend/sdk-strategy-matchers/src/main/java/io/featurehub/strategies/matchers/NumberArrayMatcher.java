package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.FeatureRolloutStrategyAttribute;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NumberArrayMatcher implements StrategyMatcher {
  private BigDecimal supplied = null;

  @Override
  public boolean match(String suppliedValue, FeatureRolloutStrategyAttribute attr) {
    try {
      Supplier<BigDecimal> bd = () -> {
        if (supplied == null) {
          supplied = new BigDecimal(suppliedValue);
        }

        return supplied;
      };

      Supplier<List<BigDecimal>> vals = () -> attr.getValues().stream()
        .map(v -> {
          if (v instanceof Integer) {
            return new BigDecimal((Integer)v);
          }
          if (v instanceof Double) {
            return new BigDecimal((Double)v);
          }
          if (v instanceof BigDecimal) {
            return (BigDecimal)v;
          }
          if (v instanceof BigInteger) {
            return new BigDecimal((BigInteger)v);
          }
          return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());

      switch (attr.getConditional()) {
        case EQUALS:
        case INCLUDES:
          return vals.get().stream().anyMatch(v -> v.equals(bd.get()));
        case ENDS_WITH:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.endsWith(v.toString()));
        case STARTS_WITH:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.startsWith(v.toString()));
        case GREATER:
          return vals.get().stream().anyMatch(v -> bd.get().compareTo(v) > 0 );
        case GREATER_EQUALS:
          return vals.get().stream().anyMatch(v -> bd.get().compareTo(v) >= 0 );
        case LESS:
          return vals.get().stream().anyMatch(v -> bd.get().compareTo(v) < 0 );
        case LESS_EQUALS:
          return vals.get().stream().anyMatch(v -> bd.get().compareTo(v) <= 0 );
        case NOT_EQUALS:
        case EXCLUDES:
          return vals.get().stream().noneMatch(v -> v.equals(bd.get()));
        case REGEX:
          return attr.getValues().stream().anyMatch(v -> v.toString().matches(suppliedValue));
      }
    } catch (Exception ignored) {

    }

    return false;
  }
}
