package io.featurehub.mr.maintenance

import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.redis.RedisSyncResult
import spock.lang.Specification

class MaintenancePublishResourceSpec extends Specification {
  MaintenanceCachePublisher publisher = Mock()
  MaintenancePublishResource resource = new MaintenancePublishResource(publisher)

  def "with redis, a full redis sync runs synchronously first, then the streaming caches are published"() {
    given:
      publisher.cachePublishingEnabled >> true
      publisher.redisConfigured >> true
    when:
      def response = resource.publishAll()
    then:
      1 * publisher.syncRedis() >> new RedisSyncResult("maintenance-publish-all", 3, 2, 1, 0, 1, 50)
    then:
      1 * publisher.publishToStreamingCaches()
      response.status == 200
      response.entity.redisConfigured == true
      response.entity.redis.environments == 3
      response.entity.redis.serviceAccounts == 2
      response.entity.redis.orphanedEnvironments == 1
  }

  def "if another redis sync holds the lock it returns 409 and does not publish"() {
    given:
      publisher.cachePublishingEnabled >> true
      publisher.redisConfigured >> true
    when:
      def response = resource.publishAll()
    then:
      1 * publisher.syncRedis() >> null
      0 * publisher.publishToStreamingCaches()
      response.status == 409
  }

  def "without redis only the streaming caches are published"() {
    given:
      publisher.cachePublishingEnabled >> true
      publisher.redisConfigured >> false
    when:
      def response = resource.publishAll()
    then:
      0 * publisher.syncRedis()
      1 * publisher.publishToStreamingCaches()
      response.status == 200
      response.entity.redisConfigured == false
      response.entity.redis == null
  }

  def "with cache publishing disabled it returns 503"() {
    given:
      publisher.cachePublishingEnabled >> false
    when:
      def response = resource.publishAll()
    then:
      0 * publisher.syncRedis()
      0 * publisher.publishToStreamingCaches()
      response.status == 503
  }

  def "the default publisher uses publishToStreamingCaches so redis is not asked to sync again"() {
    given:
      def cacheSource = Mock(CacheSource)
      def p = new MaintenancePublisher(cacheSource, null)
    when:
      p.publishToStreamingCaches()
    then:
      1 * cacheSource.publishToStreamingCaches()
      0 * cacheSource.publishObjectsAssociatedWithCache()
      p.cachePublishingEnabled
      !p.redisConfigured
      p.syncRedis() == null
  }

  def "if the redis sync fails the streaming caches are still published and it returns 500 with the error"() {
    given:
      publisher.cachePublishingEnabled >> true
      publisher.redisConfigured >> true
    when:
      def response = resource.publishAll()
    then:
      1 * publisher.syncRedis() >> { throw new RuntimeException("redis is down") }
    then:
      1 * publisher.publishToStreamingCaches()
      response.status == 500
      response.entity.cachesPublished == true
      response.entity.redisConfigured == true
      response.entity.redisError == "redis is down"
      response.entity.redis == null
  }

  def "a DummyPublisher (cloud event publishing disabled) counts as publishing disabled"() {
    given:
      def p = new MaintenancePublisher(new io.featurehub.mr.events.common.DummyPublisher(), null)
    expect:
      !p.cachePublishingEnabled
      new MaintenancePublishResource(p).publishAll().status == 503
  }
}
