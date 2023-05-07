package io.featurehub.enricher

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.enriched.model.EnrichedFeatures
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.CloudEventsTelemetryReader
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.model.FeatureValueType
import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull
import spock.lang.Specification

class FeatureEnricherProcessorSpec extends Specification {
  CloudEventsTelemetryReader telemetryReader
  FeatureEnrichmentCache cache
  CloudEventPublisher publisher
  FeatureEnricherProcessor enricher

  static class FakeTelemetryReader implements CloudEventsTelemetryReader {
    @Override
    void receive(@NotNull String subject, @NotNull CloudEvent event, @NotNull CloudEventChannelMetric metrics, @NotNull Function1<? super CloudEvent, Unit> process) {
      process(event)
    }

    @Override
    void receive(@NotNull CloudEvent event, @NotNull Function1<? super CloudEvent, Unit> process) {
      process(event)
    }
  }

  def setup() {
    ThreadLocalConfigurationSource.createContext(['enricher.enabled': 'true', 'enricher.ignore-when-empty': 'false'])
    telemetryReader = Spy(FakeTelemetryReader)
    cache = Mock()
    publisher = Mock()
    enricher = new FeatureEnricherProcessor(telemetryReader, cache, publisher)
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  CloudEvent event(List<PublishFeatureValue> features) {
    def eventBuilder = new CloudEventBuilder().newBuilder()
      .withId("x").withType(PublishFeatureValues.CLOUD_EVENT_TYPE).withSource(URI.create("http://localhost"))
    CacheJsonMapper.toEventData(eventBuilder, new PublishFeatureValues().features(features), false)
    return eventBuilder.build()
  }

  def "if the message is not a feature message, we ignore it"() {
    given:
      def ce = new CloudEventBuilder().newBuilder()
        .withId("x")
        .withType("toot").withSource(URI.create("http://localhost")).build()
    when:
      enricher.enrich(ce)
    then:
      1 * telemetryReader.receive(ce, _)
      0 * _
  }

  def "if the message has no features, it will be processed but nothing will be done"() {
    given:
      def ce = event([])
    when:
      enricher.enrich(ce)
    then:
      1 * telemetryReader.receive(ce, _)
      0 * _
  }

  def "the metric exists"() {
    when:
      def metric = enricher.metric()
    then:
      metric
  }

  PublishFeatureValue feature(UUID envId) {
    return new PublishFeatureValue()
      .action(PublishAction.CREATE)
      .environmentId(envId)
      .feature(
        new CacheEnvironmentFeature()
          .feature(
            new CacheFeature()
              .valueType(FeatureValueType.BOOLEAN)
              .version(6)
              .id(UUID.randomUUID())
              .key("FRED")))
  }

  def "if the environment does not exist, it ignores the message"() {
    given:
      def envId = UUID.randomUUID()
      def feature = feature(envId)
      def ce = event([feature])
    when:
      enricher.enrich(ce)
    then:
      1 * telemetryReader.receive(ce, _)
      1 * cache.updateFeature(feature)
      1 * cache.getEnrichableEnvironment(envId) >> { -> throw new RuntimeException() }
      0 * _
  }

  def "if the message has a feature, it will be processed"() {
    given:
      def envId = UUID.randomUUID()
      def feature = feature(envId)
      def ce = event([feature])
    and: "we have the environment"
      def environment = new PublishEnvironment()
        .action(PublishAction.CREATE)
        .count(0)
        .organizationId(UUID.randomUUID())
        .portfolioId(UUID.randomUUID())
        .applicationId(UUID.randomUUID())
        .environment(new CacheEnvironment().id(envId))
      def enrichedEnv = new EnrichmentEnvironment([], environment)
    when:
      enricher.enrich(ce)
    then:
      1 * telemetryReader.receive(ce, _)
      1 * cache.updateFeature(feature)
      1 * cache.getEnrichableEnvironment(envId) >> enrichedEnv
      1 * publisher.publish(EnrichedFeatures.CLOUD_EVENT_TYPE, { EnrichedFeatures ef ->
        ef.environment.environment.id == envId
      }, _)
      0 * _
  }
}
