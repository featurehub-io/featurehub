package io.featurehub.mr.events.redis

import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.events.common.CacheWalker
import jakarta.inject.Provider
import spock.lang.Specification

class RedisCacheSpec extends Specification {
  InMemoryRedis redis
  CacheWalker walker
  RedisCacheWriter writer
  RedisFullSync sync
  RedisCacheBroadcaster broadcaster

  def setup() {
    System.setProperty("redis.env-republish-debounce-ms", "50")
    redis = new InMemoryRedis()
    walker = Mock()
    def provider = { -> walker } as Provider<CacheWalker>
    writer = new RedisCacheWriter(redis, provider)
    sync = new RedisFullSync(redis, writer, provider)
    broadcaster = new RedisCacheBroadcaster(writer, sync)
  }

  def cleanup() {
    writer.drain()
    sync.stop()
    System.clearProperty("redis.env-republish-debounce-ms")
  }

  PublishEnvironment env(UUID id) {
    return new PublishEnvironment().environment(new CacheEnvironment().id(id).version(1))
      .organizationId(UUID.randomUUID()).portfolioId(UUID.randomUUID()).applicationId(UUID.randomUUID())
      .action(PublishAction.CREATE).count(0).featureValues([]).serviceAccounts([])
  }

  CacheServiceAccount sa(UUID id, String client, String server) {
    return new CacheServiceAccount().id(id).version(1).apiKeyClientSide(client).apiKeyServerSide(server).permissions([])
  }

  void waitForWrites() {
    // the debounce is 50ms, the drain waits for everything pending
    writer.drain()
  }

  def "many feature changes for one environment collapse into one database read and one write"() {
    given:
      def envId = UUID.randomUUID()
    when:
      50.times {
        broadcaster.publishFeatures(new PublishFeatureValues().features([
          new PublishFeatureValue().environmentId(envId).action(PublishAction.UPDATE)
        ]))
      }
      waitForWrites()
    then:
      1 * walker.publishableEnvironment(envId) >> env(envId)
      CacheJsonMapper.mapper.readValue(redis.get(redis.keys.environment(envId)), PublishEnvironment).environment.id == envId
  }

  def "an environment that is no longer publishable is removed from redis"() {
    given:
      def envId = UUID.randomUUID()
      redis.set(redis.keys.environment(envId), "{}")
    when:
      broadcaster.publishEnvironment(new PublishEnvironment().environment(new CacheEnvironment().id(envId)).action(PublishAction.DELETE))
      waitForWrites()
    then:
      1 * walker.publishableEnvironment(envId) >> null
      redis.get(redis.keys.environment(envId)) == null
  }

  def "EMPTY events are ignored"() {
    when:
      broadcaster.publishEnvironment(new PublishEnvironment().environment(new CacheEnvironment().id(UUID.randomUUID())).action(PublishAction.EMPTY))
      broadcaster.publishServiceAccount(new PublishServiceAccount().action(PublishAction.EMPTY))
      waitForWrites()
    then:
      0 * walker._
      redis.data.isEmpty()
  }

  def "a service account is written with both api key indexes"() {
    given:
      def saId = UUID.randomUUID()
    when:
      broadcaster.publishServiceAccount(new PublishServiceAccount().serviceAccount(sa(saId, "c*1", "s1")).action(PublishAction.CREATE))
      waitForWrites()
    then:
      1 * walker.publishableServiceAccount(saId) >> sa(saId, "c*1", "s1")
      redis.get(redis.keys.serviceAccountKey("c*1")) == saId.toString()
      redis.get(redis.keys.serviceAccountKey("s1")) == saId.toString()
      CacheJsonMapper.mapper.readValue(redis.get(redis.keys.serviceAccount(saId)), CacheServiceAccount).apiKeyServerSide == "s1"
  }

  def "resetting a service account key removes the old index"() {
    given:
      def saId = UUID.randomUUID()
      redis.set(redis.keys.serviceAccount(saId), CacheJsonMapper.mapper.writeValueAsString(sa(saId, "c*1", "s1")))
      redis.set(redis.keys.serviceAccountKey("c*1"), saId.toString())
      redis.set(redis.keys.serviceAccountKey("s1"), saId.toString())
    when:
      writer.refreshServiceAccountOnWorker(saId).get()
    then:
      1 * walker.publishableServiceAccount(saId) >> sa(saId, "c*1", "s2")
      redis.get(redis.keys.serviceAccountKey("s1")) == null
      redis.get(redis.keys.serviceAccountKey("s2")) == saId.toString()
      redis.get(redis.keys.serviceAccountKey("c*1")) == saId.toString()
  }

  def "deleting a service account removes it and its indexes, but not an index now owned by another"() {
    given:
      def saId = UUID.randomUUID()
      def otherId = UUID.randomUUID()
      redis.set(redis.keys.serviceAccount(saId), CacheJsonMapper.mapper.writeValueAsString(sa(saId, "c*1", "s1")))
      redis.set(redis.keys.serviceAccountKey("c*1"), saId.toString())
      redis.set(redis.keys.serviceAccountKey("s1"), otherId.toString())
    when:
      writer.refreshServiceAccountOnWorker(saId).get()
    then:
      1 * walker.publishableServiceAccount(saId) >> null
      redis.get(redis.keys.serviceAccount(saId)) == null
      redis.get(redis.keys.serviceAccountKey("c*1")) == null
      redis.get(redis.keys.serviceAccountKey("s1")) == otherId.toString()
  }

  def "a full sync writes everything, sweeps orphans after re-checking them and records meta data"() {
    given:
      def env1 = UUID.randomUUID()
      def orphanEnv = UUID.randomUUID()
      def recreatedEnv = UUID.randomUUID() // created after the id list was read, must survive the sweep
      def sa1 = UUID.randomUUID()
      def orphanSa = UUID.randomUUID()
      redis.set(redis.keys.environment(orphanEnv), "{}")
      redis.set(redis.keys.environment(recreatedEnv), "{}")
      redis.set(redis.keys.serviceAccount(orphanSa), CacheJsonMapper.mapper.writeValueAsString(sa(orphanSa, "oc*", "os")))
      redis.set(redis.keys.serviceAccountKey("oc*"), orphanSa.toString())
      redis.set(redis.keys.serviceAccountKey("dangling"), UUID.randomUUID().toString())
    and:
      walker.publishableEnvironmentIds() >> [env1]
      walker.publishableServiceAccountIds() >> [sa1]
      walker.publishableEnvironment(env1) >> env(env1)
      walker.publishableEnvironment(orphanEnv) >> null
      walker.publishableEnvironment(recreatedEnv) >> env(recreatedEnv)
      walker.publishableServiceAccount(sa1) >> sa(sa1, "c*", "s")
      walker.publishableServiceAccount(orphanSa) >> null
    when:
      def result = sync.syncAll("test")
    then:
      result.environments == 1
      result.serviceAccounts == 1
      result.orphanedEnvironments == 2
      result.orphanedServiceAccounts == 1
      redis.get(redis.keys.environment(env1)) != null
      redis.get(redis.keys.environment(orphanEnv)) == null
      redis.get(redis.keys.environment(recreatedEnv)) != null
      redis.get(redis.keys.serviceAccount(orphanSa)) == null
      redis.get(redis.keys.serviceAccountKey("oc*")) == null
      redis.get(redis.keys.serviceAccountKey("dangling")) == null
      redis.get(redis.keys.serviceAccountKey("c*")) == sa1.toString()
      redis.get(redis.keys.metaSync).contains('"environments":1')
      redis.get(redis.keys.syncLock) == null
  }

  def "a full sync is skipped when another instance holds the lock"() {
    given:
      redis.set(redis.keys.syncLock, "someone-else")
    when:
      def result = sync.syncAll("test")
    then:
      result == null
      0 * walker._
      redis.get(redis.keys.syncLock) == "someone-else"
  }

  def "a full publish request from the cache source triggers a background sync"() {
    given:
      def envId = UUID.randomUUID()
      walker.publishableEnvironmentIds() >> [envId]
      walker.publishableServiceAccountIds() >> []
      walker.publishableEnvironment(envId) >> env(envId)
    when:
      broadcaster.fullPublishRequested()
      def deadline = System.currentTimeMillis() + 5000
      while (redis.get(redis.keys.metaSync) == null && System.currentTimeMillis() < deadline) {
        Thread.sleep(20)
      }
    then:
      redis.get(redis.keys.environment(envId)) != null
      redis.get(redis.keys.metaSync) != null
  }
}
