package io.featurehub.edge.ofrep

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.CacheFeatureValue
import io.featurehub.dacha.model.DachaKeyDetailsResponse
import io.featurehub.edge.FeatureTransformerUtils
import io.featurehub.edge.KeyParts
import io.featurehub.mr.model.FeatureValueType
import jakarta.ws.rs.NotFoundException
import spock.lang.Specification

import java.util.UUID

class OfrepEvaluationServiceSpec extends Specification {
  def dachaRegistry = Mock(DachaClientServiceRegistry)
  def apiKeyService = Mock(DachaApiKeyService)
  def transformer = new FeatureTransformerUtils()
  def service = new OfrepEvaluationService(dachaRegistry, transformer)

  def envId = UUID.randomUUID()
  def key = new KeyParts("default", envId, "service-key-1")

  def booleanFeature(keyName, value) {
    def featureId = UUID.randomUUID()
    return new CacheEnvironmentFeature()
      .feature(new CacheFeature().id(featureId).key(keyName).valueType(FeatureValueType.BOOLEAN))
      .value(new CacheFeatureValue().id(featureId).key(keyName).locked(false).version(2L).value(value))
  }

  def stringFeature(keyName, value) {
    def featureId = UUID.randomUUID()
    return new CacheEnvironmentFeature()
      .feature(new CacheFeature().id(featureId).key(keyName).valueType(FeatureValueType.STRING))
      .value(new CacheFeatureValue().id(featureId).key(keyName).locked(false).version(1L).value(value))
  }

  def "extractToken handles Bearer and X-API-Key"() {
    expect:
      OfrepEvaluationService.extractToken("Bearer abc", null) == "abc"
      OfrepEvaluationService.extractToken("bearer abc", null) == "abc"
      OfrepEvaluationService.extractToken(null, "xyz") == "xyz"
      OfrepEvaluationService.extractToken("Basic abc", "xyz") == "xyz"
      OfrepEvaluationService.extractToken(null, null) == null
      OfrepEvaluationService.extractToken("Bearer", null) == null
  }

  def "a missing or malformed key is unauthorized"() {
    when:
      service.parseApiKey(null)
    then:
      thrown(OfrepUnauthorizedException)

    when:
      service.parseApiKey("not-a-key")
    then:
      thrown(OfrepUnauthorizedException)
  }

  def "two and three part keys parse, url-safe base64 tolerated"() {
    given:
      def envUuid = UUID.randomUUID().toString()
    expect:
      service.parseApiKey("${envUuid}/svc").environmentId == UUID.fromString(envUuid)
      service.parseApiKey("default/${envUuid}/svc").cacheName == "default"
      service.parseApiKey("namedcache/${envUuid}/svc").cacheName == "namedcache"
  }

  def "client-evaluated keys are rejected"() {
    when:
      service.parseApiKey("default/${UUID.randomUUID()}/*star-service")
    then:
      thrown(OfrepClientEvalKeyException)
  }

  def "an unknown api key is unauthorized"() {
    given:
      dachaRegistry.getApiKeyService("default") >> apiKeyService
      apiKeyService.getApiKeyDetails(envId, "service-key-1", true) >> { throw new NotFoundException() }
    when:
      service.evaluateAll(key, ["targetingKey": "u1"])
    then:
      thrown(OfrepUnauthorizedException)
  }

  def "bulk evaluation maps all features and builds an etag"() {
    given:
      def details = new DachaKeyDetailsResponse()
        .etag("env-etag-7")
        .extendedDataAllowed(false)
        .features([booleanFeature("on-off", true), stringFeature("greeting", "hello")])
      dachaRegistry.getApiKeyService("default") >> apiKeyService
      apiKeyService.getApiKeyDetails(envId, "service-key-1", true) >> details

    when:
      def result = service.evaluateAll(key, ["targetingKey": "u1"])
      def flags = result.first
      def etag = result.second

    then:
      flags.size() == 2
      flags[0] instanceof OfrepFlagEvaluation
      flags[0].key == "on-off"
      flags[0].value == true
      flags[0].reason == OfrepReasons.STATIC
      flags[1].value == "hello"
      etag == '"env-etag-7//' + OfrepMapper.toClientContext(["targetingKey": "u1"]).makeEtag() + '"'
  }

  def "a failing feature becomes a GENERAL failure entry rather than killing the batch"() {
    given:
      def bad = Mock(CacheEnvironmentFeature)
      bad.getFeature() >> { throw new RuntimeException("boom") }
      def details = new DachaKeyDetailsResponse()
        .etag("e")
        .extendedDataAllowed(false)
        .features([bad, booleanFeature("fine", false)])
      dachaRegistry.getApiKeyService("default") >> apiKeyService
      apiKeyService.getApiKeyDetails(envId, "service-key-1", true) >> details

    when:
      def result = service.evaluateAll(key, [:])
      def flags = result.first

    then:
      flags.size() == 2
      flags[0] instanceof OfrepEvalFailure
      flags[0].errorCode == OfrepErrorCodes.GENERAL
      flags[1] instanceof OfrepFlagEvaluation
      flags[1].value == false
  }

  def "single flag evaluation returns the named flag"() {
    given:
      def details = new DachaKeyDetailsResponse()
        .etag("env-etag-1")
        .extendedDataAllowed(false)
        .features([booleanFeature("on-off", true), stringFeature("greeting", "hello")])
      dachaRegistry.getApiKeyService("default") >> apiKeyService
      apiKeyService.getApiKeyDetails(envId, "service-key-1", true) >> details

    when:
      def result = service.evaluateOne(key, "greeting", ["targetingKey": "u1"])

    then:
      result instanceof OfrepFlagEvaluation
      result.key == "greeting"
      result.value == "hello"
  }

  def "single flag evaluation of an unknown key is FLAG_NOT_FOUND"() {
    given:
      def details = new DachaKeyDetailsResponse()
        .etag("env-etag-1")
        .extendedDataAllowed(false)
        .features([booleanFeature("on-off", true)])
      dachaRegistry.getApiKeyService("default") >> apiKeyService
      apiKeyService.getApiKeyDetails(envId, "service-key-1", true) >> details

    when:
      service.evaluateOne(key, "unknown-flag", [:])

    then:
      thrown(OfrepFlagNotFoundException)
  }
}
