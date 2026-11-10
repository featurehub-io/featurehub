package io.featurehub.edge.ofrep

import com.fasterxml.jackson.databind.ObjectMapper
import io.featurehub.sse.model.FeatureState
import io.featurehub.mr.model.FeatureValueType
import spock.lang.Specification

class OfrepMapperSpec extends Specification {

  def objectMapper = new ObjectMapper()

  def "targetingKey maps to userkey and other attributes pass through"() {
    when:
      def ctx = OfrepMapper.toClientContext(["targetingKey": "user-123", "email": "a@b.com", "plan": "premium"])
    then:
      ctx.attributes["userkey"] == ["user-123"]
      ctx.attributes["email"] == ["a@b.com"]
      ctx.attributes["plan"] == ["premium"]
      !ctx.isClientEvaluation
  }

  def "explicit userkey is overridden by targetingKey and session maps to sessionkey"() {
    when:
      def ctx = OfrepMapper.toClientContext(["userkey": "wrong", "targetingKey": "right", "session": "sess-1"])
    then:
      ctx.attributes["userkey"] == ["right"]
      ctx.attributes["sessionkey"] == ["sess-1"]
  }

  def "numbers and booleans are stringified"() {
    when:
      def ctx = OfrepMapper.toClientContext(["targetingKey": "u", "age": 42, "vip": true])
    then:
      ctx.attributes["age"] == ["42"]
      ctx.attributes["vip"] == ["true"]
  }

  def "primitive arrays become value lists"() {
    when:
      def ctx = OfrepMapper.toClientContext(["targetingKey": "u", "roles": ["admin", "dev"]])
    then:
      ctx.attributes["roles"] == ["admin", "dev"]
  }

  def "nested objects are rejected"() {
    when:
      OfrepMapper.toClientContext(["targetingKey": "u", "nested": ["a": 1]])
    then:
      thrown(OfrepInvalidContextException)
  }

  def "arrays containing objects are rejected"() {
    when:
      OfrepMapper.toClientContext(["targetingKey": "u", "things": [["a": 1]]])
    then:
      thrown(OfrepInvalidContextException)
  }

  def "attribute count is capped at 30"() {
    given:
      def context = ["targetingKey": "u"]
      (1..40).each { context["attr" + it] = "v" + it }
    when:
      def ctx = OfrepMapper.toClientContext(context)
    then:
      ctx.attributes.size() == 30
  }

  def "a strategy id produces TARGETING_MATCH with variant"() {
    given:
      def fs = new FeatureState().key("flag").type(FeatureValueType.BOOLEAN).value(true).v("strategy-1")
    when:
      def result = OfrepMapper.toOfrepFlag(fs, objectMapper)
    then:
      result instanceof OfrepFlagEvaluation
      result.key == "flag"
      result.value == true
      result.reason == OfrepReasons.TARGETING_MATCH
      result.variant == "strategy-1"
      result.metadata["featureType"] == "BOOLEAN"
  }

  def "no strategy produces STATIC"() {
    given:
      def fs = new FeatureState().key("flag").type(FeatureValueType.STRING).value("blue")
    when:
      def result = OfrepMapper.toOfrepFlag(fs, objectMapper)
    then:
      result.reason == OfrepReasons.STATIC
      result.variant == null
      result.value == "blue"
  }

  def "json flags are parsed into objects"() {
    given:
      def fs = new FeatureState().key("cfg").type(FeatureValueType.JSON).value('{"a":1}')
    when:
      def result = OfrepMapper.toOfrepFlag(fs, objectMapper)
    then:
      result.value.get("a").asInt() == 1
  }

  def "invalid json produces a PARSE_ERROR failure entry"() {
    given:
      def fs = new FeatureState().key("cfg").type(FeatureValueType.JSON).value('{not json')
    when:
      def result = OfrepMapper.toOfrepFlag(fs, objectMapper)
    then:
      result instanceof OfrepEvalFailure
      result.errorCode == OfrepErrorCodes.PARSE_ERROR
      result.key == "cfg"
  }

  def "a null value serializes as code-default (no value)"() {
    given:
      def fs = new FeatureState().key("novalue").type(FeatureValueType.STRING)
    when:
      def result = OfrepMapper.toOfrepFlag(fs, objectMapper)
    then:
      result instanceof OfrepFlagEvaluation
      result.value == null
      result.reason == OfrepReasons.STATIC
  }

  def "bulk etag combines environment etag and context hash"() {
    given:
      def ctx = OfrepMapper.toClientContext(["targetingKey": "u"])
    when:
      def etag = OfrepMapper.bulkEtag("env-etag-1", ctx)
    then:
      etag == '"env-etag-1//' + ctx.makeEtag() + '"'
  }

  def "etag falls back to 0 when the environment etag is absent"() {
    given:
      def ctx = OfrepMapper.toClientContext([:])
    expect:
      OfrepMapper.bulkEtag(null, ctx) == '"0//' + ctx.makeEtag() + '"'
  }
}
