package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.RolloutStrategyAttribute;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class DateTimeArrayMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    try {
      Supplier<OffsetDateTime> suppliedDate = () -> OffsetDateTime.from(formatter.parse(suppliedValue));

      switch (attr.getConditional()) {
        case EQUALS: // all match makes no sense
        case INCLUDES: // same as equals
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.get().equals(OffsetDateTime.from(formatter.parse(v.toString()))));
        case ENDS_WITH:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.endsWith(v.toString()));
        case STARTS_WITH:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.startsWith(v.toString()));
        case GREATER:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.get().compareTo(OffsetDateTime.from(formatter.parse(v.toString()))) > 0);
        case GREATER_EQUALS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.get().compareTo(OffsetDateTime.from(formatter.parse(v.toString()))) >= 0);
        case LESS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.get().compareTo(OffsetDateTime.from(formatter.parse(v.toString()))) < 0);
        case LESS_EQUALS:
          return attr.getValues().stream()
            .anyMatch(v -> suppliedDate.get().compareTo(OffsetDateTime.from(formatter.parse(v.toString()))) <= 0);
        case NOT_EQUALS:
        case EXCLUDES: // same as not_equals
          return attr.getValues().stream()
            .noneMatch(v -> suppliedDate.get().equals(OffsetDateTime.from(formatter.parse(v.toString()))));
        case REGEX:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.matches(v.toString()));
      }
    } catch (Exception ignored) {
      ignored.printStackTrace();
    }

    return false;
  }
}
