package io.featurehub.dacha2.client

import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.dacha.model.CacheServiceAccountPermission
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha2.Dacha2NewCacheImpl
import io.featurehub.dacha2.FeatureValuesFactoryImpl
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.model.Dacha2Environment
import io.featurehub.mr.model.Dacha2ServiceAccount
import io.featurehub.mr.model.RoleType
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.ServerErrorException
import jakarta.ws.rs.ServiceUnavailableException
import jakarta.ws.rs.core.Response
import kotlin.jvm.functions.Function0
import io.featurehub.redis.FeatureHubRedis
import io.featurehub.redis.RedisSource
import spock.lang.Specification

class Dacha2FallbackServiceClientSpec extends Specification {
  Dacha2ServiceClient mr
  InMemoryRedis redis
  long now
  Dacha2FallbackServiceClient client
  UUID envId
  UUID saId

  def setup() {
    mr = Mock()
    redis = new InMemoryRedis()
    now = 1000
    client = new Dacha2FallbackServiceClient(mr, redis, 10000, { -> now } as Function0<Long>)
    envId = UUID.randomUUID()
    saId = UUID.randomUUID()
  }

  PublishEnvironment env() {
    return new PublishEnvironment().environment(new CacheEnvironment().id(envId).version(1))
      .organizationId(UUID.randomUUID()).portfolioId(UUID.randomUUID()).applicationId(UUID.randomUUID())
      .action(PublishAction.CREATE).count(0).featureValues([]).serviceAccounts([saId])
  }

  CacheServiceAccount sa() {
    return new CacheServiceAccount().id(saId).version(1).apiKeyClientSide("c*1").apiKeyServerSide("s1")
      .permissions([new CacheServiceAccountPermission().environmentId(envId).permissions([RoleType.READ])])
  }

  void storeInRedis() {
    redis.set(redis.keys.environment(envId), CacheJsonMapper.mapper.writeValueAsString(env()))
    redis.set(redis.keys.serviceAccount(saId), CacheJsonMapper.mapper.writeValueAsString(sa()))
    redis.set(redis.keys.serviceAccountKey("c*1"), saId.toString())
    redis.set(redis.keys.serviceAccountKey("s1"), saId.toString())
  }

  def "when MR is up its answer is used and redis is not consulted"() {
    given:
      def result = new Dacha2Environment().env(env())
    when:
      def found = client.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> result
      found == result
      !client.open
  }

  def "a 404 from MR is authoritative and never falls back to redis"() {
    given:
      storeInRedis()
    when:
      client.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> { throw new NotFoundException() }
      thrown(NotFoundException)
      !client.open
  }

  def "a 403 from MR is authoritative"() {
    when:
      client.getServiceAccount("s1", "k")
    then:
      1 * mr.getServiceAccount("s1", "k") >> { throw new ForbiddenException() }
      thrown(ForbiddenException)
  }

  def "a connection failure opens the circuit and serves from redis"() {
    given:
      storeInRedis()
    when:
      def found = client.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> { throw new ProcessingException("connection refused") }
      found.env.environment.id == envId
      client.open
  }

  def "a 503 (maintenance responder) opens the circuit and serves service accounts from redis by either key"() {
    given:
      storeInRedis()
    when:
      def byClient = client.getServiceAccount("c*1", null)
      def byServer = client.getServiceAccount("s1", null)
    then:
      1 * mr.getServiceAccount("c*1", null) >> {
        throw new ServerErrorException(Response.status(503).header("retry-after", "Wed, 21 Oct 2026 07:28:00 GMT").build())
      }
      0 * mr._ // circuit open, the second request does not go to MR
      byClient.serviceAccount.id == saId
      byServer.serviceAccount.id == saId
  }

  def "a redis miss is ServiceUnavailable (not NotFound) so it is never sin-binned"() {
    when:
      client.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> { throw new ProcessingException("down") }
      thrown(ServiceUnavailableException)
  }

  def "a stale api key index is not trusted"() {
    given:
      storeInRedis()
      redis.set(redis.keys.serviceAccountKey("old-key"), saId.toString())
    when:
      client.getServiceAccount("old-key", null)
    then:
      1 * mr.getServiceAccount("old-key", null) >> { throw new ProcessingException("down") }
      thrown(ServiceUnavailableException)
  }

  def "if redis is also down the original MR failure is surfaced"() {
    given:
      def brokenRedis = Stub(io.featurehub.redis.FeatureHubRedis) {
        getKeys() >> redis.keys
        get(_) >> { throw new RuntimeException("redis down") }
      }
      def c = new Dacha2FallbackServiceClient(mr, brokenRedis, 10000, { -> now } as Function0<Long>)
      def mrFailure = new ProcessingException("mr down")
    when:
      c.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> { throw mrFailure }
      def e = thrown(ProcessingException)
      e.is(mrFailure)
  }

  def "the circuit probes MR after the retry interval and closes, notifying listeners of what came from redis"() {
    given:
      storeInRedis()
      Set<UUID> recoveredEnvs = null
      Set<String> recoveredKeys = null
      client.addRecoveryListener({ envs, keys -> recoveredEnvs = envs; recoveredKeys = keys } as Dacha2RecoveryListener)
    when: "MR goes down"
      client.getEnvironment(envId, null)
      client.getServiceAccount("s1", null)
    then:
      1 * mr.getEnvironment(envId, null) >> { throw new ProcessingException("down") }
      0 * mr.getServiceAccount(_, _)
      client.open
    when: "before the retry interval, MR is not tried"
      now += 5000
      client.getEnvironment(envId, null)
    then:
      0 * mr._
    when: "after the interval MR is probed, still down"
      now += 6000
      client.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> { throw new ProcessingException("still down") }
      client.open
      recoveredEnvs == null
    when: "MR comes back"
      now += 11000
      def found = client.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> new Dacha2Environment().env(env())
      !client.open
      found.env.environment.id == envId
      recoveredEnvs == [envId] as Set
      recoveredKeys == ["c*1", "s1"] as Set
  }

  def "through the real cache, a redis miss during an outage does not stop the environment loading once MR is back"() {
    given:
      def cache = new Dacha2NewCacheImpl(client, new FeatureValuesFactoryImpl())
      redis.set(redis.keys.serviceAccount(saId), CacheJsonMapper.mapper.writeValueAsString(sa()))
      redis.set(redis.keys.serviceAccountKey("s1"), saId.toString())
    when: "MR is down and the environment is not in redis"
      def first = cache.getFeatureCollection(envId, "s1", true)
    then:
      mr.getServiceAccount("s1", _) >> { throw new ProcessingException("down") }
      first == null
    when: "MR is back"
      now += 20000
      def second = cache.getFeatureCollection(envId, "s1", true)
    then:
      mr.getServiceAccount("s1", _) >> new Dacha2ServiceAccount().serviceAccount(sa())
      mr.getEnvironment(envId, _) >> new Dacha2Environment().env(env())
      second != null
      second.features.environment.environment.id == envId
    cleanup:
      cache.closeCache()
  }

  def "with no redis configured MR failures pass straight through and the circuit never opens"() {
    given:
      FeatureHubRedis current = null
      def c = new Dacha2FallbackServiceClient(mr, { -> current } as RedisSource, 10000, { -> now } as Function0<Long>)
      storeInRedis()
    when:
      c.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> { throw new ProcessingException("down") }
      thrown(ProcessingException)
      !c.open
  }

  def "redis can be enabled and disabled at runtime"() {
    given:
      FeatureHubRedis current = null
      def c = new Dacha2FallbackServiceClient(mr, { -> current } as RedisSource, 10000, { -> now } as Function0<Long>)
      storeInRedis()
    when: "redis is configured while MR is down"
      current = redis
      def found = c.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> { throw new ProcessingException("down") }
      found.env.environment.id == envId
      c.open
    when: "redis is removed again while the circuit is open, MR is asked directly"
      current = null
      c.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> { throw new ProcessingException("still down") }
      thrown(ProcessingException)
    when: "MR comes back"
      def again = c.getEnvironment(envId, null)
    then:
      1 * mr.getEnvironment(envId, null) >> new Dacha2Environment().env(env())
      again.env.environment.id == envId
      !c.open
  }

  /**
   * When MR is scaled to 0 during maintenance, a Kubernetes Service in front of it has no endpoints: there is no 503.
   * Depending on the network layer the connection is either refused or the packets are dropped (connect timeout).
   * Both must open the circuit - this uses the real HTTP client rather than a mocked exception.
   */
  @spock.lang.Unroll
  def "an unreachable MR (#scenario) with the real HTTP client falls back to redis"() {
    given:
      storeInRedis()
      def httpClient = jakarta.ws.rs.client.ClientBuilder.newClient()
      httpClient.property(org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT, 500)
      httpClient.property(org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT, 500)
      System.setProperty("management-repository.url", url)
      def realMr = new Dacha2Client(httpClient)
      def c = new Dacha2FallbackServiceClient(realMr, redis, 10000, { -> now } as Function0<Long>)
    when:
      def start = System.currentTimeMillis()
      def found = c.getEnvironment(envId, null)
      def elapsed = System.currentTimeMillis() - start
    then:
      found.env.environment.id == envId
      c.open
      elapsed < 5000 // bounded by the connect timeout, not hanging
    cleanup:
      System.clearProperty("management-repository.url")
      httpClient?.close()
    where:
      scenario                                  | url
      "connection refused, no endpoints"        | "http://127.0.0.1:1"
      "packets dropped, connect timeout"        | "http://10.255.255.1:8904"
  }
}
