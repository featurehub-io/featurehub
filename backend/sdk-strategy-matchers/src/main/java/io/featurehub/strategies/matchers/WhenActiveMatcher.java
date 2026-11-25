package io.featurehub.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttributeConditional;
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates the well-known fhubWhenActive strategy attribute (rollout-strategies spec 1.2.0,
 * well_known_attributes.fhubWhenActive). The attribute's values hold ISO-8601 date-times and the
 * clock is compared against them with the attribute's conditional, the clock on the left. The attribute's type is
 * ignored and nothing is ever taken from the client context. Both the values and the clock are truncated to the whole
 * second (strategy evaluation never works below the second), then compared as instants, not strings. It never throws.
 */
public class WhenActiveMatcher {
  public static final String FIELD_NAME = "fhubWhenActive";

  private final Clock clock;

  public WhenActiveMatcher() {
    this(Clock.systemUTC());
  }

  public WhenActiveMatcher(Clock clock) {
    this.clock = clock;
  }

  public static boolean isWhenActive(String fieldName) {
    return FIELD_NAME.equalsIgnoreCase(fieldName);
  }

  public boolean match(FeatureRolloutStrategyAttribute attr) {
    return match(attr, clock.instant());
  }

  /**
   * @param at the time to evaluate at, so a caller can use one instant for a whole request
   */
  public boolean match(FeatureRolloutStrategyAttribute attr, Instant at) {
    RolloutStrategyAttributeConditional conditional = attr.getConditional();
    List<Object> rawValues = attr.getValues();

    if (conditional == null || rawValues == null) {
      return false;
    }

    List<Instant> values = rawValues.stream()
      .filter(Objects::nonNull)
      .map(WhenActiveMatcher::parse)
      .filter(Objects::nonNull)
      .toList();

    // no values that are valid date-times never match, NOT_EQUALS included
    if (values.isEmpty()) {
      return false;
    }

    Instant now = at.truncatedTo(ChronoUnit.SECONDS);

    return switch (conditional) {
      case EQUALS -> values.stream().anyMatch(now::equals);
      case NOT_EQUALS -> values.stream().noneMatch(now::equals);
      case GREATER -> values.stream().anyMatch(now::isAfter);
      case GREATER_EQUALS -> values.stream().anyMatch(v -> !now.isBefore(v));
      case LESS -> values.stream().anyMatch(now::isBefore);
      case LESS_EQUALS -> values.stream().anyMatch(v -> !now.isAfter(v));
      // INCLUDES, EXCLUDES, STARTS_WITH, ENDS_WITH, REGEX mean nothing for a moment in time
      default -> false;
    };
  }

  /**
   * @return the value as an instant truncated to the second, or null if it is not an ISO-8601 date-time with an offset (UTC "Z" or explicit)
   */
  public static Instant parse(Object value) {
    try {
      return OffsetDateTime.parse(value.toString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        .truncatedTo(ChronoUnit.SECONDS);
    } catch (Exception ignored) {
      return null;
    }
  }
}
