package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.FeatureRolloutStrategyAttribute;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class DateArrayMatcher implements StrategyMatcher {
  private LocalDate supplied;

  @Override
  public boolean match(String suppliedValue, FeatureRolloutStrategyAttribute attr) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

      Supplier<LocalDate> suppliedDate = () -> {
        if (supplied == null) {
          supplied = LocalDate.from(formatter.parse(suppliedValue));
        }

        return supplied;
      };

      switch (attr.getConditional()) {
        case EQUALS: // all match makes no sense
        case INCLUDES: // same as equals
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.get().equals(LocalDate.from(formatter.parse(v.toString()))));
        case ENDS_WITH:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.endsWith(v.toString()));
        case STARTS_WITH:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.startsWith(v.toString()));
        case GREATER:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.get().compareTo(LocalDate.from(formatter.parse(v.toString()))) > 0);
        case GREATER_EQUALS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.get().compareTo(LocalDate.from(formatter.parse(v.toString()))) >= 0);
        case LESS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.get().compareTo(LocalDate.from(formatter.parse(v.toString()))) < 0);
        case LESS_EQUALS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.get().compareTo(LocalDate.from(formatter.parse(v.toString()))) <= 0);
        case NOT_EQUALS:
        case EXCLUDES: // same as not_equals
          return attr.getValues().stream()
            .noneMatch(v -> suppliedDate.get().equals(LocalDate.from(formatter.parse(v.toString()))));
        case REGEX:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.matches(v.toString()));
      }
    } catch (Exception ignored) {
    }
    return false;
  }
}
