package io.featurehub.edge.stats

import io.featurehub.publish.ChannelNames
import io.featurehub.publish.NATSSource
import io.featurehub.sse.stats.model.EdgeStatsBundle
import io.nats.client.Connection
import spock.lang.Specification

import java.time.OffsetDateTime

class NATSStatPublisherSpec extends Specification {
  def "when we pass in a bundle, it gets published"() {
    given: "we have a NATS Source"
      def nSource = Mock(NATSSource)
      def nConn = Mock(Connection)
      nSource.connection >> nConn
    and: "a publisher"
      def pub = new NATSStatPublisher(nSource)
    when: "i publish a bundle"
      pub.publish("sausage", new EdgeStatsBundle().misses(0).timestamp(OffsetDateTime.now()))
    then:
      1 * nConn.publish(ChannelNames.edgeStatsChannel("sausage"), _)
  }

  def "a failed publish doesn't bubble out"() {
    given: "we have a NATS Source"
      def nSource = Mock(NATSSource) // connection will cause NPE
    and: "a publisher"
      def pub = new NATSStatPublisher(nSource)
    when: "i publish a bundle"
      pub.publish("sausage", new EdgeStatsBundle().misses(0).timestamp(OffsetDateTime.now()))
    then:
      1 == 1
  }
}
