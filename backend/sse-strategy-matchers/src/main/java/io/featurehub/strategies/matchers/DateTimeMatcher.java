package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.RolloutStrategyAttribute;
import io.featurehub.sse.model.RolloutStrategyAttributeConditional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class DateTimeMatcher implements StrategyMatcher {

  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    try {
      if (attr.getConditional() == RolloutStrategyAttributeConditional.REGEX) {
        return suppliedValue.matches(attr.getValue().toString());
      }

      DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

      Supplier<OffsetDateTime> suppliedDate = () -> OffsetDateTime.from(formatter.parse(suppliedValue));

      Supplier<OffsetDateTime> val = () -> OffsetDateTime.from(formatter.parse(attr.getValue().toString()));

      switch (attr.getConditional()) {
        case EQUALS:
        case INCLUDES:
          return suppliedDate.get().equals(val.get());
        case ENDS_WITH:
          return suppliedValue.endsWith(attr.getValue().toString());
        case STARTS_WITH:
          return suppliedValue.startsWith(attr.getValue().toString());
        case GREATER:
          return suppliedDate.get().compareTo(val.get()) > 0;
        case GREATER_EQUALS:
          return suppliedDate.get().compareTo(val.get()) >= 0;
        case LESS:
          return suppliedDate.get().compareTo(val.get()) < 0;
        case LESS_EQUALS:
          return suppliedDate.get().compareTo(val.get()) <= 0;
        case NOT_EQUALS:
        case EXCLUDES:
          return !suppliedDate.get().equals(val.get());
        case REGEX:
          return suppliedValue.matches(attr.getValue().toString());
      }
    } catch (Exception ignored) {
    }

    return false;
  }
}
