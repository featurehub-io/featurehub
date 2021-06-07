package io.featurehub.edge.stats

import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import spock.lang.Specification

import java.util.concurrent.Executors

class StatKeyEventCollectionSpec extends Specification {
  def "multi threaded access to a key collector works as expected"() {
    given: "i have a collection"
      def envId = UUID.randomUUID()
      def sc = new StatKeyEventCollection(new KeyParts('1', envId, '3'))
    and: "i have a set of threads ready"
      def pool = Executors.newFixedThreadPool(6)
      Runnable run = new Runnable() {
        @Override
        void run() {
          EdgeHitSourceType.values().each { hit ->
            EdgeHitResultType.values().each {result ->
              sc.add(result, hit)
            }
          }
        }
      }
    when: "i set them going"
      def futures = (1..6).collect { pool.submit(run) }
    and: "i wait for them to finish"
      futures.each {it.get() }
    and: "then i squash them"
      def squashed = sc.squash()
    then:
      sc.size() == (EdgeHitResultType.values().size() * EdgeHitSourceType.values().size())
      squashed.svcKey == '3'
      squashed.envId == envId
      squashed.counters.size() == (EdgeHitResultType.values().size() * EdgeHitSourceType.values().size())
      squashed.counters.each {it.count == 6}
  }
}
