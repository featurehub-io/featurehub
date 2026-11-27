package io.featurehub.events.local

import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.events.CloudEventBaseReceiverRegistry
import io.featurehub.events.CloudEventConfigDiscoveryService
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.rest.Info
import io.featurehub.utils.ExecutorSupplier
import kotlin.jvm.functions.Function1
import spock.lang.Specification

import java.time.OffsetDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LocalConfiguredSourceSpec extends Specification {
  CloudEventPublisherRegistry publisherRegistry
  CloudEventReceiverRegistry receiverRegistry
  CloudEventBaseReceiverRegistry enricherRegistry
  ExecutorSupplier executorSupplier
  List<ExecutorService> executors
  Map<String, List<Function1<CloudEvent, Object>>> publishHandlers
  List<Boolean> compressFlags
  LocalConfiguredSource source

  def setup() {
    publishHandlers = [:]
    compressFlags = []
    executors = []

    publisherRegistry = Stub()
    publisherRegistry.registerForPublishing(_, _, _, _) >> { String type, metric, boolean compress, Function1 handler ->
      publishHandlers.computeIfAbsent(type) { [] }.add(handler)
      compressFlags.add(compress)
    }

    receiverRegistry = Mock()
    enricherRegistry = Mock()

    executorSupplier = Stub()
    executorSupplier.executorService(_) >> {
      def ex = Executors.newSingleThreadExecutor()
      executors.add(ex)
      ex
    }

    CloudEventConfigDiscoveryService.@Companion.clearTags()
  }

  def cleanup() {
    CloudEventConfigDiscoveryService.@Companion.clearTags()
    ['dacha1.enabled', 'dacha2.enabled', 'webhooks.features.enabled', Info.APPLICATION_NAME_PROPERTY].each {
      System.clearProperty(it)
    }
    executors.each { it.shutdownNow() }
  }

  void partyServer() {
    System.setProperty('dacha1.enabled', 'false')
    System.setProperty('dacha2.enabled', 'true')
    System.setProperty(Info.APPLICATION_NAME_PROPERTY, 'party-server')
    CloudEventConfigDiscoveryService.@Companion.addTags('dacha2', 'edge-dacha2', 'mr', 'mr-dacha2')

    def discovery = new CloudEventConfigDiscoveryService(publisherRegistry, receiverRegistry)
    source = new LocalConfiguredSource(discovery, executorSupplier)
    source.started()
  }

  CloudEvent ce(String type) {
    return CloudEventBuilder.v1().withId(UUID.randomUUID().toString()).withTime(OffsetDateTime.now())
      .withType(type).withSubject('io.featurehub.events.kwong-pastry')
      .withSource(new URI('http://party-server')).withData('application/json', '{"name":"lingling"}'.bytes).build()
  }

  void publish(String type, CloudEvent event = ce(type)) {
    def handlers = publishHandlers[type]
    assert handlers != null : "no publisher registered for ${type}"
    handlers.each { it.invoke(event) }
  }

  void drain() {
    executors.each { it.shutdown() }
    executors.each { assert it.awaitTermination(5, TimeUnit.SECONDS) }
  }

  def "publish-feature-v1 has exactly one publisher and arrives in the common registry exactly once"() {
    given:
      partyServer()
    when:
      publish('publish-feature-v1')
      drain()
    then:
      publishHandlers['publish-feature-v1'].size() == 1
      1 * receiverRegistry.process({ CloudEvent e -> e.type == 'publish-feature-v1' })
      0 * enricherRegistry.process(_)
  }

  def "environment and service account updates on the cache channel reach the common registry"() {
    given:
      partyServer()
    when:
      publish('publish-environment-v1')
      publish('publish-serviceaccount-v1')
      drain()
    then:
      1 * receiverRegistry.process({ CloudEvent e -> e.type == 'publish-environment-v1' })
      1 * receiverRegistry.process({ CloudEvent e -> e.type == 'publish-serviceaccount-v1' })
  }

  def "enricher-ping-v1 reaches the enricher registry and not the common one"() {
    given:
      receiverRegistry.registry('enricher') >> enricherRegistry
      partyServer()
    when:
      publish('enricher-ping-v1')
      drain()
    then:
      1 * enricherRegistry.process({ CloudEvent e -> e.type == 'enricher-ping-v1' })
      0 * receiverRegistry.process(_)
  }

  def "feature-update-v1 from edge reaches mr"() {
    given:
      partyServer()
    when:
      publish('feature-update-v1')
      drain()
    then:
      1 * receiverRegistry.process({ CloudEvent e -> e.type == 'feature-update-v1' })
  }

  def "enriched features from dacha2 reach edge, regardless of the channel conditional, the same as nats"() {
    given:
      partyServer()
    when:
      publish('enriched-feature-v1')
      drain()
    then:
      1 * receiverRegistry.process({ CloudEvent e -> e.type == 'enriched-feature-v1' })
  }

  def "nothing is compressed in-process"() {
    when:
      partyServer()
    then:
      !compressFlags.isEmpty()
      compressFlags.every { !it }
  }

  def "a channel with publishers but no subscribers drops events without error"() {
    given: "the usage emitter is part of this build"
      CloudEventConfigDiscoveryService.@Companion.addTags('fhos-usage-emitter')
      partyServer()
    when:
      publish('edge-stat-bundle-v1')
      drain()
    then:
      source.subscribers('edge-stats-emitter').isEmpty()
      0 * receiverRegistry.process(_)
      noExceptionThrown()
  }

  def "order is preserved for a burst of events to one subscriber"() {
    given:
      List<String> received = Collections.synchronizedList([])
      receiverRegistry.process(_) >> { CloudEvent e -> received.add(e.id) }
      partyServer()
    and:
      def events = (1..500).collect { ce('publish-feature-v1') }
    when:
      events.each { publish('publish-feature-v1', it) }
      drain()
    then:
      received == events*.id
  }

  def "a failing subscriber does not stop later events being delivered"() {
    given:
      int count = 0
      receiverRegistry.process(_) >> { CloudEvent e ->
        count++
        if (count == 1) throw new RuntimeException('keep silent')
      }
      partyServer()
    when:
      publish('publish-feature-v1')
      publish('publish-feature-v1')
      drain()
    then:
      count == 2
  }

  def "after shutdown events are dropped"() {
    given:
      partyServer()
    when:
      source.shutdown()
      publish('publish-feature-v1')
      drain()
    then:
      0 * receiverRegistry.process(_)
  }
}
