package io.featurehub.dacha2.resource

import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.CacheFeatureValue
import io.featurehub.dacha.model.CacheServiceAccountPermission
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha2.Dacha2Cache
import io.featurehub.dacha2.EnvironmentFeatures
import io.featurehub.dacha2.FeatureCollection
import io.featurehub.mr.model.RoleType
import jakarta.ws.rs.NotFoundException
import spock.lang.Specification

class DachaApiKeyResourceSpec extends Specification {
  Dacha2Cache cache
  DachaApiKeyResource resource
  UUID envId
  String apiKey
  UUID orgId
  UUID appId
  UUID portId
  UUID serviceAccountId
  Map<String, String> envInfo = ["me": "you"]

  def setup() {
    envId = UUID.randomUUID()
    apiKey = "1234"
    cache = Mock()
    resource = new DachaApiKeyResource(cache)
    orgId = UUID.randomUUID()
    appId = UUID.randomUUID()
    portId = UUID.randomUUID()
    serviceAccountId = UUID.randomUUID()
    envInfo = ["me": "you"]
  }

  def "getApiKeyDetails: if there is no match i get a not found exception"() {
    when:
      resource.getApiKeyDetails(envId, apiKey, true)
    then:
      1 * cache.getFeatureCollection(envId, apiKey) >> null
      thrown(NotFoundException)
  }

  EnvironmentFeatures createEnv() {
    return new EnvironmentFeatures(
      new PublishEnvironment().action(PublishAction.CREATE)
        .count(0)
        .organizationId(orgId)
        .portfolioId(portId)
        .applicationId(appId)
        .environment(new CacheEnvironment().id(envId).version(1).environmentInfo(envInfo))
        .featureValues([
          new CacheEnvironmentFeature().feature(new CacheFeature().id(UUID.randomUUID()).version(1).key("x")),
          new CacheEnvironmentFeature().feature(new CacheFeature().id(UUID.randomUUID()).version(1).key("y")
          ).value(new CacheFeatureValue().version(1).retired(true)),
        ])
    )
  }

  FeatureCollection featureCollection() {
    return new FeatureCollection(createEnv(), new CacheServiceAccountPermission().permissions([RoleType.READ]), serviceAccountId)
  }

  def "getApiKeyDetails: if it is found, we get all the details"() {
    given: "we have the data"
      def fc = featureCollection()
    when: "i call to get the data"
      def result1 = resource.getApiKeyDetails(envId, apiKey, true)
    and: "i ask for retired false"
      def result2 = resource.getApiKeyDetails(envId, apiKey, false)
    then:
      2 * cache.getFeatureCollection(envId, apiKey) >> fc
      result1.organizationId == orgId
      result1.applicationId == appId
      result1.portfolioId == portId
      result1.serviceKeyId == serviceAccountId
      result1.etag == fc.features.etag
      result1.environmentInfo == envInfo
      result1.features.size() == 1
      result2.features.size() == 2
  }

  def "getApiKeyPermissions: not environment = 404"() {
    when:
      resource.getApiKeyPermissions(envId, apiKey, "x")
    then:
      thrown(NotFoundException)
      1 * cache.getFeatureCollection(envId, apiKey) >> null
  }

  def "getApiKeyPermissions: no feature matched = 404"() {
    when:
      resource.getApiKeyPermissions(envId, apiKey, "z")
    then:
      thrown(NotFoundException)
      1 * cache.getFeatureCollection(envId, apiKey) >> featureCollection()
  }

  def "getApiKeyPermissions: feature matched"() {
    when:
      def result1 = resource.getApiKeyPermissions(envId, apiKey, "x")
    then:
      result1.organizationId == orgId
      result1.applicationId == appId
      result1.portfolioId == portId
      result1.serviceKeyId == serviceAccountId
      result1.environmentInfo == envInfo
      result1.feature != null
      1 * cache.getFeatureCollection(envId, apiKey) >> featureCollection()
  }

  def "dachaEnvironmentResource: no environment, not found exception"() {
    given:
      def res = new DachaEnvironmentResource(cache)
    when:
      res.getEnvironmentStructure(envId)
    then:
      thrown(NotFoundException)
      1 * cache.findEnvironment(envId) >> null
  }

  def "dachaEnvironmentResource: found environment so get data"() {
    given:
      def res = new DachaEnvironmentResource(cache)
    when:
      def result1 = res.getEnvironmentStructure(envId)
    then:
      result1.organizationId == orgId
      result1.applicationId == appId
      result1.portfolioId == portId
      1 * cache.findEnvironment(envId) >> createEnv()
  }

}
