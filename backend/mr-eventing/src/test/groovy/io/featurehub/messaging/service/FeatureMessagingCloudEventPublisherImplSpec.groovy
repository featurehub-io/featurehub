package io.featurehub.messaging.service

import io.featurehub.db.api.CloudEventLinkType
import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.api.SingleNullableFeatureValueUpdate
import io.featurehub.db.api.TrackingEventApi
import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.events.DynamicCloudEventDestination
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.messaging.model.MessagingFeatureValueUpdate
import io.featurehub.utils.ExecutorSupplier
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
  TrackingEventApi trackingEventApi
  UUID orgId
  DynamicCloudEventDestination hook

  def setup() {
    orgId = UUID.randomUUID()
    publisher = Mock()
    dynamicPublisherRegistry = Mock()
    executorSupplier = Mock()
    executor = Mock()
    trackingEventApi = Mock()
    executorSupplier.executorService(_) >> executor
    cePublisher = new FeatureMessagingCloudEventPublisherImpl(executorSupplier, trackingEventApi)
    dbFeatureValue = DbFeatureTestProvider.provideFeatureValue()
    version = new SingleNullableFeatureValueUpdate<>(true, 1L, null)
    hook = Mock()
  }

  FeatureMessagingUpdate fmUpdate() {
    def oldFeatureValue = "old"
    return new FeatureMessagingUpdate()
      .whoUpdated("Alfie")
      .organizationId(orgId)
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
      cePublisher = new FeatureMessagingCloudEventPublisherImpl(executorSupplier, trackingEventApi) {
        public FeatureMessagingUpdate toFeatureMessagingUpdate(FeatureMessagingParameter featureMessagingParam) {
          return featureMessagingUpdate
        }
      }
    and:
      cePublisher.addHook(hook)
    when:
      cePublisher.publish(featureMessagingParameter, orgId)
    then:
      1 * executor.submit(_) >> { Runnable task -> task.run() }
      2 * hook.enabled([:], orgId) >> true
      1 * hook.getCloudEventType() >> "blah/blah"
      1 * trackingEventApi.createInitialRecord(_, "blah/blah", featureMessagingUpdate.organizationId,
        CloudEventLinkType.env, featureMessagingUpdate.environmentId, _, _, _)
      1 * hook.publish('messaging-feature-v1', orgId, _, _, _)
      0 * _
  }

  def "should not publish FeatureMessagingUpdate when messaging publish is not enabled"() {
    given:
      def featureMessagingParameter = fmParam()
    and:
      cePublisher = new FeatureMessagingCloudEventPublisherImpl(executorSupplier, trackingEventApi)
    when:
      cePublisher.publish(featureMessagingParameter, orgId)
    then:
      0 * _

  }

}
