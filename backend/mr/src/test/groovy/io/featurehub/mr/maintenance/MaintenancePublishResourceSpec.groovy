package io.featurehub.mr.maintenance

import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.redis.RedisFullSync
import io.featurehub.mr.events.common.CacheWalker
import io.featurehub.mr.events.redis.RedisCacheWriter
import io.featurehub.redis.FeatureHubRedis
import io.featurehub.redis.RedisKeys
import jakarta.inject.Provider
import kotlin.jvm.functions.Function0
import spock.lang.Specification

class MaintenancePublishResourceSpec extends Specification {
  MaintenanceCachePublisher publisher = Mock()
  MaintenancePublishResource resource = new MaintenancePublishResource(publisher)

  def "with redis it starts the publish in the background and returns 202 straight away"() {
    given:
      publisher.cachePublishingEnabled >> true
      publisher.redisConfigured >> true
    when:
      def response = resource.publishAll()
    then:
      1 * publisher.publishAllInBackground()
      response.status == 202
      response.entity.redisConfigured == true
      response.entity.message.contains("maintenance-publish-all: finished")
  }

  def "without redis it starts the publish and says redis is not configured"() {
    given:
      publisher.cachePublishingEnabled >> true
      publisher.redisConfigured >> false
    when:
      def response = resource.publishAll()
    then:
      1 * publisher.publishAllInBackground()
      response.status == 202
      response.entity.redisConfigured == false
  }

  def "with cache publishing disabled it returns 503"() {
    given:
      publisher.cachePublishingEnabled >> false
    when:
      def response = resource.publishAll()
    then:
      0 * publisher.publishAllInBackground()
      response.status == 503
  }

  def "a DummyPublisher (cloud event publishing disabled) counts as publishing disabled"() {
    given:
      def p = new MaintenancePublisher(new io.featurehub.mr.events.common.DummyPublisher(), null)
    expect:
      !p.cachePublishingEnabled
      new MaintenancePublishResource(p).publishAll().status == 503
  }

  def "without redis the publisher publishes to the streaming caches, so redis is not asked to sync"() {
    given:
      def cacheSource = Mock(CacheSource)
      def p = new MaintenancePublisher(cacheSource, null)
    when:
      p.publishAllInBackground()
    then:
      1 * cacheSource.publishToStreamingCaches()
      0 * cacheSource.publishObjectsAssociatedWithCache()
      p.cachePublishingEnabled
      !p.redisConfigured
  }

  // no objenesis on this classpath, so the mock needs real constructor arguments
  RedisFullSync mockFullSync() {
    def redis = Stub(FeatureHubRedis) { getKeys() >> new RedisKeys("test") }
    def provider = { -> null } as Provider<CacheWalker>
    def writer = new RedisCacheWriter(redis, provider)
    return Mock(RedisFullSync, constructorArgs: [redis, writer, provider])
  }

  def "with redis the publisher hands the sync and the streaming publish to the sync thread, never the request thread"() {
    given:
      def fullSync = mockFullSync()
      def cacheSource = Mock(CacheSource)
      def p = new MaintenancePublisher(cacheSource, fullSync)
      Function0 afterwards = null
    when:
      p.publishAllInBackground()
    then: "nothing runs on the request thread"
      1 * fullSync.syncInBackgroundThen("maintenance-publish-all", _) >> { args -> afterwards = args[1] as Function0 }
      0 * fullSync.syncAll(_)
      0 * cacheSource._
    when: "the sync thread finishes the sync"
      afterwards.invoke()
    then: "the streaming caches are published without asking redis for another sync"
      1 * cacheSource.publishToStreamingCaches()
      0 * cacheSource.publishObjectsAssociatedWithCache()
  }
}
