package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.RolloutStrategyAttribute;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateArrayMatcher implements StrategyMatcher {
  private final DateTimeFormatter formatter;

  public DateArrayMatcher(DateTimeFormatter formatter) {
    this.formatter = formatter;
  }

  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    try {
      LocalDateTime suppliedDate = LocalDateTime.from(formatter.parse(suppliedValue));

      switch (attr.getConditional()) {
        case EQUALS: // all match makes no sense
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.equals(LocalDateTime.from(formatter.parse(v.toString()))));
        case ENDS_WITH:
          break;
        case STARTS_WITH:
          break;
        case GREATER:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.compareTo(LocalDateTime.from(formatter.parse(v.toString()))) > 0);
        case GREATER_EQUALS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.compareTo(LocalDateTime.from(formatter.parse(v.toString()))) >= 0);
        case LESS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.compareTo(LocalDateTime.from(formatter.parse(v.toString()))) < 0);
        case LESS_EQUALS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.compareTo(LocalDateTime.from(formatter.parse(v.toString()))) <= 0);
        case NOT_EQUALS:
          return attr.getValues().stream()
            .noneMatch(v -> suppliedDate.equals(LocalDateTime.from(formatter.parse(v.toString()))));
        case INCLUDES: // same as equals
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.equals(LocalDateTime.from(formatter.parse(v.toString()))));
        case EXCLUDES: // same as not_equals
          return attr.getValues().stream()
            .noneMatch(v -> suppliedDate.equals(LocalDateTime.from(formatter.parse(v.toString()))));
        case REGEX:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.matches(v.toString()));
      }
    } catch (Exception e) {}
    return false;
  }
}
