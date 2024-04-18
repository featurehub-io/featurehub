package io.featurehub.events

import cd.connect.cloudevents.CloudEventSubject
import cd.connect.cloudevents.CloudEventType
import cd.connect.cloudevents.TaggedCloudEvent
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import spock.lang.Specification

import java.time.OffsetDateTime

class CloudEventReceiverRegistrySpec extends Specification {
  CloudEventReceiverRegistryInternal reg
  FakeOpenTelemetryWriter writer
  FakeExecutorService executorService

  def setup() {
    writer = new FakeOpenTelemetryWriter()
    executorService = new FakeExecutorService()
    reg = new CloudEventReceiverRegistryInternal("common", writer, executorService)
  }

  @CloudEventType("simple1")
  @CloudEventSubject("io.featurehub.events.messaging")
  static class Simple1 implements TaggedCloudEvent {
  }

  CloudEvent makeCE() {
    return CloudEventBuilder.v1().withId(UUID.randomUUID().toString()).withTime(OffsetDateTime.now())
      .withType("simple1").withSubject("io.featurehub.events.messaging")
      .withSource(new URI("http://mr")).withData("application/json", "{}".bytes).build()
  }

  def "when we have two listeners (and one uninterested one), they will both get a cloud event message with the open telemetry wrapper"() {
    given: "we have a cloud event"
      def ce = makeCE()
    and: "i register two simple listeners"
      def l1 = new FakeSimpleRegistryListener(Simple1, reg)
      def l2 = new FakeSubjectRegistryListener(Simple1, "simple1", "io.featurehub.events.messaging", reg)
    and: "one for the wrong subject"
      def l3 = new FakeSubjectRegistryListener(Simple1, "simple1", "not-called", reg)
    when: "i send the message"
      reg.process(ce)
    then:
      writer.simpleTrigger == 1 // its only unwrapped once
      l1.data instanceof Simple1
      l1.ce == ce
      l2.data instanceof Simple1
      l2.ce == ce
      l3.ce == null
      l3.data == null
  }

  def "if we have two listeners, and the message is delivered directly, they will receive it without open telemetry"() {
    given: "we have a cloud event"
      def ce = makeCE()
    and: "i register two simple listeners"
      def l1 = new FakeTypeRegistryListener(Simple1, "simple1", reg)
      def l2 = new FakeSubjectRegistryListener(Simple1, "simple1", "io.featurehub.events.messaging", reg)
    and: "one for the wrong subject"
      def l3 = new FakeSubjectRegistryListener(Simple1, "simple1", "not-called", reg)
    when: "i send the message"
      reg.process(new Simple1(), ce)
    then:
      writer.simpleTrigger == 0
      l1.ce == ce
      l2.ce == ce
  }
}
