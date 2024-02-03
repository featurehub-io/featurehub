package io.featurehub.dacha.resource

import groovy.transform.CompileStatic
import io.featurehub.dacha.EnvironmentFeatures
import io.featurehub.dacha.InternalCache
import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.CacheServiceAccountPermission
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishAction
import io.featurehub.mr.model.RoleType
import jakarta.ws.rs.NotFoundException
import spock.lang.Specification

class DachaApiKeyResourceSpec extends Specification {
  InternalCache cache
  DachaApiKeyResource resource

  def setup() {
    cache = Mock(InternalCache)
    resource = new DachaApiKeyResource(cache)
  }

  def "if we ask for an environment / service key combo that does not exist we get a NFE"() {
    given: "we tell the cache to reject requests"
      cache.cacheComplete() >> true
      cache.getFeaturesByEnvironmentAndServiceAccount(_, _) >> null
    when: "we ask for a key"
      resource.getApiKeyDetails(UUID.randomUUID(), "xxx")
    then:
      thrown NotFoundException
  }

  def "if we ask for an environment / service key combo that does exist we get a valid data set back"() {
    given: "we have an environment and service key"
      cache.cacheComplete() >> true
      def eId = UUID.randomUUID()
      def sKey = "123*456"
    and: "we have a internal cache"
      def orgId = UUID.randomUUID()
      def portId = UUID.randomUUID()
      def appId = UUID.randomUUID()
      def serviceAccountId = UUID.randomUUID()

      cache.getFeaturesByEnvironmentAndServiceAccount(eId, sKey) >> new InternalCache.FeatureCollection(
        new EnvironmentFeatures(
          new PublishEnvironment()
            .environment(new CacheEnvironment().id(eId))
            .portfolioId(portId)
            .organizationId(orgId)
            .applicationId(appId)
            .featureValues([
              new CacheEnvironmentFeature()
                .feature(new CacheFeature().version(1).id(UUID.randomUUID()))
            ])
            .action(PublishAction.CREATE)
            .count(1)
        ), new CacheServiceAccountPermission(), serviceAccountId)
    when: "we ask for a bad key"
      def details = resource.getApiKeyDetails(eId, sKey)
    then:
      details.features.size() == 1
      details.organizationId == orgId
      details.portfolioId == portId
      details.applicationId == appId
      details.serviceKeyId == serviceAccountId
  }

  def "if we ask for permissions for a key combo that doesn't exist we get a NFE"() {
    given: "we tell the cache to reject requests"
      cache.cacheComplete() >> true
      cache.getFeaturesByEnvironmentAndServiceAccount(_, _) >> null
    when: "we ask for a key"
      resource.getApiKeyPermissions(UUID.randomUUID(), "xxx", "FEATURE_ONE")
    then:
      thrown NotFoundException
  }

  def "if we ask for a feature key that doesn't exist we get a NFE"() {
    given: "we tell the cache to accept the request"
      cache.cacheComplete() >> true
      cache.getFeaturesByEnvironmentAndServiceAccount(_, _) >> randomCollection()
    when: "we ask for a key that isn't in the list"
      resource.getApiKeyPermissions(UUID.randomUUID(), "xxx", "FEATURE_ONE")
    then:
      thrown NotFoundException
  }

  @CompileStatic
  static InternalCache.FeatureCollection randomCollection() {
    return new InternalCache.FeatureCollection(
      new EnvironmentFeatures(
        new PublishEnvironment()
          .environment(new CacheEnvironment().id(UUID.randomUUID()))
          .portfolioId(UUID.randomUUID())
          .organizationId(UUID.randomUUID())
          .applicationId(UUID.randomUUID())
          .featureValues([
          ])
          .action(PublishAction.CREATE)
          .count(0)
      ), new CacheServiceAccountPermission(), UUID.randomUUID())
  }

  def "if we ask for a key that does exist, we get details and roles back"() {
    given: "we tell the cache to accept the request"
      cache.cacheComplete() >> true
      def key = 'FEATURE_ONE'
      def fc = randomCollection()
      def ef = fc.features as EnvironmentFeatures
      ef.set(new CacheEnvironmentFeature().feature(new CacheFeature().version(1).id(UUID.randomUUID()).key(key)))
      fc.perms.permissions([RoleType.CHANGE_VALUE])

      cache.getFeaturesByEnvironmentAndServiceAccount(_, _) >> fc
    when: "we ask for a key that isn't in the list"
      def details = resource.getApiKeyPermissions(UUID.randomUUID(), "xxx", key)
    then:
      details.roles == [RoleType.CHANGE_VALUE]
      details.feature.feature.key == key
      details.organizationId == fc.features.environment.organizationId
      details.portfolioId == fc.features.environment.portfolioId
      details.applicationId == fc.features.environment.applicationId
      details.serviceKeyId == fc.serviceAccountId
  }
}
