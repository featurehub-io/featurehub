package io.featurehub.strategies.matchers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Turns a supplied DATE or DATETIME context value into a UTC instant (rollout-strategies spec, matchers.DATE):
 * a date-only "YYYY-MM-DD" is midnight UTC, a date-time with an offset is converted to UTC, and a date-time
 * without an offset is taken as UTC (the server has no meaningful "local" time for a client).
 */
final class SuppliedDateTime {
  private SuppliedDateTime() {}

  /**
   * @return the instant, or null if the value is not an ISO-8601 date or date-time
   */
  static Instant parse(String value) {
    if (value == null) {
      return null;
    }

    try {
      return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
    } catch (Exception ignored) {
    }

    try {
      return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC);
    } catch (Exception ignored) {
    }

    try {
      return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneOffset.UTC).toInstant();
    } catch (Exception ignored) {
    }

    return null;
  }

  /**
   * @return the UTC date of the value, or null if it is not an ISO-8601 date or date-time
   */
  static LocalDate parseDate(String value) {
    Instant instant = parse(value);
    return instant == null ? null : LocalDate.ofInstant(instant, ZoneOffset.UTC);
  }
}
