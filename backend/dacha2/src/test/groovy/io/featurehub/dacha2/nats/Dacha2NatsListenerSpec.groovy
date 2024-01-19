package io.featurehub.dacha2.nats

import io.featurehub.enricher.FeatureEnricher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.nats.NatsListener
import io.featurehub.publish.NATSSource
import spock.lang.Specification

class Dacha2NatsListenerSpec extends Specification {
  NATSSource nats
  Dacha2NatsListener listener
  CloudEventReceiverRegistry eventListener
  NatsListener natsListener
  FeatureEnricher featureEnricher

  def setup() {
    nats = Mock()
    natsListener = Mock()
    eventListener = Mock()
    featureEnricher = Mock()
  }

  def make() {
    listener = new Dacha2NatsListener(nats, eventListener, featureEnricher)
  }

  def "a listener to will process the message"() {
    when:
      make()
      listener.started()
      listener.shutdown()
    then:
      1 * nats.createTopicListener(_, _,) >> natsListener
      1 * featureEnricher.enabled >> false
      1 * natsListener.close()
  }
}
