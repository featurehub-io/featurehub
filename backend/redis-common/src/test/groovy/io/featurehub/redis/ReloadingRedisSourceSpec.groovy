package io.featurehub.redis

import kotlin.jvm.functions.Function1
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class ReloadingRedisSourceSpec extends Specification {
  Path dir
  Path configFile
  Path secretFile
  List<RedisConfig> created
  List<FeatureHubRedis> clients
  List<RedisConfig> changes

  def setup() {
    dir = Files.createTempDirectory("redis-reload")
    configFile = dir.resolve("redis.properties")
    secretFile = dir.resolve("redis-secret.properties")
    created = []
    clients = []
    changes = []
  }

  def cleanup() {
    dir.toFile().deleteDir()
  }

  ReloadingRedisSource source(Function1<RedisConfig, FeatureHubRedis> factory = null) {
    def f = factory ?: { RedisConfig cfg ->
      created << cfg
      def client = Mock(FeatureHubRedis)
      clients << client
      return client
    } as Function1<RedisConfig, FeatureHubRedis>

    return new ReloadingRedisSource("dacha2.redis", [configFile, secretFile], 0, f, 0,
      { RedisConfig c -> changes << c; return kotlin.Unit.INSTANCE } as Function1<RedisConfig, kotlin.Unit>, false)
  }

  def "with no files there is no redis"() {
    when:
      def s = source()
    then:
      s.current() == null
      created.isEmpty()
  }

  def "redis is enabled when the file appears, combining the config and secret files"() {
    given:
      def s = source()
    when:
      configFile.text = "dacha2.redis.url=rediss://10.0.0.1:6378\ndacha2.redis.key-prefix=acme\n"
      secretFile.text = "dacha2.redis.password=s3cret\n"
      def changed = s.reload()
    then:
      changed
      s.current() != null
      created.size() == 1
      created[0].password == 's3cret'
      created[0].keyPrefix == 'acme'
      created[0].tls
      changes == [created[0]]
  }

  def "an unchanged file does not reconnect"() {
    given:
      configFile.text = "dacha2.redis.url=redis://host:6379\n"
      def s = source()
    when:
      def changed = s.reload()
    then:
      !changed
      created.size() == 1
  }

  def "a changed configuration swaps the connection and closes the old one"() {
    given:
      configFile.text = "dacha2.redis.url=redis://host1:6379\n"
      def s = source()
      def first = s.current()
    when:
      configFile.text = "dacha2.redis.url=redis://host2:6379\n"
      def changed = s.reload()
    then:
      changed
      s.current() != first
      created*.hosts*.host.flatten() == ['host1', 'host2']
      1 * first.close()
  }

  def "removing the url disables redis"() {
    given:
      configFile.text = "dacha2.redis.url=redis://host1:6379\n"
      def s = source()
      def first = s.current()
    when:
      configFile.text = "# disabled\n"
      def changed = s.reload()
    then:
      changed
      s.current() == null
      1 * first.close()
      changes.last() == null
  }

  def "an invalid configuration keeps the current connection"() {
    given:
      configFile.text = "dacha2.redis.url=redis://host1:6379\n"
      def s = source()
      def first = s.current()
    when:
      configFile.text = "dacha2.redis.url=http://not-redis\n"
      def changed = s.reload()
    then:
      !changed
      s.current().is(first)
      0 * first.close()
  }

  def "a connection that cannot be created keeps the current connection"() {
    given:
      configFile.text = "dacha2.redis.url=redis://host1:6379\n"
      def calls = 0
      def s = source({ RedisConfig cfg ->
        calls++
        if (calls > 1) throw new RuntimeException("cannot connect")
        return Mock(FeatureHubRedis)
      } as Function1<RedisConfig, FeatureHubRedis>)
      def first = s.current()
    when:
      configFile.text = "dacha2.redis.url=redis://host2:6379\n"
      def changed = s.reload()
    then:
      !changed
      s.current().is(first)
  }

  def "file paths are parsed from a comma separated list"() {
    expect:
      ReloadingRedisSource.files(" /a/b.properties, /c/d.properties ,") == [Path.of("/a/b.properties"), Path.of("/c/d.properties")]
      ReloadingRedisSource.files(null) == []
  }
}
