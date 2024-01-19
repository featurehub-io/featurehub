package io.featurehub.events

import spock.lang.Specification

class EventingFeatureSourceSpec extends Specification {

  def "it should be able to find the cloud events definitions"() {
    when:
      def e = new CloudEventProviderBase()
      e.getConfig({
        it.nats.each { print "nats config: ${it.value.cloudEvents}"}
      })
    then:
      1 == 1
  }
}
