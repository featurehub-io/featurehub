package io.featurehub.dacha2.nats


import io.featurehub.dacha2.Dacha2CloudEventListener
import io.featurehub.events.nats.NatsListener
import io.featurehub.publish.NATSSource
import spock.lang.Specification

class Dacha2NatsListenerSpec extends Specification {
  NATSSource nats
  Dacha2NatsListener listener
  Dacha2CloudEventListener eventListener
  NatsListener natsListener

  def setup() {
    nats = Mock()
    natsListener = Mock()
    eventListener = Mock()
  }

  def make() {
    listener = new Dacha2NatsListener(nats, eventListener)
  }

  def "a listener to will process the message"() {
    when:
      listener = new Dacha2NatsListener(nats, eventListener)
      listener.shutdown()
    then:
      1 * nats.createTopicListener(_, _,) >> natsListener
      1 * natsListener.close()
  }
}
