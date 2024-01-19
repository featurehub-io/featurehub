package io.featurehub.events

import spock.lang.Specification

class CloudEventConfigDiscoverySpec extends Specification {

  def "it should be able to find the cloud events definitions"() {
    when:
    CloudEventConfigDiscovery.@Companion.discover(CloudEventsStreamingLayer.nats, { it ->
        it.each { println it } })
    then:
      1 == 1
  }
}
