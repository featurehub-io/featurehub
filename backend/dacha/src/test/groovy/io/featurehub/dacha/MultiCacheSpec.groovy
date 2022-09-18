package io.featurehub.dacha

import io.featurehub.dacha.CacheAction
import io.featurehub.dacha.resource.DachaEdgeNATSAdapter
import io.featurehub.publish.NATSSource
import io.nats.client.Connection
import io.nats.client.Dispatcher
import spock.lang.Specification

/**
 */
class MultiCacheSpec extends Specification {
  List<Cache> cacheList
  class Cache {
    ServerConfig config
    CacheManager cacheManager
    InternalCache cache
  }

  NATSSource natsSource
  DachaEdgeNATSAdapter dachaEdgeNATSAdapter

  def setup() {
    System.clearProperty("nats.urls")
    System.clearProperty("cache.name")
    System.clearProperty("cache.mit")
    System.clearProperty("cache.timeout")
    System.clearProperty("cache.complete-timeout")

    cacheList = []

    Connection conn = Mock(Connection)
    Dispatcher dispatcher  = Mock(Dispatcher)
    Dispatcher subscribe = Mock(Dispatcher)
    natsSource = Mock(NATSSource)
    natsSource.connection >> conn
    conn.createDispatcher(_) >> dispatcher
    dispatcher.subscribe(_) >> subscribe
    subscribe.unsubscribe(_) >> dispatcher

  }

  def cleanup() {
    cacheList.each { c ->
      c.cacheManager.shutdown();
      c.config.shutdown();
    }
  }

  Cache mrCache() {
    Cache cache = new Cache()

    cache.cache = new MrInMemoryCache()

    System.setProperty("nats.urls", "nats://localhost:4222")
    System.setProperty("cache.name", "multi")
    System.setProperty("cache.mit", "1")
    System.setProperty("cache.timeout", "300")
    System.setProperty("cache.complete-timeout", "300")
    cache.config = new ServerConfig(cache.cache, natsSource, dachaEdgeNATSAdapter)

    cache.cacheManager = new CacheManager(cache.cache, cache.config, natsSource)

    cacheList.add(cache)

    return cache
  }

  Cache randomCache() {
    Cache cache = new Cache()

    cache.cache = new InMemoryCache()

    System.setProperty("nats.urls", "nats://localhost:4222")
    System.setProperty("cache.name", "multi")
    System.clearProperty("cache.mit")
    cache.config = new ServerConfig(cache.cache, natsSource, dachaEdgeNATSAdapter)

    cache.cacheManager = new CacheManager(cache.cache, cache.config, natsSource)

    cacheList.add(cache)

    return cache
  }

  def "on startup, if running just MR it will immediately come to rest"() {
    when: "we create the mr cache"
      def mrCache = mrCache()
    and: "it will be at rest after init"
      mrCache.cacheManager.init()
    then:
      mrCache.cacheManager.currentAction == CacheAction.AT_REST
  }

  def "on startup a random cache will never come to rest and be waiting seeking complete cache as there is no master"() {
    when: "we create a random cache and initialize it"
      System.setProperty("cache.timeout", "1")
      def cache = randomCache()
      cache.cacheManager.init()
    then: "it will remain in incomplete cache mode"
      cache.cacheManager.currentAction == CacheAction.WAITING_FOR_COMPLETE_SOURCE
  }

  def "on startup five random caches will never "() {
    when: "we create a random cache and initialize it"
        System.setProperty("cache.timeout", "1")
        List<Cache> caches = (1..5).collect({randomCache()})
        caches.each {c -> c.cacheManager.init()}
    then: "it will remain in incomplete cache mode"
        caches.each { c -> assert c.cacheManager.currentAction == CacheAction.WAITING_FOR_COMPLETE_SOURCE}
  }

//  def "on startup, one incomplete cache and MR will negotiate filling"() {
//    when: "we have an MR"
//      def mrCache = mrCache()
//      mrCache.cacheManager.init()
//      assert mrCache.cacheManager.currentAction == CacheAction.AT_REST
//    and: "we have a random cache"
//      System.clearProperty("cache.mit")
//      System.setProperty("cache.timeout", "300")
//      System.setProperty("cache.complete-timeout", "900")
//      List<Cache> caches = (1..1).collect({randomCache()})
//      def start = System.currentTimeMillis()
//      caches.each({ c ->
//        new Thread({
//          c.cacheManager.init()
//        }).run()
//      })
//    and: "we wait for timeout"
//      def allFinished = false
//      for(int count = 0; count < 30; count ++) {
//        print("${count + 1}..")
//        allFinished = caches.count({ c -> c.cacheManager.currentAction != CacheAction.AT_REST}) == 0
//        if (!allFinished) {
//          Thread.sleep(500)
//        } else {
//          break
//        }
//      }
//      if (!allFinished) {
//        caches.each { c ->
//          if (c.cacheManager.currentAction != CacheAction.AT_REST) {
//            println("cache ${c.cacheManager.id} is not at rest.")
//          }
//        }
//      }
//      println "\ncompleted in ${System.currentTimeMillis()-start}ms"
//    then:
//      allFinished
//  }
}
