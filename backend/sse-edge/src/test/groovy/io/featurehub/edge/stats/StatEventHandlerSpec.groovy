package io.featurehub.edge.stats

import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import spock.lang.Specification

import java.util.concurrent.Executor

class StatEventHandlerSpec extends Specification {
  def "we deliver 3 events across two API keys they are collected and we can grab them and they aren't there on  the next call"() {
    given: "we have a event handler"
      def eh = new StatEventHandler(Mock(StatsOrchestrator))
      def env2 = UUID.randomUUID()
      def envB = UUID.randomUUID()
    and: "we push three sets of data in"
      eh.onEvent(Stat.@Companion.create(new KeyParts("1", env2, '3'),
        EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.EVENTSOURCE), 1, false)
      eh.onEvent(Stat.@Companion.create(new KeyParts("1", env2, '3'),
        EdgeHitResultType.SUCCESS, EdgeHitSourceType.EVENTSOURCE), 1, false)
      eh.onEvent(Stat.@Companion.create(new KeyParts("a", envB, 'c'),
        EdgeHitResultType.SUCCESS, EdgeHitSourceType.EVENTSOURCE), 1, false)
    when: "we collect it"
      def data = eh.ejectData()
    and: "we insert some more"
      eh.onEvent(Stat.@Companion.create(new KeyParts("a", envB, 'c'),
        EdgeHitResultType.SUCCESS, EdgeHitSourceType.EVENTSOURCE), 1, false)
      def data2 = eh.ejectData()
    then:
      data.size() == 2
      data[new KeyParts("1", env2, '3')].size() == 2
      data[new KeyParts("a", envB, 'c')].size() == 1
      data2[new KeyParts("a", envB, 'c')].size() == 1
  }

  def "we set the panic size to 1 and this will cause it to eject data for every request"() {
    given: "we have a event handler"
      def orh = Mock(StatsOrchestrator)
      System.setProperty('edge.stats.panic-threshold', '1')
      def eh = new StatEventHandler(orh) {
        @Override
        protected Executor makeExecutor() {
          return {it.run() } as Executor
        }
      }
      def env2 = UUID.randomUUID()
      def envB = UUID.randomUUID()
    when: "I emit 3x data"
      eh.onEvent(Stat.@Companion.create(new KeyParts("1", env2, '3'),
        EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.EVENTSOURCE), 1, false)
      eh.onEvent(Stat.@Companion.create(new KeyParts("1", env2, '3'),
        EdgeHitResultType.SUCCESS, EdgeHitSourceType.EVENTSOURCE), 1, false)
      eh.onEvent(Stat.@Companion.create(new KeyParts("a", envB, 'c'),
        EdgeHitResultType.SUCCESS, EdgeHitSourceType.EVENTSOURCE), 1, false)
    then:
      3 * orh.squashAndPublish(_)
  }

  def cleanup() {
    System.clearProperty('edge.stats.panic-threshold')
  }
}
