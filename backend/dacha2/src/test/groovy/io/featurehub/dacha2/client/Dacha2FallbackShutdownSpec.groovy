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
  def "closing the fallback client stops its probe thread and closes its redis source"() {
    given:
      def source = Mock(RedisSource)
      def client = new Dacha2FallbackServiceClient(Mock(Dacha2ServiceClient), source, 10000,
        { -> 0L } as Function0<Long>, true, 100)
    when:
      client.close()
    then:
      1 * source.close()
    and: "the probe thread has gone"
      Thread.allStackTraces.keySet().findAll { it.name == "dacha2-redis-fallback" && it.alive }
        .every { it.join(1000); !it.alive }
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
}
