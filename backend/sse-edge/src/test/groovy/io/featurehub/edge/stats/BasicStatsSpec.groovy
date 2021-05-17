package io.featurehub.edge.stats

import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import spock.lang.Specification
import com.lmax.disruptor.EventHandler

class BasicStatsSpec extends Specification {
  def "a disruptor must have used a bucket size of a factor of 2"() {
    given: "i use a factor of 2"
      System.setProperty("edge.stats.disruptor-buffer-size", "64")
    when: "i create an instance"
      def sd = new StatDisruptor(Mock(EventHandler<Stat>))
    then:
      sd != null
  }

  def "a disruptor must not have used a bucket size that isn't a factor of 2"() {
    given: "i don't use a factor of 2"
      System.setProperty("edge.stats.disruptor-buffer-size", "63")
    when: "i create an instance"
      def sd = new StatDisruptor(Mock(EventHandler<Stat>))
    then:
      thrown(RuntimeException)
  }

  def "the event factory gives me new empty stat objects"() {
    when: "i ask for a new stat"
      def s = new StatEventFactory().newInstance()
    then:
      s != null
  }

  def "stat beans work as expected"() {
    when: "i set up a stat bean"
      def s = new Stat()
      s.resultType = EdgeHitResultType.FORBIDDEN
      s.hitSourceType = EdgeHitSourceType.TESTSDK
      s.apiKey = new KeyParts("a", "b", 'c"')
    then:
      s.resultType == EdgeHitResultType.FORBIDDEN
      s.hitSourceType == EdgeHitSourceType.TESTSDK
      s.apiKey.equals( new KeyParts("a", "b", 'c"'))
  }

  def "the stats counter works as expected"() {
    when: "i set up  the counter"
      def sc = new StatCounter(EdgeHitResultType.FORBIDDEN, EdgeHitSourceType.EVENTSOURCE)
      sc.counter.incrementAndGet()
      sc.counter.incrementAndGet()
    then:
      sc.resultType == EdgeHitResultType.FORBIDDEN
      sc.hitSourceType == EdgeHitSourceType.EVENTSOURCE
      sc.counter.longValue() == 2
  }

  // can't really do much more because of the nature of the lmax in terms of stubbing its thread factory

  def cleanup() {
    System.clearProperty("edge.stats.disruptor-buffer-size");
    System.clearProperty("edge.stats.publish-misses")
  }
}
