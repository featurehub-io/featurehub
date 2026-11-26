package io.featurehub.strategies.matchers

import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

import static io.featurehub.mr.model.RolloutStrategyAttributeConditional.*

/**
 * The attribute vectors from rollout-strategies.yaml (1.2.0) well_known_attributes.fhubWhenActive.
 */
class WhenActiveMatcherSpec extends Specification {
  static final String AT = "2026-12-01T09:00:00Z"

  static boolean matchAt(String clock, RolloutStrategyAttributeConditional conditional, List<Object> values,
                         RolloutStrategyFieldType type = RolloutStrategyFieldType.DATETIME) {
    def matcher = new WhenActiveMatcher(Clock.fixed(Instant.parse(clock), ZoneOffset.UTC))
    return matcher.match(new FeatureRolloutStrategyAttribute()
      .fieldName("fhubWhenActive").type(type).conditional(conditional).values(values))
  }

  @Unroll
  def "#conditional against #values at #clock is #result"() {
    expect:
      matchAt(clock, conditional, values, type) == result
    where:
      conditional    | type                            | values         | clock                      || result
      GREATER_EQUALS | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T08:59:59.999Z" || false
      GREATER_EQUALS | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T09:00:00.000Z" || true
      GREATER_EQUALS | RolloutStrategyFieldType.DATETIME | [AT]         | "2027-01-01T00:00:00Z"     || true
      GREATER_EQUALS | RolloutStrategyFieldType.STRING   | [AT]         | "2026-12-02T00:00:00Z"     || true
      GREATER        | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T09:00:00.000Z" || false
      LESS           | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T08:59:59.999Z" || true
      LESS           | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T09:00:00.000Z" || false
      LESS_EQUALS    | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T09:00:00.000Z" || true
      EQUALS         | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T09:00:00.000Z" || true
      EQUALS         | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T09:00:00.999Z" || true
      EQUALS         | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T09:00:01Z"     || false
      NOT_EQUALS     | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T09:00:00.500Z" || false
      NOT_EQUALS     | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T09:00:01Z"     || true
      GREATER        | RolloutStrategyFieldType.DATETIME | [AT]         | "2026-12-01T09:00:00.999Z" || false
      INCLUDES       | RolloutStrategyFieldType.DATETIME | [AT]         | "2027-01-01T00:00:00Z"     || false
      EXCLUDES       | RolloutStrategyFieldType.DATETIME | [AT]         | "2027-01-01T00:00:00Z"     || false
      REGEX          | RolloutStrategyFieldType.DATETIME | [AT]         | "2027-01-01T00:00:00Z"     || false
      STARTS_WITH    | RolloutStrategyFieldType.DATETIME | [AT]         | "2027-01-01T00:00:00Z"     || false
      ENDS_WITH      | RolloutStrategyFieldType.DATETIME | [AT]         | "2027-01-01T00:00:00Z"     || false
      NOT_EQUALS     | RolloutStrategyFieldType.DATETIME | []           | "2027-01-01T00:00:00Z"     || false
      GREATER_EQUALS | RolloutStrategyFieldType.DATETIME | []           | "2026-12-02T00:00:00Z"     || false
      GREATER_EQUALS | RolloutStrategyFieldType.DATETIME | ["next tuesday"] | "2026-12-02T00:00:00Z" || false
      // explicit offsets are converted to UTC: 21:00+12:00 is 09:00Z
      GREATER_EQUALS | RolloutStrategyFieldType.DATETIME | ["2026-12-01T21:00:00+12:00"] | "2026-12-01T09:00:00Z" || true
      LESS           | RolloutStrategyFieldType.DATETIME | ["2026-12-01T21:00:00+12:00"] | "2026-12-01T09:00:00Z" || false
      // fractional seconds are dropped on both sides: compared to the second
      GREATER_EQUALS | RolloutStrategyFieldType.DATETIME | ["2026-12-01T09:00:00.750Z"] | "2026-12-01T09:00:00.100Z" || true
      GREATER_EQUALS | RolloutStrategyFieldType.DATETIME | ["2026-12-01T09:00:00.750Z"] | "2026-12-01T08:59:59.999Z" || false
      // no offset is not a valid value
      GREATER_EQUALS | RolloutStrategyFieldType.DATETIME | ["2026-12-01T09:00:00"]      | "2027-01-01T00:00:00Z"     || false
  }

  def "several values: invalid and null ones are dropped, GREATER_EQUALS uses the earliest and LESS the latest"() {
    given:
      def values = [null, "keep silent", "2026-12-05T00:00:00Z", AT]
    expect:
      matchAt("2026-12-02T00:00:00Z", GREATER_EQUALS, values)
      matchAt("2026-12-02T00:00:00Z", LESS, values)
      !matchAt("2026-12-06T00:00:00Z", LESS, values)
      !matchAt(AT, NOT_EQUALS, values)
      matchAt("2026-12-02T00:00:00Z", NOT_EQUALS, values)
  }

  def "null values or conditional never match and never throw"() {
    given:
      def matcher = new WhenActiveMatcher()
    expect:
      !matcher.match(new FeatureRolloutStrategyAttribute().fieldName("fhubWhenActive").conditional(GREATER_EQUALS).values(null))
      !matcher.match(new FeatureRolloutStrategyAttribute().fieldName("fhubWhenActive").conditional(null).values([AT]))
  }

  def "the field name is recognised case-insensitively"() {
    expect:
      WhenActiveMatcher.isWhenActive(name) == result
    where:
      name             || result
      "fhubWhenActive" || true
      "FHUBWHENACTIVE" || true
      "fhubwhenactive" || true
      "now"            || false
      "ormkorn"        || false
      null             || false
  }
}
