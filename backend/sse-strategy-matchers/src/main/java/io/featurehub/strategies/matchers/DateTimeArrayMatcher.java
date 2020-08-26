package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.RolloutStrategyAttribute;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateTimeArrayMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    try {
      LocalDate suppliedDate = LocalDate.from(formatter.parse(suppliedValue));

      switch (attr.getConditional()) {
        case EQUALS: // all match makes no sense
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.equals(LocalDate.from(formatter.parse(v.toString()))));
        case ENDS_WITH:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.endsWith(v.toString()));
        case STARTS_WITH:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.startsWith(v.toString()));
        case GREATER:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.compareTo(LocalDate.from(formatter.parse(v.toString()))) > 0);
        case GREATER_EQUALS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.compareTo(LocalDate.from(formatter.parse(v.toString()))) >= 0);
        case LESS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.compareTo(LocalDate.from(formatter.parse(v.toString()))) < 0);
        case LESS_EQUALS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.compareTo(LocalDate.from(formatter.parse(v.toString()))) <= 0);
        case NOT_EQUALS:
          return attr.getValues().stream()
            .noneMatch(v -> suppliedDate.equals(LocalDate.from(formatter.parse(v.toString()))));
        case INCLUDES: // same as equals
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.equals(LocalDate.from(formatter.parse(v.toString()))));
        case EXCLUDES: // same as not_equals
          return attr.getValues().stream()
            .noneMatch(v -> suppliedDate.equals(LocalDate.from(formatter.parse(v.toString()))));
        case REGEX:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.matches(v.toString()));
      }
    } catch (Exception ignored) {
    }

    return false;
  }
}
