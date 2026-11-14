package io.featurehub.redis

import kotlin.jvm.functions.Function1
import redis.clients.jedis.UnifiedJedis
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.exceptions.JedisException
import spock.lang.Specification

/**
 * Redis is an optional fallback - an unreachable Redis must never stop MR or Dacha2 from starting.
 */
class JedisFeatureHubRedisSpec extends Specification {
  // nothing listens on port 1, so connecting is refused straight away
  static RedisConfig unreachable(String mode, String master = null) {
    RedisConfig.parse("redis://127.0.0.1:1", mode, null, null, master, null, 200, 200, null)
  }

  def "constructing a #mode client does not connect, so an unreachable redis does not fail startup"() {
    when:
      def redis = new JedisFeatureHubRedis(unreachable(mode, master))
    then:
      noExceptionThrown()
    when:
      redis.get("x")
    then: "it fails like any other redis outage would"
      thrown(JedisException)
    cleanup:
      redis?.close()
    where:
      mode         | master
      "cluster"    | null
      "sentinel"   | "mymaster"
      "standalone" | null
  }

  def "a failed connection is retried after the backoff and the client is then reused"() {
    given:
      def client = Mock(UnifiedJedis)
      int attempts = 0
      def factory = { RedisConfig c ->
        attempts++
        if (attempts == 1) throw new JedisConnectionException("down")
        return client
      } as Function1<RedisConfig, UnifiedJedis>
      def redis = new JedisFeatureHubRedis(unreachable("cluster"), 50, factory)
    when: "the first use fails to connect"
      redis.get("a")
    then:
      thrown(JedisConnectionException)
      attempts == 1
    when: "used again inside the backoff, it fails without trying to connect"
      redis.get("a")
    then:
      thrown(JedisConnectionException)
      attempts == 1
    when: "after the backoff it connects"
      Thread.sleep(80)
      def v1 = redis.get("a")
      def v2 = redis.get("b")
    then:
      attempts == 2
      1 * client.get("a") >> "1"
      1 * client.get("b") >> "2"
      v1 == "1"
      v2 == "2"
    when:
      redis.close()
    then:
      1 * client.close()
    when: "once closed it never reconnects"
      redis.get("a")
    then:
      thrown(JedisConnectionException)
      attempts == 2
  }

  def "a non-jedis construction failure is surfaced as a connection failure"() {
    given:
      def redis = new JedisFeatureHubRedis(unreachable("cluster"), 0,
        { RedisConfig c -> throw new IllegalStateException("boom") } as Function1<RedisConfig, UnifiedJedis>)
    when:
      redis.get("a")
    then:
      def e = thrown(JedisConnectionException)
      e.cause instanceof IllegalStateException
  }
}
