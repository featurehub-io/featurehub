package io.featurehub.edge.stats

import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import io.featurehub.sse.stats.model.EdgeStatsBundle
import spock.lang.Specification

class StatsCollectionOrchestratorSpec extends Specification {
  def "if the stats collection is empty, no publishing will happen"() {
    given: "i have a publisher"
      def pub = Mock(StatPublisher)
      def orch = new StatsCollectionOrchestrator(pub)
    when:
      orch.squashAndPublish([:])
    then:
      0 * pub.publish(_, _)
  }

  def "a collection with two caches will publish twice"() {
    given: "i have a publisher"
      def pub = Mock(StatPublisher)
      def orch = new StatsCollectionOrchestrator(pub)
    and:
      def k1 = new KeyParts("cache1", UUID.randomUUID(), "2")
      def col1 = new StatKeyEventCollection(k1)
      col1.add(EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.EVENTSOURCE)
      col1.add(EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.EVENTSOURCE)
      col1.add(EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.EVENTSOURCE)
    and:
      def k2 = new KeyParts("cache2", k1.environmentId, "2")
      def col2 = new StatKeyEventCollection(k2)
      col2.add(EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.EVENTSOURCE)
      col2.add(EdgeHitResultType.SUCCESS_UNTIL_KICKED_OFF, EdgeHitSourceType.EVENTSOURCE)
    when:
      orch.squashAndPublish([(k1): col1, (k2): col2])
    then:
      1 * pub.publish("cache1", _)
      1 * pub.publish("cache2", { EdgeStatsBundle bundle ->
        bundle.apiKeys.size() == 1
        bundle.apiKeys[0].envId == k1.environmentId
        bundle.apiKeys[0].svcKey == '2'
        find(bundle, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.EVENTSOURCE)
        find(bundle, EdgeHitResultType.SUCCESS_UNTIL_KICKED_OFF, EdgeHitSourceType.EVENTSOURCE)
      })
  }

  // we have to have this outside otherwise Spock is checking the equals methods as individual checks and fails on 1st miss
  private static def find(EdgeStatsBundle bundle, EdgeHitResultType result, EdgeHitSourceType source) {
    return bundle.apiKeys[0].counters.find({it.resultType == result
      && it.hitType == source}) != null
  }

  def "when the threshold is one and we have two items in the bundle, it will publish twice"() {
    given: "i have overridden the max apis per publish property and set it to 1"
      System.setProperty('edge.stats.max-apis-per-publish', '1')
    and:
      def pub = Mock(StatPublisher)
      def orch = new StatsCollectionOrchestrator(pub)
    and:
      def k1 = new KeyParts("cache1", UUID.randomUUID(), "2")
      def col1 = new StatKeyEventCollection(k1)
      col1.add(EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.EVENTSOURCE)
    and:
      def k2 = new KeyParts("cache1", UUID.randomUUID(), "b")
      def col2 = new StatKeyEventCollection(k2)
      col2.add(EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.EVENTSOURCE)
    when:
      def result = orch.squashAndPublish([(k1): col1, (k2): col2])
    then:
      2 * pub.publish('cache1', { EdgeStatsBundle bundle ->
        bundle.apiKeys.size() == 1
      })
      result
  }

  def "a failed publish is caught and doesn't bubble up"() {
    given: "i have a mocked publisher and an orchestrator"
    and:
      def pub = Mock(StatPublisher)
      def orch = new StatsCollectionOrchestrator(pub)
      pub.publish(_ as String,_ as EdgeStatsBundle) >> { String cn, EdgeStatsBundle bundle -> throw new RuntimeException("bad") }
    and:
      def k1 = new KeyParts("cache1", UUID.randomUUID(), "2")
      def col1 = new StatKeyEventCollection(k1)
      col1.add(EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.EVENTSOURCE)
    when:
      def result = orch.squashAndPublish([(k1):col1])
    then:
      !result
  }

  def cleanup() {
    System.clearProperty('edge.stats.max-apis-per-publish')
  }
}
