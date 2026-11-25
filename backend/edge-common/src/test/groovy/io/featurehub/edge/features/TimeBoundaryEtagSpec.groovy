package io.featurehub.edge.features

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeatureValue
import io.featurehub.dacha.model.CacheRolloutStrategy
import io.featurehub.dacha.model.CacheRolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TimeBoundaryEtagSpec extends Specification {
  static CacheRolloutStrategyAttribute attr(String fieldName, RolloutStrategyFieldType type, List<Object> values) {
    return new CacheRolloutStrategyAttribute().fieldName(fieldName).type(type)
      .conditional(RolloutStrategyAttributeConditional.GREATER_EQUALS).values(values)
  }

  static List<CacheEnvironmentFeature> features(CacheRolloutStrategyAttribute... attrs) {
    return [
      new CacheEnvironmentFeature(), // no value
      new CacheEnvironmentFeature().value(new CacheFeatureValue()), // no strategies
      new CacheEnvironmentFeature().value(new CacheFeatureValue().rolloutStrategies([
        new CacheRolloutStrategy().id("orm").attributes(attrs.toList())
      ]))
    ]
  }

  static String etagAt(String clock, List<CacheEnvironmentFeature> features) {
    return TimeBoundaryEtag.INSTANCE.etag("kwong", features, Clock.fixed(Instant.parse(clock), ZoneOffset.UTC))
  }

  def "environments without clock based strategies keep Dacha's etag"() {
    expect:
      etagAt("2026-12-01T09:00:00Z", null) == "kwong"
      etagAt("2026-12-01T09:00:00Z", features(attr("ormkorn", RolloutStrategyFieldType.STRING, ["keep silent"]))) == "kwong"
      // a "now" attribute of a non date type is not clock based
      etagAt("2026-12-01T09:00:00Z", features(attr("now", RolloutStrategyFieldType.STRING, ["2026-12-01T09:00:00Z"]))) == "kwong"
      // unparseable values add nothing
      etagAt("2026-12-01T09:00:00Z", features(attr("fhubWhenActive", RolloutStrategyFieldType.DATETIME, ["chago", null]))) == "kwong"
  }

  @Unroll
  def "fhubWhenActive flips at the second of the time and at the next second: #clock -> #etag"() {
    expect:
      etagAt(clock, features(attr("FHUBWHENACTIVE", RolloutStrategyFieldType.STRING, ["2026-12-01T09:00:00Z"]))) == etag
    where:
      clock                            || etag
      "2026-12-01T08:59:59.999999999Z" || "kwong_t0"
      "2026-12-01T09:00:00Z"           || "kwong_t1"
      "2026-12-01T09:00:00.999Z"       || "kwong_t1"
      "2026-12-01T09:00:01Z"           || "kwong_t2"
      "2027-01-01T00:00:00Z"           || "kwong_t2"
  }

  @Unroll
  def "now DATETIME flips at the second and the next one: #clock -> #etag"() {
    expect:
      etagAt(clock, features(attr("now", RolloutStrategyFieldType.DATETIME, ["2026-12-01T09:00:00.500Z"]))) == etag
    where:
      clock                      || etag
      "2026-12-01T08:59:59.999Z" || "kwong_t0"
      "2026-12-01T09:00:00Z"     || "kwong_t1"
      "2026-12-01T09:00:00.999Z" || "kwong_t1"
      "2026-12-01T09:00:01Z"     || "kwong_t2"
  }

  @Unroll
  def "now DATE flips at UTC midnight of the date and of the next day: #clock -> #etag"() {
    expect:
      etagAt(clock, features(attr("Now", RolloutStrategyFieldType.DATE, ["2026-12-01"]))) == etag
    where:
      clock                  || etag
      "2026-11-30T23:59:59Z" || "kwong_t0"
      "2026-12-01T00:00:00Z" || "kwong_t1"
      "2026-12-01T23:59:59Z" || "kwong_t1"
      "2026-12-02T00:00:00Z" || "kwong_t2"
  }

  def "boundaries across several strategies are counted once each"() {
    given:
      def fs = features(
        attr("fhubWhenActive", RolloutStrategyFieldType.DATETIME, ["2026-12-01T09:00:00Z", "2026-12-05T09:00:00Z"]),
        attr("fhubWhenActive", RolloutStrategyFieldType.DATETIME, ["2026-12-01T09:00:00Z"]))
    expect:
      TimeBoundaryEtag.INSTANCE.boundaries(fs).size() == 4
      etagAt("2026-12-03T00:00:00Z", fs) == "kwong_t2"
      etagAt("2026-12-06T00:00:00Z", fs) == "kwong_t4"
  }

  @Unroll
  def "fhubWhenActive with #conditional never matches, so it adds no boundaries"() {
    given:
      def a = attr("fhubWhenActive", RolloutStrategyFieldType.DATETIME, ["2026-12-01T09:00:00Z"]).conditional(conditional)
    expect:
      etagAt("2027-01-01T00:00:00Z", features(a)) == "kwong"
    where:
      conditional << [RolloutStrategyAttributeConditional.INCLUDES, RolloutStrategyAttributeConditional.EXCLUDES,
                      RolloutStrategyAttributeConditional.STARTS_WITH, RolloutStrategyAttributeConditional.ENDS_WITH,
                      RolloutStrategyAttributeConditional.REGEX]
  }
}
