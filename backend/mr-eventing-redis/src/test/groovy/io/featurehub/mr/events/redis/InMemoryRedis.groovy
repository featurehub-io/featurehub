package io.featurehub.mr.events.redis

import io.featurehub.redis.FeatureHubRedis
import io.featurehub.redis.RedisKeys
import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull

import java.util.concurrent.ConcurrentHashMap

/**
 * A very small in-memory stand in for Redis, good enough for the key/value semantics we use.
 */
class InMemoryRedis implements FeatureHubRedis {
  final Map<String, String> data = new ConcurrentHashMap<>()
  final RedisKeys keys = new RedisKeys("test")

  @Override
  RedisKeys getKeys() { return keys }

  @Override
  String get(@NotNull String key) { return data.get(key) }

  @Override
  void set(@NotNull String key, @NotNull String value) { data.put(key, value) }

  @Override
  long del(@NotNull String... k) {
    long count = 0
    k.each { if (data.remove(it) != null) count++ }
    return count
  }

  @Override
  boolean setIfAbsent(@NotNull String key, @NotNull String value, long ttlMs) {
    return data.putIfAbsent(key, value) == null
  }

  @Override
  boolean deleteIfEquals(@NotNull String key, @NotNull String expected) {
    return data.remove(key, expected)
  }

  @Override
  boolean expireIfEquals(@NotNull String key, @NotNull String expected, long ttlMs) {
    return data.get(key) == expected // no TTLs in the fake
  }

  @Override
  void scanKeys(@NotNull String pattern, int batchSize, @NotNull Function1<? super List<String>, Unit> consumer) {
    def prefix = pattern.substring(0, pattern.length() - 1) // we only use trailing * patterns
    def matched = data.keySet().findAll { it.startsWith(prefix) }.toList()
    matched.collate(batchSize).each { consumer.invoke(it) }
  }

  @Override
  void close() {}
}
