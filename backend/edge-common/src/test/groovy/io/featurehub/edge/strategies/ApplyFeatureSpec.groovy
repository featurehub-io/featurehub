package io.featurehub.edge.strategies

import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.sse.model.FeatureRolloutStrategy
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute
import io.featurehub.strategies.matchers.MatcherRegistry
import io.featurehub.strategies.percentage.PercentageCalculator
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ApplyFeatureSpec extends Specification {
  ApplyFeature applyFeature
  PercentageCalculator percentageCalculator
  Integer pc

  def setup() {
    // mocking doesn't work for "where" clauses
    pc = 21
    percentageCalculator =  {String u, String id -> pc } as PercentageCalculator
//    percentageCalculator = new PC()
    applyFeature = new ApplyFeature(percentageCalculator, new MatcherRegistry())
  }

  @Unroll
  def "if the user's percent is calculated x% and we have a rollout strategy at 20%"() {
    given: "we have rollout strategies set"
      def rollout = [
        new FeatureRolloutStrategy()
          .id("1234")
          .percentage(underPercent)
          .value("blue")
      ]
    and:
      def cac = new ClientContext(false)
      cac.attributes.put(ClientContext.USERKEY, ['mary@mary.com'])
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, "key", UUID.randomUUID().toString(), cac)
    then:
      val.matched == matched
      val.value == expected
    where:
      underPercent || expected | matched
      22      || "blue"     | true
      75      || "blue"     | true
      15      || null   | false
      20      || null   | false
  }

  @Unroll
  def "if the user's percent is calculated x% and we have no rollout strategies"() {
    given: "we have rollout strategies set"
      def rollout = new ArrayList<FeatureRolloutStrategy>()
    and:
      def cac = new ClientContext(false)
      cac.attributes.put(ClientContext.USERKEY, ['mary@mary.com'])
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, "key", UUID.randomUUID().toString(), cac)
    then:
      val.matched == matched
      val.value == expected
    where:
      percent || expected | matched
      22      || null     | false
      15      || null     | false
      20      || null     | false
  }

  @Unroll
  def "We have rollout strategies but no user key"() {
    given: "we have rollout strategies set"
      def rollout = [
        new FeatureRolloutStrategy()
          .percentage(20)
          .value("blue")
      ]
    and:
      def cac = new ClientContext(false)
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, "key", UUID.randomUUID().toString(), cac)
    then:
      val.matched == matched
      val.value == expected
    where:
      percent || expected | matched
      22      || null     | false
      15      || null     | false
      20      || null     | false
  }

  def "a null ClientAttributeConnection generates the default value"() {
    given: "we have rollout strategies set"
      def rollout = [
        new FeatureRolloutStrategy()
          .percentage(20)
          .value("blue")
      ]
    and:
      def cac = new ClientContext(false)
    and:
      pc = null
    when: "i ask for the percentage"
      def val = applyFeature.applyFeature(rollout, "key", UUID.randomUUID().toString(), cac)
    then:
      !val.matched
  }

  @Unroll
  def 'array matcher works as expected'() {
    given:
      def rollout = [
        new FeatureRolloutStrategy().value("sausage")
          .id("1234")
          .percentage(0)
          .attributes([
            new FeatureRolloutStrategyAttribute()
              .fieldName("warehouseId")
              .conditional(conditional)
              .values(val)
              .type(RolloutStrategyFieldType.STRING)
          ])
      ]
    and:
      def calc = Mock(PercentageCalculator)
      def matcherRepo = new MatcherRegistry()
      def applier = new ApplyFeature(calc, matcherRepo)
    and:
      def cac = new ClientContext(true)
      cac.attributes[ClientContext.SESSIONKEY] = ['poorple']
      List<String> values = (suppliedValues instanceof String) ? [suppliedValues.toString()] : suppliedValues.toList()
      cac.attributes['warehouseId'] = values
    when:
      def theResult = applier.applyFeature(rollout, 'feature', 'id', cac)
    then:
      theResult.matched == result
    where:
      suppliedValues     | conditional                                        | val                          || result
      'Android'          | RolloutStrategyAttributeConditional.EQUALS         | ['Android']                  || true
      'Android'          | RolloutStrategyAttributeConditional.EQUALS         | ['android']                  || false
      'Android'          | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['Android']                  || false
      'Android'          | RolloutStrategyAttributeConditional.NOT_EQUALS     | ['android']                  || true
      'ios'              | RolloutStrategyAttributeConditional.INCLUDES       | ['android', 'ios', 'chrome'] || true
      'linux'            | RolloutStrategyAttributeConditional.INCLUDES       | ['android', 'ios', 'chrome'] || false
      'ios'              | RolloutStrategyAttributeConditional.EXCLUDES       | ['android', 'ios', 'chrome'] || false
      'linux'            | RolloutStrategyAttributeConditional.EXCLUDES       | ['android', 'ios', 'chrome'] || true
      'peaches'          | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['peaches']                  || true
      'peaches'          | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['zebras']                   || false
      'peaches'          | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['cream', 'zebras']          || true
      'actapus (gold)'   | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['mammal']                   || true
      'actapus (gold)'   | RolloutStrategyAttributeConditional.LESS           | ['mammal']                   || true
      'actapus (gold)'   | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['aardvark']                 || false
      '1'                | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['a', 'b']                   || true
      'c'                | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['a', 'b']                   || false
      'a'                | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['a', 'b']                   || true
      'b'                | RolloutStrategyAttributeConditional.LESS_EQUALS    | ['a', 'b']                   || true
      'actapus (gold)'   | RolloutStrategyAttributeConditional.LESS           | ['aardvark', 'aardshark']    || false
      'actapus (gold)'   | RolloutStrategyAttributeConditional.REGEX          | ['(.*)gold(.*)']              | true
      'actapus (gold)'   | RolloutStrategyAttributeConditional.REGEX          | ['(.*)purple(.*)']            | false
      ['ios', 'Android'] | RolloutStrategyAttributeConditional.EQUALS         | ['Android']                  || true
      ['ios', 'Android'] | RolloutStrategyAttributeConditional.EQUALS         | ['ios']                      || true
      ['ios', 'Android'] | RolloutStrategyAttributeConditional.EQUALS         | ['linux']                    || false
  }

  def "we have a CAC with no percentage data at all but a match via attributes"() {
    given: "we have a strategy set"
      def rollout = [
        new FeatureRolloutStrategy().value("sausage")
          .id("1234")
          .percentage(0)
          .attributes([
            new FeatureRolloutStrategyAttribute()
              .fieldName("warehouseId")
              .conditional(RolloutStrategyAttributeConditional.EQUALS)
              .values(['ponsonby'])
              .type(RolloutStrategyFieldType.STRING)
          ])
      ]
    and:
      def calc = Mock(PercentageCalculator)
      def matcherRepo = new MatcherRegistry()
      def applier = new ApplyFeature(calc, matcherRepo)

    and:
      def cac = new ClientContext(true)
      cac.attributes[ClientContext.SESSIONKEY] = ['poorple']
      cac.attributes['warehouseId'] = ['ponsonby']
    when:
      def result = applier.applyFeature(rollout, 'feature', 'id', cac)
    then:
        result.matched
      result.value == 'sausage'
  }

  static FeatureRolloutStrategyAttribute whenActive(String at,
                                                    RolloutStrategyAttributeConditional conditional = RolloutStrategyAttributeConditional.GREATER_EQUALS,
                                                    String fieldName = 'fhubWhenActive') {
    return new FeatureRolloutStrategyAttribute().fieldName(fieldName).type(RolloutStrategyFieldType.DATETIME)
      .conditional(conditional).values([at])
  }

  ApplyFeature applierAt(String clock) {
    return new ApplyFeature(percentageCalculator, new MatcherRegistry(), Clock.fixed(Instant.parse(clock), ZoneOffset.UTC))
  }

  @Unroll
  def "fhubWhenActive combined with another attribute: shop #shop at #clock matched=#matched"() {
    given:
      def rollout = [new FeatureRolloutStrategy().id("ling").value("kwong pastry").attributes([
        whenActive("2026-12-01T09:00:00Z"),
        new FeatureRolloutStrategyAttribute().fieldName("shop").type(RolloutStrategyFieldType.STRING)
          .conditional(RolloutStrategyAttributeConditional.EQUALS).values(["omu&co"])
      ])]
    and:
      def cac = new ClientContext(false)
      cac.attributes['shop'] = [shop]
    when:
      def val = applierAt(clock).applyFeature(rollout, "key", "fv-id", cac)
    then:
      val.matched == matched
      val.value == (matched ? "kwong pastry" : null)
    where:
      shop          | clock                  || matched
      "omu&co"      | "2026-12-02T00:00:00Z" || true
      "kwong pastry"| "2026-12-02T00:00:00Z" || false
      "omu&co"      | "2026-11-30T00:00:00Z" || false
  }

  @Unroll
  def "fhubWhenActive with a percentage at #clock matched=#matched"() {
    given:
      def rollout = [new FeatureRolloutStrategy().id("orm").value("chago").percentage(1000000)
        .attributes([whenActive("2026-12-01T09:00:00Z")])]
    and:
      def cac = new ClientContext(false)
      cac.attributes.put(ClientContext.USERKEY, ['kornaphat'])
    when:
      def val = applierAt(clock).applyFeature(rollout, "key", "fv-id", cac)
    then:
      val.matched == matched
    where:
      clock                  || matched
      "2026-12-02T00:00:00Z" || true
      "2026-11-30T00:00:00Z" || false
  }

  def "a strategy with only fhubWhenActive matches everyone once the time is reached, and LESS turns it off"() {
    given:
      def rollout = [
        new FeatureRolloutStrategy().id("off").value("off").attributes([
          whenActive("2026-12-01T09:00:00Z", RolloutStrategyAttributeConditional.LESS)]),
        new FeatureRolloutStrategy().id("on").value("on").attributes([
          whenActive("2026-12-01T09:00:00Z", RolloutStrategyAttributeConditional.GREATER_EQUALS, 'FHUBWHENACTIVE')])
      ]
    and:
      def cac = new ClientContext(false)
    expect:
      applierAt("2026-12-01T08:59:59.999Z").applyFeature(rollout, "key", "fv-id", cac).value == "off"
      applierAt("2026-12-01T09:00:00Z").applyFeature(rollout, "key", "fv-id", cac).value == "on"
  }

  def "the client context cannot supply fhubWhenActive or now to change the result"() {
    given:
      def rollout = [new FeatureRolloutStrategy().id("ling").value("sirilak")
        .attributes([whenActive("2026-12-01T09:00:00Z")])]
    and:
      def cac = new ClientContext(false)
      cac.attributes['fhubWhenActive'] = ['2020-01-01T00:00:00Z']
      cac.attributes['fhubwhenactive'] = ['2030-01-01T00:00:00Z']
      cac.attributes['now'] = ['2030-01-01T00:00:00Z']
    when:
      def val = applierAt("2026-11-30T00:00:00Z").applyFeature(rollout, "key", "fv-id", cac)
    then:
      !val.matched
  }

  @Unroll
  def "now is generated in UTC for #type #conditional #values at #clock"() {
    given:
      def rollout = [new FeatureRolloutStrategy().id("orm").value("always wonder").attributes([
        new FeatureRolloutStrategyAttribute().fieldName(fieldName).type(type).conditional(conditional).values(values)
      ])]
    and:
      def cac = new ClientContext(false)
      if (supplied != null) cac.attributes[fieldName] = supplied
    when:
      def val = applierAt(clock).applyFeature(rollout, "key", "fv-id", cac)
    then:
      val.matched == matched
    where:
      fieldName | type                              | conditional                                        | values                   | supplied | clock                       || matched
      'now'     | RolloutStrategyFieldType.DATETIME | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['2026-12-01T09:00:00Z'] | null     | '2026-12-01T09:00:00Z'      || true
      'now'     | RolloutStrategyFieldType.DATETIME | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['2026-12-01T09:00:00Z'] | null     | '2026-12-01T08:59:59Z'      || false
      'NOW'     | RolloutStrategyFieldType.DATETIME | RolloutStrategyAttributeConditional.LESS           | ['2026-12-01T09:00:00Z'] | []       | '2026-12-01T08:59:59Z'      || true
      // milliseconds are dropped by the DATETIME matcher
      'now'     | RolloutStrategyFieldType.DATETIME | RolloutStrategyAttributeConditional.EQUALS         | ['2026-12-01T09:00:00Z'] | null     | '2026-12-01T09:00:00.750Z'  || true
      'now'     | RolloutStrategyFieldType.DATETIME | RolloutStrategyAttributeConditional.EQUALS         | ['2026-12-01T21:00:00+12:00'] | null | '2026-12-01T09:00:00Z'     || true
      // the date is the UTC date: 23:30Z on the 1st is already the 2nd in New Zealand
      'now'     | RolloutStrategyFieldType.DATE     | RolloutStrategyAttributeConditional.EQUALS         | ['2026-12-01']           | null     | '2026-12-01T23:30:00Z'      || true
      'now'     | RolloutStrategyFieldType.DATE     | RolloutStrategyAttributeConditional.GREATER        | ['2026-12-01']           | null     | '2026-12-02T00:00:00Z'      || true
      'now'     | RolloutStrategyFieldType.DATE     | RolloutStrategyAttributeConditional.GREATER        | ['2026-12-01']           | null     | '2026-12-01T23:59:59Z'      || false
      // a client supplied "now" is still used
      'now'     | RolloutStrategyFieldType.DATETIME | RolloutStrategyAttributeConditional.GREATER_EQUALS | ['2026-12-01T09:00:00Z'] | ['2030-01-01T00:00:00Z'] | '2026-11-01T00:00:00Z' || true
  }

  @Unroll
  def "fhubWhenActive with an unsupported conditional #conditional is not skipped: the strategy never applies"() {
    given:
      def rollout = [new FeatureRolloutStrategy().id("ling").value("kwong pastry").attributes([
        whenActive(value, conditional),
        new FeatureRolloutStrategyAttribute().fieldName("shop").type(RolloutStrategyFieldType.STRING)
          .conditional(RolloutStrategyAttributeConditional.EQUALS).values(["omu&co"])
      ])]
    and:
      def cac = new ClientContext(false)
      cac.attributes['shop'] = ['omu&co']
    expect:
      !applierAt("2027-01-01T00:00:00Z").applyFeature(rollout, "key", "fv-id", cac).matched
    where:
      conditional                                     | value
      RolloutStrategyAttributeConditional.INCLUDES    | "2026-12-01T09:00:00Z"
      RolloutStrategyAttributeConditional.EXCLUDES    | "2026-12-01T09:00:00Z"
      RolloutStrategyAttributeConditional.STARTS_WITH | "2026"
      RolloutStrategyAttributeConditional.ENDS_WITH   | "Z"
      RolloutStrategyAttributeConditional.REGEX       | ".*"
  }
}
