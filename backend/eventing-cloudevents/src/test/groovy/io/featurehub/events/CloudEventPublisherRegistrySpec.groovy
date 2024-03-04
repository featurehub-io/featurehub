package io.featurehub.events

import cd.connect.cloudevents.CloudEventSubject
import cd.connect.cloudevents.CloudEventType
import cd.connect.cloudevents.TaggedCloudEvent
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.metrics.MetricsCollector
import io.featurehub.utils.ExecutorSupplier
import spock.lang.Specification

import java.time.OffsetDateTime

class CloudEventPublisherRegistrySpec extends Specification {
  CloudEventPublisherRegistryProcessor proc
  FakeTelemetryWriteCallback telemetryWriter
  CloudEventReceiverRegistry receiverRegistry
  ExecutorSupplier executorSupplier
  CloudEventChannelMetric metric

  @CloudEventType("simple1")
  @CloudEventSubject("io.featurehub.events.messaging")
  static class Simple1 implements TaggedCloudEvent {
  }

  CloudEventBuilder makeCE() {
    return CloudEventBuilder.v1().withId(UUID.randomUUID().toString()).withTime(OffsetDateTime.now()).withSource(new URI("http://mr"))
  }

  def setup() {
    metric = new CloudEventChannelMetric(MetricsCollector.@Companion.counter("failures", "failures"),
      MetricsCollector.@Companion.histogram("perf", "perf"))
    telemetryWriter = new FakeTelemetryWriteCallback() // pass calls straight thru
    receiverRegistry = Mock()
    executorSupplier = Stub()

    executorSupplier.executorService(_) >> new FakeExecutorService()
    proc = new CloudEventPublisherRegistryProcessor(telemetryWriter, receiverRegistry, executorSupplier)
  }

  def "i can register a callback for a supported event and it will be triggered when the event comes in"() {
    given: "i have registered for callbacks"
      def fake1 = new FakePublishCallback("simple1", metric, false, proc)
    and: "we have a ce builder"
      def ceBuilder = makeCE()
    when: "i trigger an event"
      proc.publish("simple1", new Simple1(), ceBuilder)
    then:
      1 * receiverRegistry.hasListeners("simple1") >> false
      telemetryWriter.triggered == 1
      fake1.event.type == "simple1"
      fake1.event.subject == "io.featurehub.events.messaging"
      fake1.event.data != null
      fake1.text() == "{}"
      0 * _
  }

  def "i can have multiple handlers for the same event and also a receiver registry implementation"() {
    given: "i have two registered for callbacks"
      def fake1 = new FakePublishCallback("simple1", metric, false, proc)
      def fake2 = new FakePublishCallback("simple1", metric, false, proc)
    and: "we have a ce builder"
      def ceBuilder = makeCE()
      def data = new Simple1()
    when: "i trigger an event"
      proc.publish("simple1", data, ceBuilder)
    then:
      1 * receiverRegistry.hasListeners("simple1") >> true
      1 * receiverRegistry.process(data, { CloudEvent evt ->
        evt.type == 'simple1'
      })
      telemetryWriter.triggered == 2
      fake1.event != null
      fake2.event != null
      0 * _
  }

  def "if we publish an event and we have a compressed publisher and an uncompressed publisher, the content type is set correctly"() {
    given: "i have a compessed callbacks"
      def fake1 = new FakePublishCallback("simple1", metric, true, proc)
    and: "an uncompressed callback"
      def fake2 = new FakePublishCallback("simple1", metric, false, proc)
    and: "we have a ce builder"
      def ceBuilder = makeCE()
      def data = new Simple1()
    when: "i trigger an event"
      proc.publish("simple1", data, ceBuilder)
    then:
      1 * receiverRegistry.hasListeners("simple1") >> false
      fake1.event.dataContentType == 'application/json+gzip'
      fake2.event.dataContentType == 'application/json'
      0 * _
  }

  def "i try and publish an event that the publisher doesn't recognize and it asks the receiver registry"() {
    given: "i have a ce builder"
      def ceBuilder = makeCE()
      def data = new Simple1()
    when: "i publish the event"
      proc.publish("simple1", data, ceBuilder)
    then:
      1 * receiverRegistry.hasListeners("simple1") >> true
      1 * receiverRegistry.process(data, { CloudEvent evt ->
        evt.type == 'simple1'
      })
  }

  def "if i try and publish an event which is enriched"() {
    given: "i have simple data"
      def data = new Simple1()
    and: "an uncompressed callback"
      def fake2 = new FakePublishCallback("simple1", metric, false, proc)
    when: "i publish"
      def callback = new FakePublishEnricherCallback(proc)
      callback.publish(data)
    then:
      1 * receiverRegistry.hasListeners("simple1") >> false
      fake2.event.dataContentType == "application/json"
      callback.trigger == 1
  }
}
