package io.featurehub.dacha.resource

import io.featurehub.dacha.InternalCache
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureValueCacheItem
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.ServiceAccountPermission
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
      cache.getFeaturesByEnvironmentAndServiceAccount(_, _) >> null
    when: "we ask for a key"
      resource.getApiKeyDetails(UUID.randomUUID(), "xxx")
    then:
      thrown NotFoundException
  }

  def "if we ask for an environment / service key combo that does exist we get a valid data set back"() {
    given: "we have an environment and service key"
      def eId = UUID.randomUUID()
      def sKey = "123*456"
    and: "we have a internal cache"
      def orgId = UUID.randomUUID()
      def portId = UUID.randomUUID()
      def appId = UUID.randomUUID()
      def serviceAccountId = UUID.randomUUID()

      cache.getFeaturesByEnvironmentAndServiceAccount(eId, sKey) >> new InternalCache.FeatureCollection([new FeatureValueCacheItem().environmentId(eId)], new ServiceAccountPermission(),
          orgId, portId, appId, serviceAccountId)
    when: "we ask for a bad key"
      def details = resource.getApiKeyDetails(eId, sKey)
    then:
      details.features.size() == 1
      details.features[0].environmentId == eId
      details.organizationId == orgId
      details.portfolioId == portId
      details.applicationId == appId
      details.serviceKeyId == serviceAccountId
  }

  def "if we ask for permissions for a key combo that doesn't exist we get a NFE"() {
    given: "we tell the cache to reject requests"
      cache.getFeaturesByEnvironmentAndServiceAccount(_, _) >> null
    when: "we ask for a key"
      resource.getApiKeyPermissions(UUID.randomUUID(), "xxx", "FEATURE_ONE")
    then:
      thrown NotFoundException
  }

  def "if we ask for a feature key that doesn't exist we get a NFE"() {
    given: "we tell the cache to accept the request"
      cache.getFeaturesByEnvironmentAndServiceAccount(_, _) >> new InternalCache.FeatureCollection([new FeatureValueCacheItem()], new ServiceAccountPermission(),
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
    when: "we ask for a key that isn't in the list"
      resource.getApiKeyPermissions(UUID.randomUUID(), "xxx", "FEATURE_ONE")
    then:
      thrown NotFoundException
  }

  def "if we ask for a key that does exist, we get details and roles back"() {
    given: "we tell the cache to accept the request"
      def key = 'FEATURE_ONE'
      def orgId = UUID.randomUUID()
      def portId = UUID.randomUUID()
      def appId = UUID.randomUUID()
      def serviceAccountId = UUID.randomUUID()
      cache.getFeaturesByEnvironmentAndServiceAccount(_, _) >> new InternalCache.FeatureCollection([new FeatureValueCacheItem().feature(
        new Feature().key(key)
      )], new ServiceAccountPermission().permissions([RoleType.CHANGE_VALUE]), orgId, portId, appId, serviceAccountId)
    when: "we ask for a key that isn't in the list"
      def details = resource.getApiKeyPermissions(UUID.randomUUID(), "xxx", key)
    then:
      details.roles == [RoleType.CHANGE_VALUE]
      details.feature.feature.key == key
      details.organizationId == orgId
      details.portfolioId == portId
      details.applicationId == appId
      details.serviceKeyId == serviceAccountId
  }
}
