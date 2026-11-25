package io.featurehub.edge.features

import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.dacha.model.DachaKeyDetailsResponse
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeatureValue
import io.featurehub.dacha.model.CacheRolloutStrategy
import io.featurehub.dacha.model.CacheRolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.jersey.config.JacksonObjectProvider
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

import java.util.concurrent.CompletableFuture

class FeatureRequestCollectionSpec extends Specification {
  FeatureTransformer transformer
  ClientContext context
  CompletableFuture<List<FeatureRequestResponse>> future
  List<FeatureRequestResponse> responses
  KeyParts kp1
  KeyParts kp2
//  EtagStructureHolder

  def setup() {
    transformer = Mock(FeatureTransformer)
    context = new ClientContext(false)
    future = new CompletableFuture<List<FeatureRequestResponse>>()
    responses = []
    kp1 = new KeyParts("c1", UUID.randomUUID(), "1")
    kp2 = new KeyParts("c1", UUID.randomUUID(), "1")
  }

  def "if the etag holder is invalid, we will get success/failed mix appropriately"() {
    given: "we have an invalid etag holder"
      def holder = new EtagStructureHolder(Map.of(kp1, "x", kp2, "y"), "0", false)
    and: "we have created our collection"
      def coll = new FeatureRequestCollection(2, transformer, context, future, holder)
    when: "we drop our requests in"
      def r1 = Mock(FeatureRequester)
      r1.key >> kp1
      def r2 = Mock(FeatureRequester)
      r2.key >> kp2
      coll.complete(r1)
      coll.complete(r2)
    and: "we wait for the future"
      def responses = future.get()
    then:
      responses.size() == 2
      responses[0].success == FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE
      responses[1].success == FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE
  }

  def "if all requests complete and have the same etags, we will get a NO_CHANGE response"() {
    given: "we have an valid etag holder"
      def holder = new EtagStructureHolder(Map.of(kp1, "x", kp2, "y"), "0", true)
    and: "we have created our collection"
      def coll = new FeatureRequestCollection(2, transformer, context, future, holder)
    when: "we drop our requests in"
      def r1 = Mock(FeatureRequester)
      def details1 = new DachaKeyDetailsResponse().etag("x")
      r1.key >> kp1
      r1.details >> details1
      def r2 = Mock(FeatureRequester)
      def details2 = new DachaKeyDetailsResponse().etag("y")
      r2.key >> kp2
      r2.details >> details2
      coll.complete(r1)
      coll.complete(r2)
    and: "we wait for the future"
      def responses = future.get()
    then:
      responses.size() == 2
      responses[0].success == FeatureRequestSuccess.NO_CHANGE
      responses[1].success == FeatureRequestSuccess.NO_CHANGE
  }

  def "if all requests complete and at least one has a different etag, we will get a SUCCESS response"() {
    given: "we have an valid etag holder"
      def holder = new EtagStructureHolder(Map.of(kp1, "x", kp2, "y"), "0", true)
    and: "we have created our collection"
      def coll = new FeatureRequestCollection(2, transformer, context, future, holder)
    when: "we drop our requests in"
      def r1 = Mock(FeatureRequester)
      def details1 = new DachaKeyDetailsResponse().etag("x").features([])
      r1.key >> kp1
      r1.details >> details1
      def r2 = Mock(FeatureRequester)
      def details2 = new DachaKeyDetailsResponse().etag("z").features([]) // different
      r2.key >> kp2
      r2.details >> details2
      coll.complete(r1)
      coll.complete(r2)
    and: "we wait for the future"
      def responses = future.get()
    then:
      responses.size() == 2
      responses[0].success == FeatureRequestSuccess.SUCCESS
      responses[0].etag == 'x'
      responses[1].success == FeatureRequestSuccess.SUCCESS
      responses[1].etag == 'z'
      2 * transformer.transform([], context, false, _) >> []
  }

  def "if any requests failed, the ones that are successful will get a success response even if their etags are the same"() {
    given: "we have an valid etag holder"
      def holder = new EtagStructureHolder(Map.of(kp1, "x", kp2, "y"), "0", true)
    and: "we have created our collection"
      def coll = new FeatureRequestCollection(2, transformer, context, future, holder)
    when: "we drop our requests in"
      def r1 = Mock(FeatureRequester)
      def details1 = new DachaKeyDetailsResponse().etag("x").features([])
      r1.key >> kp1
      r1.details >> null // request failed
      def r2 = Mock(FeatureRequester)
      def details2 = new DachaKeyDetailsResponse().etag("z").features([]) // different
      r2.key >> kp2
      r2.details >> details2
      coll.complete(r1)
      coll.complete(r2)
    and: "we wait for the future"
      def responses = future.get()
    then:
      responses.size() == 2
      responses[0].success == FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE
      responses[0].etag == '0'
      responses[1].success == FeatureRequestSuccess.SUCCESS
      responses[1].etag == 'z'
      1 * transformer.transform([], context, false, _) >> []
  }

  static List<CacheEnvironmentFeature> whenActiveFeatures(String at) {
    return [new CacheEnvironmentFeature().value(new CacheFeatureValue().rolloutStrategies([
      new CacheRolloutStrategy().id("ling").attributes([
        new CacheRolloutStrategyAttribute().fieldName("fhubWhenActive").type(RolloutStrategyFieldType.DATETIME)
          .conditional(RolloutStrategyAttributeConditional.GREATER_EQUALS).values([at])
      ])
    ]))]
  }

  List<FeatureRequestResponse> requestAt(ClientContext ctx, String clock, String incomingEtag, boolean valid = true) {
    def holder = new EtagStructureHolder(incomingEtag == null ? [:] : Map.of(kp1, incomingEtag), "0", valid)
    def coll = new FeatureRequestCollection(1, transformer, ctx, future, holder,
      Clock.fixed(Instant.parse(clock), ZoneOffset.UTC))
    def r1 = Mock(FeatureRequester)
    r1.key >> kp1
    r1.details >> new DachaKeyDetailsResponse().etag("kwong").features(whenActiveFeatures("2026-12-01T09:00:00Z"))
    coll.complete(r1)
    return future.get()
  }

  def "a server evaluated etag records which side of a fhubWhenActive time the clock is on"() {
    when: "a full request before the time"
      def before = requestAt(context, "2026-12-01T08:59:59Z", null, false)
    then:
      before[0].success == FeatureRequestSuccess.SUCCESS
      before[0].etag == "kwong_t0"
      1 * transformer.transform(_, context, false, _) >> []
  }

  def "a server evaluated client presenting its etag gets NO_CHANGE until the fhubWhenActive time passes"() {
    expect:
      requestAt(context, clock, "kwong_t0")[0].success == expected
    where:
      clock                  || expected
      "2026-12-01T08:59:59Z" || FeatureRequestSuccess.NO_CHANGE
      "2026-12-01T09:00:00Z" || FeatureRequestSuccess.SUCCESS
      "2026-12-02T00:00:00Z" || FeatureRequestSuccess.SUCCESS
  }

  def "after the fhubWhenActive time the new etag is stable"() {
    expect:
      requestAt(context, "2026-12-02T00:00:00Z", "kwong_t2")[0].success == FeatureRequestSuccess.NO_CHANGE
  }

  def "a server evaluated client holding an etag from before this change (no time part) gets a full result"() {
    expect:
      requestAt(context, "2026-11-30T00:00:00Z", "kwong")[0].success == FeatureRequestSuccess.SUCCESS
  }

  def "client evaluated keys keep Dacha's etag as they evaluate the time themselves"() {
    given:
      def clientEval = new ClientContext(true)
    expect:
      requestAt(clientEval, "2026-12-02T00:00:00Z", "kwong")[0].success == FeatureRequestSuccess.NO_CHANGE
  }

  def "a full response carries the environment's poll interval override"() {
    given:
      def holder = new EtagStructureHolder([:], "0", false)
      def coll = new FeatureRequestCollection(2, transformer, context, future, holder)
      def r1 = Mock(FeatureRequester)
      r1.key >> kp1
      r1.details >> new DachaKeyDetailsResponse().etag("x").features([])
        .environmentInfo(["pollInterval": "30", "cacheControl": "max-age=20"])
      def r2 = Mock(FeatureRequester)
      r2.key >> kp2
      r2.details >> new DachaKeyDetailsResponse().etag("y").features([]).environmentInfo(["pollInterval": "4"])
    when:
      coll.complete(r1)
      coll.complete(r2)
      def responses = future.get()
    then:
      responses[0].environment.pollInterval == 30
      responses[1].environment.pollInterval == null
      2 * transformer.transform([], context, false, _) >> []
    and: "the field is only on the wire for the environment that has a value"
      def json = JacksonObjectProvider.mapper.writeValueAsString(responses*.environment)
      json.contains('"pollInterval":30')
      json.count('pollInterval') == 1
  }

  def "the poll interval comes from mgmt.pollInterval first, then pollInterval, ignoring non-numbers, and is null below 5s"() {
    expect:
      FeatureRequestCollection.pollInterval(info) == expected
    where:
      info                                                || expected
      null                                                || null
      [:]                                                 || null
      ["cacheControl": "max-age=20"]                      || null
      ["pollInterval": "45"]                              || 45
      ["pollInterval": " 45 "]                            || 45
      ["mgmt.pollInterval": "120", "pollInterval": "45"]  || 120
      ["mgmt.pollInterval": "kwong", "pollInterval": "45"] || 45
      ["pollInterval": "0"]                               || null
      ["pollInterval": "-10"]                             || null
      ["pollInterval": "4"]                               || null
      ["pollInterval": "5"]                               || 5
      ["mgmt.pollInterval": "3", "pollInterval": "45"]    || null
      ["pollInterval": "1.5"]                             || null
      ["pollInterval": "orm"]                             || null
  }

  /** a clock that moves on each time it is read, so a second read would land after the fhubWhenActive time */
  static class SteppingClock extends Clock {
    List<Instant> instants
    int reads = 0

    SteppingClock(List<String> times) { instants = times.collect { Instant.parse(it) } }

    @Override ZoneId getZone() { ZoneOffset.UTC }
    @Override Clock withZone(ZoneId zone) { this }
    @Override Instant instant() { instants[Math.min(reads++, instants.size() - 1)] }
  }

  def "the etag's clock part and the evaluation use the one instant taken before evaluating"() {
    given: "a clock that would cross the fhubWhenActive time between reads"
      def clock = new SteppingClock(["2026-12-01T08:59:59Z", "2026-12-01T09:00:01Z"])
      def holder = new EtagStructureHolder([:], "0", false)
      def coll = new FeatureRequestCollection(1, transformer, context, future, holder, clock)
      def r1 = Mock(FeatureRequester)
      r1.key >> kp1
      r1.details >> new DachaKeyDetailsResponse().etag("kwong").features(whenActiveFeatures("2026-12-01T09:00:00Z"))
    when:
      coll.complete(r1)
      def responses = future.get()
    then: "the features are evaluated at the same instant the etag was counted at"
      1 * transformer.transform(_, context, false, Instant.parse("2026-12-01T08:59:59Z")) >> []
      responses[0].etag == "kwong_t0"
      clock.reads == 1
  }
}
