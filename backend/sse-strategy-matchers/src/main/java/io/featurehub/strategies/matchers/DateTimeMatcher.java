package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.RolloutStrategyAttribute;
import io.featurehub.sse.model.RolloutStrategyAttributeConditional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeMatcher implements StrategyMatcher {

  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    try {
      if (attr.getConditional() == RolloutStrategyAttributeConditional.REGEX) {
        return suppliedValue.matches(attr.getValue().toString());
      }

      DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

      LocalDateTime suppliedDate = LocalDateTime.from(formatter.parse(suppliedValue));

      LocalDateTime val = LocalDateTime.from(formatter.parse(attr.getValue().toString()));

      switch (attr.getConditional()) {
        case EQUALS:
          return suppliedDate.equals(val);
        case ENDS_WITH:
          break;
        case STARTS_WITH:
          break;
        case GREATER:
          return suppliedDate.compareTo(val) > 0;
        case GREATER_EQUALS:
          return suppliedDate.compareTo(val) >= 0;
        case LESS:
          return suppliedDate.compareTo(val) < 0;
        case LESS_EQUALS:
          return suppliedDate.compareTo(val) <= 0;
        case NOT_EQUALS:
          return !suppliedDate.equals(val);
        case INCLUDES:
          break;
        case EXCLUDES:
          break;
        case REGEX:
          break;
      }
    } catch (Exception ignored) {}

    return false;
  }
}
