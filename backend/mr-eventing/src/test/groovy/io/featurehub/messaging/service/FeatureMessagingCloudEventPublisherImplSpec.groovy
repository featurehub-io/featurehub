package io.featurehub.messaging.service

import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.api.SingleNullableFeatureValueUpdate
import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.events.DynamicCloudEventDestination
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.messaging.model.MessagingFeatureValueUpdate
import io.featurehub.utils.ExecutorSupplier
import org.jetbrains.annotations.NotNull
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ExecutorService

class FeatureMessagingCloudEventPublisherImplSpec extends Specification {
  FeatureMessagingCloudEventPublisherImpl cePublisher
  DbFeatureValue dbFeatureValue
  CloudEventPublisherRegistry publisher
  SingleNullableFeatureValueUpdate<Long> version
  ExecutorSupplier executorSupplier
  ExecutorService executor
  CloudEventDynamicPublisherRegistry dynamicPublisherRegistry

  static class FakeDynamicPublisherDestination implements DynamicCloudEventDestination {
    final String destination = "https://blah"
    final Map<String,String> headers
    final String cloudEventType

    FakeDynamicPublisherDestination(String cloudEventType, Map<String, String> headers) {
      this.cloudEventType = cloudEventType
      this.headers = headers
    }

//    @Override
//    String getDestination() {
//      return this.destination
//    }
//
//    @Override
//    Map<String, String> getHeaders() {
//      return this.headers
//    }

    @Override
    Map<String, String> additionalProperties(@NotNull Map<String, String> sourceWebhookMap) {
      return [:]
    }

    @Override
    String getConfigInfix() {
      return "fake"
    }
  }

  def setup() {
    publisher = Mock()
    dynamicPublisherRegistry = Mock()
    executorSupplier = Mock()
    executor = Mock()
    executorSupplier.executorService(_) >> executor
    cePublisher = new FeatureMessagingCloudEventPublisherImpl(publisher, executorSupplier)
    dbFeatureValue = DbFeatureTestProvider.provideFeatureValue()
    version = new SingleNullableFeatureValueUpdate<>(true, 1L, null)
  }

  FeatureMessagingUpdate fmUpdate() {
    def oldFeatureValue = "old"
    return new FeatureMessagingUpdate()
      .whoUpdated("Alfie")
      .whenUpdated(LocalDateTime.now().atOffset(ZoneOffset.UTC))
      .portfolioId(dbFeatureValue.environment.parentApplication.portfolio.id)
      .environmentId(dbFeatureValue.environment.id)
      .applicationId(dbFeatureValue.environment.parentApplication.id)
      .featureValueUpdated(new MessagingFeatureValueUpdate()
        .updated(dbFeatureValue.defaultValue)
        .previous(oldFeatureValue))
  }

  FeatureMessagingParameter fmParam() {
    def oldFeatureValue = "old"
    def defaultValueUpdate = new SingleNullableFeatureValueUpdate<String>(
      true, dbFeatureValue.defaultValue, oldFeatureValue)
    def lockUpdate = new SingleFeatureValueUpdate(false, false, false)
    def retiredUpdate = new SingleFeatureValueUpdate(false, false, false)
    def strategiesUpdate = new MultiFeatureValueUpdate(false, [], [], [])
    return new FeatureMessagingParameter(dbFeatureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategiesUpdate, version)
  }

  def "should publish FeatureMessagingUpdate cloud event"() {
    given:
      def featureMessagingUpdate = fmUpdate()
      def featureMessagingParameter = fmParam()
    and:
      cePublisher = new FeatureMessagingCloudEventPublisherImpl(publisher, executorSupplier) {
        public FeatureMessagingUpdate toFeatureMessagingUpdate(FeatureMessagingParameter featureMessagingParam) {
          return featureMessagingUpdate
        }
      }
    and:
      cePublisher.setHooks([new FakeDynamicPublisherDestination(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, [:])])
    when:
      cePublisher.publish(featureMessagingParameter)
    then:
      1 * executor.submit(_) >> { Runnable task -> task.run() }
      1 * publisher.publish(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, featureMessagingUpdate, _)
      0 * _
  }

  def "should not publish FeatureMessagingUpdate when messaging publish is not enabled"() {
    given:
      def featureMessagingParameter = fmParam()
    and:
      cePublisher = new FeatureMessagingCloudEventPublisherImpl(publisher, executorSupplier)
    when:
      cePublisher.publish(featureMessagingParameter)
    then:
      0 * _

  }

}
