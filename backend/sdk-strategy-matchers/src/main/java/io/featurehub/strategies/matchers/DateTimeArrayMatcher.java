package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.FeatureRolloutStrategyAttribute;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Compares a supplied date-time against the attribute's date-times as UTC instants, to the second
 * (milliseconds are dropped). A supplied value without an offset is taken as UTC.
 */
public class DateTimeArrayMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, FeatureRolloutStrategyAttribute attr) {
    try {
      Instant supplied = toSeconds(SuppliedDateTime.parse(suppliedValue));

      if (supplied == null) {
        return false;
      }

      switch (attr.getConditional()) {
        case EQUALS: // all match makes no sense
        case INCLUDES: // same as equals
          return attr.getValues().stream()
            .anyMatch(v -> supplied.equals(value(v)));
        case ENDS_WITH:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.endsWith(v.toString()));
        case STARTS_WITH:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.startsWith(v.toString()));
        case GREATER:
          return attr.getValues().stream()
            .anyMatch(v -> supplied.compareTo(value(v)) > 0);
        case GREATER_EQUALS:
          return attr.getValues().stream()
            .anyMatch(v -> supplied.compareTo(value(v)) >= 0);
        case LESS:
          return attr.getValues().stream()
            .anyMatch(v -> supplied.compareTo(value(v)) < 0);
        case LESS_EQUALS:
          return attr.getValues().stream()
            .anyMatch(v -> supplied.compareTo(value(v)) <= 0);
        case NOT_EQUALS:
        case EXCLUDES: // same as not_equals
          return attr.getValues().stream()
            .noneMatch(v -> supplied.equals(value(v)));
        case REGEX:
          return attr.getValues().stream().anyMatch(v -> suppliedValue.matches(v.toString()));
      }
    } catch (Exception ignored) {
    }

    return false;
  }

  private static Instant toSeconds(Instant instant) {
    return instant == null ? null : instant.truncatedTo(ChronoUnit.SECONDS);
  }

  private static Instant value(Object v) {
    Instant instant = toSeconds(SuppliedDateTime.parse(v.toString()));
    if (instant == null) {
      throw new IllegalArgumentException("not a date-time");
    }
    return instant;
  }
}
