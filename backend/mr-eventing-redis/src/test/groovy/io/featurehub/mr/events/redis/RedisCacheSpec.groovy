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
import kotlin.Unit
import kotlin.jvm.functions.Function1
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

  def "a sync that loses its lock (e.g. it expired and another instance took it) stops before the sweep"() {
    given:
      System.setProperty("redis.full-sync-lock-ms", "300") // renewed every 1s (the minimum)
      def lockedSync = new RedisFullSync(redis, writer, { -> walker } as Provider<CacheWalker>)
      def orphan = UUID.randomUUID()
      redis.set(redis.keys.environment(orphan), "{}")
      walker.publishableEnvironmentIds() >> {
        // another instance takes over the lock while we are walking the database
        redis.set(redis.keys.syncLock, "another-instance")
        Thread.sleep(1600)
        []
      }
      walker.publishableServiceAccountIds() >> []
      walker.publishableEnvironment(orphan) >> null
    when:
      lockedSync.syncAll("test")
    then:
      thrown(IllegalStateException)
      redis.get(redis.keys.environment(orphan)) != null // the sweep did not run
      redis.get(redis.keys.syncLock) == "another-instance" // and we did not release their lock
    cleanup:
      lockedSync.stop()
      System.clearProperty("redis.full-sync-lock-ms")
  }

  def "the sync lock is released when the sync finishes"() {
    given:
      walker.publishableEnvironmentIds() >> []
      walker.publishableServiceAccountIds() >> []
    when:
      def result = sync.syncAll("test")
    then:
      result != null
      redis.get(redis.keys.syncLock) == null
  }

  def "syncOnSyncThread runs the sync on the untenanted redis-full-sync thread, not the caller's"() {
    given:
      def envId = UUID.randomUUID()
      String walkedOn = null
      walker.publishableEnvironmentIds() >> { walkedOn = Thread.currentThread().name; [envId] }
      walker.publishableServiceAccountIds() >> []
      walker.publishableEnvironment(envId) >> env(envId)
    when:
      def result = sync.syncOnSyncThread("test").get()
    then:
      walkedOn == "redis-full-sync"
      result.environments == 1
      redis.get(redis.keys.environment(envId)) != null
  }

  def "syncOnSyncThread holds null when another instance holds the lock"() {
    given:
      redis.set(redis.keys.syncLock, "someone-else")
    expect:
      sync.syncOnSyncThread("test").get() == null
  }

  def "an api key added to a service account during the sweep is not removed (e.g. a client key reset)"() {
    given: "service account X had keys K1/S, and has just been reset to K3/S - the writer wrote X, then sakey:K3"
      def saX = UUID.randomUUID()
      def before = CacheJsonMapper.mapper.writeValueAsString(sa(saX, "K1*", "S"))
      redis.set(redis.keys.serviceAccount(saX), CacheJsonMapper.mapper.writeValueAsString(sa(saX, "K3*", "S")))
      redis.set(redis.keys.serviceAccountKey("K3*"), saX.toString())
      redis.set(redis.keys.serviceAccountKey("S"), saX.toString())
    and: "the sweep's first read of X happened just before the reset was written"
      def racing = new InMemoryRedis() {
        boolean armed = false
        @Override
        void scanKeys(String pattern, int batchSize, Function1<? super List<String>, Unit> consumer) {
          // only the api key sweep sees the old copy, not the sync's own refresh of X
          if (pattern == keys.serviceAccountKeyPattern) armed = true
          super.scanKeys(pattern, batchSize, consumer)
        }

        @Override
        String get(String key) {
          if (armed && key == keys.serviceAccount(saX)) {
            armed = false
            return before
          }
          return super.get(key)
        }
      }
      racing.data.putAll(redis.data)
      def provider = { -> walker } as Provider<CacheWalker>
      def racingWriter = new RedisCacheWriter(racing, provider)
      def racingSync = new RedisFullSync(racing, racingWriter, provider)
      walker.publishableEnvironmentIds() >> []
      walker.publishableServiceAccountIds() >> [saX]
      walker.publishableServiceAccount(saX) >> sa(saX, "K3*", "S")
    when:
      def result = racingSync.syncAll("test")
    then: "the sweep's old copy says X does not hold K3, but the confirmation re-read does - so K3 is kept"
      racing.get(racing.keys.serviceAccountKey("K3*")) == saX.toString()
      racing.get(racing.keys.serviceAccountKey("S")) == saX.toString()
      result.removedApiKeys == 0
    cleanup:
      racingWriter.drain()
      racingSync.stop()
  }

  def "a sync where some writes fail is reported as incomplete, with what failed"() {
    given:
      def good = UUID.randomUUID()
      def bad = UUID.randomUUID()
      def badSa = UUID.randomUUID()
      walker.publishableEnvironmentIds() >> [good, bad]
      walker.publishableServiceAccountIds() >> [badSa]
      walker.publishableEnvironment(good) >> env(good)
      walker.publishableEnvironment(bad) >> { throw new RuntimeException("database unavailable") }
      walker.publishableServiceAccount(badSa) >> { throw new RuntimeException("database unavailable") }
    when:
      def result = sync.syncAll("test")
    then:
      result.environments == 2
      result.failedEnvironments == 1
      result.failedServiceAccounts == 1
      !result.complete
      redis.get(redis.keys.environment(good)) != null
      redis.get(redis.keys.metaSync).contains('"complete":false')
      redis.get(redis.keys.syncLock) == null
  }

  def "a complete sync says so"() {
    given:
      walker.publishableEnvironmentIds() >> []
      walker.publishableServiceAccountIds() >> []
    expect:
      sync.syncAll("test").complete
      redis.get(redis.keys.metaSync).contains('"complete":true')
  }

  def "the lock is released even if the lock renewal cannot be scheduled (e.g. while shutting down)"() {
    given:
      sync.stop()
    when:
      sync.syncAll("test")
    then:
      thrown(java.util.concurrent.RejectedExecutionException)
      redis.get(redis.keys.syncLock) == null
  }

  def "syncInBackgroundThen returns straight away, then syncs and runs afterwards on the redis-full-sync thread"() {
    given:
      def envId = UUID.randomUUID()
      def done = new java.util.concurrent.CountDownLatch(1)
      String afterwardsOn = null
      walker.publishableEnvironmentIds() >> [envId]
      walker.publishableServiceAccountIds() >> []
      walker.publishableEnvironment(envId) >> env(envId)
    when:
      sync.syncInBackgroundThen("test") { afterwardsOn = Thread.currentThread().name; done.countDown(); kotlin.Unit.INSTANCE }
      done.await(5, java.util.concurrent.TimeUnit.SECONDS)
    then:
      afterwardsOn == "redis-full-sync"
      redis.get(redis.keys.environment(envId)) != null
  }

  def "syncInBackgroundThen still runs afterwards when the sync fails or is skipped"() {
    given:
      def done = new java.util.concurrent.CountDownLatch(2)
      walker.publishableEnvironmentIds() >> { throw new RuntimeException("database unavailable") }
    when: "the sync fails"
      sync.syncInBackgroundThen("test") { done.countDown(); kotlin.Unit.INSTANCE }
    and: "the next one is skipped, another instance holds the lock"
      sync.syncInBackgroundThen("test") { redis.set(redis.keys.syncLock, "someone-else"); kotlin.Unit.INSTANCE }
      sync.syncInBackgroundThen("test") { done.countDown(); kotlin.Unit.INSTANCE }
    then:
      done.await(5, java.util.concurrent.TimeUnit.SECONDS)
  }

  def "the api key sweep reads the indexes and their service accounts in batches, not one key at a time"() {
    given:
      def counting = new InMemoryRedis() {
        int indexGets = 0
        int mgets = 0
        @Override
        String get(String key) {
          if (key.startsWith(keys.serviceAccountKey(""))) indexGets++
          return super.get(key)
        }
        @Override
        List<String> mget(List<String> k) {
          mgets++
          return k.collect { data.get(it) }
        }
      }
      def ids = (1..20).collect { UUID.randomUUID() }
      ids.each { id ->
        counting.set(counting.keys.serviceAccount(id), CacheJsonMapper.mapper.writeValueAsString(sa(id, "c*${id}", "s${id}")))
        counting.set(counting.keys.serviceAccountKey("c*${id}"), id.toString())
        counting.set(counting.keys.serviceAccountKey("s${id}"), id.toString())
      }
      counting.set(counting.keys.serviceAccountKey("stale"), ids[0].toString())
      def provider = { -> walker } as Provider<CacheWalker>
      def countingWriter = new RedisCacheWriter(counting, provider)
      def countingSync = new RedisFullSync(counting, countingWriter, provider)
      walker.publishableEnvironmentIds() >> []
      walker.publishableServiceAccountIds() >> ids
      ids.each { id -> walker.publishableServiceAccount(id) >> sa(id, "c*${id}", "s${id}") }
    when:
      def result = countingSync.syncAll("test")
    then: "one scan batch of 41 indexes: one MGET of the indexes, one of the 20 service accounts"
      counting.mgets == 2
      counting.indexGets == 0
      result.removedApiKeys == 1
      counting.get(counting.keys.serviceAccountKey("stale")) == null
      counting.get(counting.keys.serviceAccountKey("c*${ids[0]}")) == ids[0].toString()
    cleanup:
      countingWriter.drain()
      countingSync.stop()
  }

  def "on shutdown the redis connection is closed, and a failure closing it does not escape"() {
    given:
      def r = Mock(io.featurehub.redis.FeatureHubRedis)
    when:
      new RedisConnectionShutdown(r).shutdown()
    then:
      1 * r.close() >> { throw new RuntimeException("already gone") }
      noExceptionThrown()
  }

  def "the redis connection is closed after the writer has drained and the full sync has stopped"() {
    expect:
      RedisConnectionShutdown.getAnnotation(io.featurehub.lifecycle.LifecyclePriority).priority() >
        io.featurehub.lifecycle.DrainExecutorPool.getAnnotation(io.featurehub.lifecycle.LifecyclePriority).priority()
      RedisConnectionShutdown.getAnnotation(io.featurehub.lifecycle.LifecyclePriority).priority() >
        io.featurehub.lifecycle.LifecyclePriority.APPLICATION_PRIORITY_END
      RedisFullSyncLifecycle.getAnnotation(io.featurehub.lifecycle.LifecyclePriority) == null // the default, 100
  }
}
