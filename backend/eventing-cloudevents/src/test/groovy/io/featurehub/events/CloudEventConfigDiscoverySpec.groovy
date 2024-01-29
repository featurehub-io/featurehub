package io.featurehub.events

import spock.lang.Specification

class CloudEventConfigDiscoverySpec extends Specification {
  CloudEventPublisherRegistry publisherRegistry
  CloudEventReceiverRegistry receiverRegistry
  CloudEventConfigDiscoveryService svc

  def setup() {
    publisherRegistry = Mock()
    receiverRegistry = Mock()

    svc = new CloudEventConfigDiscoveryService(publisherRegistry, receiverRegistry)
  }

  def "it should be able to find the cloud events definitions but find no publishers or subscribers because it isn't matching any tags"() {
    given:
      CloudEventConfigDiscoveryProcessor proc = Mock()
    when:
      svc.discover("nats", proc)
    then:
      0 * proc.processPublisher(_, _)
      0 * proc.processSubscriber(_, _)
  }
}
