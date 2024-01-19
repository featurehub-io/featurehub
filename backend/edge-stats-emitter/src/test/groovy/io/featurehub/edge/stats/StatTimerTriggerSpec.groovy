package io.featurehub.edge.stats

import spock.lang.Specification

class StatTimerTriggerSpec extends Specification {
  def "A timer will get kicked off"() {
    given: "i set up a mock collector"
      def statCollector = Mock(StatCollector)
      def statsOrchestrator = Mock(StatsOrchestrator)
    and: "set the stats publish interval"
      System.setProperty('edge.stats.publish-interval-ms', '1') // 1 ms
    and: "create the trigger, starting off the timer"
      def trigger = new StatTimeTrigger(statCollector, statsOrchestrator)
      trigger.started()
    when: "i wait to ensure the timer has gone off"
      Thread.sleep(50)
    then: "make sure process was called one or more times"
      (1.._) * statsOrchestrator.squashAndPublish(_)
      (1.._) * statCollector.ejectData()
    cleanup:
      System.clearProperty('edge.stats.publish-interval-ms')
      trigger.shutdown()
  }
}
