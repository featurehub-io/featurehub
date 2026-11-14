package io.featurehub.db.publish

import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.mr.events.common.SelfSyncingCacheBroadcast
import spock.lang.Specification

import java.util.concurrent.ExecutorService

class CacheBroadcastProxySpec extends Specification {
  ExecutorService executor = Mock(ExecutorService)

  def "with one streaming broadcaster plus redis, both are called in order on the calling thread"() {
    given:
      def calls = []
      def dacha = Stub(CacheBroadcast) { publishEnvironment(_) >> { calls << "dacha:${Thread.currentThread().name}" } }
      def redis = Stub(SelfSyncingCacheBroadcast) { publishEnvironment(_) >> { calls << "redis:${Thread.currentThread().name}" } }
      def proxy = new CacheBroadcastProxy([redis, dacha], executor)
      def thread = Thread.currentThread().name
    when:
      proxy.publishEnvironment(new PublishEnvironment())
      proxy.publishEnvironment(new PublishEnvironment())
    then: "nothing is handed off to the executor, so events cannot be reordered"
      0 * executor._
      calls == ["dacha:$thread", "redis:$thread", "dacha:$thread", "redis:$thread"]*.toString()
  }

  def "a failing broadcaster does not stop the others"() {
    given:
      def dacha = Mock(CacheBroadcast)
      def redis = Mock(SelfSyncingCacheBroadcast)
      def proxy = new CacheBroadcastProxy([dacha, redis], executor)
      def env = new PublishEnvironment()
    when:
      proxy.publishEnvironment(env)
    then:
      1 * dacha.publishEnvironment(env) >> { throw new RuntimeException("nats down") }
      1 * redis.publishEnvironment(env)
  }

  def "several blocking broadcasters are fanned out, self-syncing ones stay inline"() {
    given:
      def a = Mock(CacheBroadcast)
      def b = Mock(CacheBroadcast)
      def redis = Mock(SelfSyncingCacheBroadcast)
      def proxy = new CacheBroadcastProxy([a, b, redis], executor)
      def env = new PublishEnvironment()
    when:
      proxy.publishEnvironment(env)
    then:
      2 * executor.submit(_ as Runnable) >> { Runnable r -> r.run(); null }
      1 * a.publishEnvironment(env)
      1 * b.publishEnvironment(env)
      1 * redis.publishEnvironment(env)
  }
}
