package io.featurehub.dacha2.client

import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.redis.FeatureHubRedis
import io.featurehub.redis.RedisSource
import io.featurehub.redis.StaticRedisSource
import kotlin.jvm.functions.Function0
import spock.lang.Specification

/**
 * The fallback client owns a probe thread and its redis source (reload thread, connections) - shutdown must release
 * them, without creating a client that was never used.
 */
class Dacha2FallbackShutdownSpec extends Specification {
  static Set<Thread> probeThreads() {
    Thread.allStackTraces.keySet().findAll { it.name == "dacha2-redis-fallback" && it.alive } as Set
  }

  def "closing the fallback client stops its probe thread and closes its redis source"() {
    given: "other specs may leave their own probe threads running, so only look at the one this client starts"
      def existing = probeThreads()
      def source = Mock(RedisSource)
      def client = new Dacha2FallbackServiceClient(Mock(Dacha2ServiceClient), source, 10000,
        { -> 0L } as Function0<Long>, true, 100)
      def ours = probeThreads() - existing
    expect:
      ours.size() == 1
    when:
      client.close()
    then:
      1 * source.close()
    and: "its probe thread has gone"
      ours.every { it.join(5000); !it.alive }
  }

  def "a failure closing the redis source does not escape close"() {
    given:
      def source = Mock(RedisSource) { close() >> { throw new RuntimeException("boom") } }
      def client = new Dacha2FallbackServiceClient(Mock(Dacha2ServiceClient), source, 10000,
        { -> 0L } as Function0<Long>, false, 100)
    when:
      client.close()
    then:
      noExceptionThrown()
  }

  def "a static redis source closes its connection"() {
    given:
      def redis = Mock(FeatureHubRedis)
    when:
      new StaticRedisSource(redis).close()
    then:
      1 * redis.close()
  }

  def "shutdown closes the client the factory created, once"() {
    given:
      def source = Mock(RedisSource)
      def client = new Dacha2FallbackServiceClient(Mock(Dacha2ServiceClient), source, 10000,
        { -> 0L } as Function0<Long>, false, 100)
      def resources = new Dacha2FallbackResources()
      resources.created(client)
      def shutdown = new Dacha2FallbackShutdown(resources)
    when:
      shutdown.shutdown()
      shutdown.shutdown()
    then:
      1 * source.close()
  }

  def "shutdown does nothing if no fallback client was ever created"() {
    when:
      new Dacha2FallbackShutdown(new Dacha2FallbackResources()).shutdown()
    then:
      noExceptionThrown()
  }

  def "shutdown also closes the dedicated management repository client, once"() {
    given:
      def mrClient = Mock(jakarta.ws.rs.client.Client)
      def client = new Dacha2FallbackServiceClient(Mock(Dacha2ServiceClient), Mock(RedisSource), 10000,
        { -> 0L } as Function0<Long>, false, 100)
      def resources = new Dacha2FallbackResources()
      resources.created(client, mrClient)
    when:
      new Dacha2FallbackShutdown(resources).shutdown()
      new Dacha2FallbackShutdown(resources).shutdown()
    then:
      1 * mrClient.close()
  }

  def "a retry interval below the minimum uses the minimum and keeps going"() {
    given:
      def existing = probeThreads()
    when:
      def client = new Dacha2FallbackServiceClient(Mock(Dacha2ServiceClient), Mock(RedisSource), retry,
        { -> 0L } as Function0<Long>, true, 100)
    then:
      noExceptionThrown()
      client.retryIntervalMs == Dacha2FallbackServiceClient.MINIMUM_RETRY_INTERVAL_MS
      (probeThreads() - existing).size() == 1
    cleanup:
      client?.close()
    where:
      retry << [0L, -5L, 10L]
  }
}
