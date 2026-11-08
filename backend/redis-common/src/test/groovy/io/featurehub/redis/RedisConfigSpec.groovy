package io.featurehub.redis

import spock.lang.Specification

class RedisConfigSpec extends Specification {
  def "a simple url parses as standalone with defaults"() {
    when:
      def cfg = RedisConfig.parse("redis://localhost:6380", null, null, null, null, null, null, null, null)
    then:
      !cfg.tls
      cfg.hosts == [new RedisHost("localhost", 6380)]
      cfg.mode == RedisMode.STANDALONE
      cfg.username == null
      cfg.password == null
      cfg.database == 0
      cfg.keyPrefix == "featurehub"
      cfg.connectTimeoutMs == 2000
  }

  def "an auth-string-only url (managed redis style) puts the value in the password"() {
    when:
      def cfg = RedisConfig.parse("rediss://s3cr%40t@10.0.0.3:6378/2", null, null, null, null, "acme", null, null, null)
    then:
      cfg.tls
      cfg.username == null
      cfg.password == 's3cr@t'
      cfg.database == 2
      cfg.hosts == [new RedisHost("10.0.0.3", 6378)]
      cfg.keyPrefix == 'acme'
  }

  def "user and password are parsed and explicit config overrides the url"() {
    when:
      def cfg = RedisConfig.parse("redis://fred:pw@host", null, null, "override", null, null, null, null, null)
    then:
      cfg.username == 'fred'
      cfg.password == 'override'
      cfg.hosts == [new RedisHost("host", 6379)]
  }

  def "the password is never in toString"() {
    when:
      def cfg = RedisConfig.parse("redis://fred:topsecret@host", null, null, null, null, null, null, null, null)
    then:
      !cfg.toString().contains("topsecret")
  }

  def "cluster mode allows multiple hosts, standalone does not"() {
    when:
      def cfg = RedisConfig.parse("redis://a:1,b:2", "cluster", null, null, null, null, null, null, null)
    then:
      cfg.mode == RedisMode.CLUSTER
      cfg.hosts.size() == 2
    when:
      RedisConfig.parse("redis://a:1,b:2", null, null, null, null, null, null, null, null)
    then:
      thrown(IllegalArgumentException)
  }

  def "sentinel mode requires a master"() {
    when:
      RedisConfig.parse("redis://a:26379", "sentinel", null, null, null, null, null, null, null)
    then:
      thrown(IllegalArgumentException)
  }

  def "bad schemes are rejected"() {
    when:
      RedisConfig.parse("http://a:1", null, null, null, null, null, null, null, null)
    then:
      thrown(IllegalArgumentException)
  }

  def "keys use a hash tag prefix and ids round trip"() {
    given:
      def keys = new RedisKeys("acme")
      def id = UUID.randomUUID()
    expect:
      keys.environment(id) == "{acme}:env:${id}".toString()
      keys.serviceAccountKey("abc*def") == "{acme}:sakey:abc*def"
      keys.environmentIdFromKey(keys.environment(id)) == id
      keys.serviceAccountIdFromKey(keys.serviceAccount(id)) == id
      keys.environmentIdFromKey(keys.serviceAccount(id)) == null
      keys.environmentIdFromKey("{acme}:env:not-a-uuid") == null
      keys.environmentPattern == "{acme}:env:*"
  }

  def "hostnames with underscores (docker-compose service names), IPv6 and default ports are accepted"() {
    expect:
      RedisConfig.parse(url, null, null, null, null, null, null, null, null).hosts == hosts
    where:
      url                                   | hosts
      "redis://redis_cache:6379"            | [new RedisHost("redis_cache", 6379)]
      "redis://redis_cache"                 | [new RedisHost("redis_cache", 6379)]
      "rediss://pw@my_redis.local:6380/1"   | [new RedisHost("my_redis.local", 6380)]
      "redis://[::1]:6390"                  | [new RedisHost("::1", 6390)]
      "redis://[fe80::1]"                   | [new RedisHost("fe80::1", 6379)]
  }

  def "invalid hosts and ports are rejected"() {
    when:
      RedisConfig.parse(url, null, null, null, null, null, null, null, null)
    then:
      thrown(IllegalArgumentException)
    where:
      url << ["redis://host:notaport", "redis://host:0", "redis://host:70000", "redis://::1:6379", "redis://[::1]x", "redis://:6379"]
  }
}
