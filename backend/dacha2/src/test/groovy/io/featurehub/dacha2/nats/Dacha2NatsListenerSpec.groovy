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
      3 * nats.createTopicListener(_, _,) >> natsListener
      3 * natsListener.close()
  }

//  def "a listener can process a message into a cloud event"() {
//    given: "a cloud event"
//      def event = CloudEventBuilder.v1().withSubject("x").withType("y")
//        .withId("z").withSource(new URI("http://localhost")).withData("text/plain", "text".bytes).build()
//    and: "a listener"
//      make()
//    when: "it is delivered"
//      listener.processEvent(NatsMessageFactory.createWriter().writeBinary(event))
//    then:
//      1 * eventListener.process({ CloudEvent evt ->
//        event.subject == evt.subject
//      })
//  }

//  def "processing fails"() {
//    given: "a cloud event"
//      def event = CloudEventBuilder.v1().withSubject("x").withType("y")
//        .withId("z").withSource(new URI("http://localhost")).withData("text/plain", "text".bytes).build()
//    and: "a listener"
//      make()
//    when: "it is delivered"
//      listener.processEvent(NatsMessageFactory.createWriter().writeBinary(event))
//    then:
//      1 * eventListener.process(_) >> { throw new RuntimeException("failed") }
//  }
}
