package io.featurehub.messaging.service

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.messaging.common.DbFeatureTestProvider
import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.api.SingleNullableFeatureValueUpdate
import io.featurehub.events.CloudEventPublisher
import io.featurehub.messaging.converter.FeatureMessagingConverter
import io.featurehub.messaging.converter.FeatureMessagingParameter
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.messaging.model.MessagingFeatureValueUpdate
import io.featurehub.utils.ExecutorSupplier
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ExecutorService

class FeatureMessagingCloudEventPublisherImplSpec extends Specification {
  FeatureMessagingCloudEventPublisherImpl featureMessagingCloudEventPublisher
  CloudEventPublisher cloudEventPublisher
  FeatureMessagingConverter featureMessagingConverter
  DbFeatureValue dbFeatureValue
  ExecutorSupplier executorSupplier
  ExecutorService executor

  def setup() {
    cloudEventPublisher = Mock()
    featureMessagingConverter = Mock()
    executorSupplier = Mock()
    executor = Mock()
    executorSupplier.executorService(_) >> executor
    ThreadLocalConfigurationSource.createContext(['messaging.publisher.thread-pool': "1", 'messaging.publish.enabled': "true"])

    featureMessagingCloudEventPublisher = new FeatureMessagingCloudEventPublisherImpl(featureMessagingConverter, cloudEventPublisher,executorSupplier)
    dbFeatureValue = DbFeatureTestProvider.provideFeatureValue()
  }


  def "should publish FeatureMessagingUpdate cloud event"() {
    given:
    def oldFeatureValue = "old"
    def defaultValueUpdate = new SingleNullableFeatureValueUpdate<String>(
      true, dbFeatureValue.defaultValue, oldFeatureValue)
    def lockUpdate = new SingleFeatureValueUpdate(false, false, false)
    def retiredUpdate = new SingleFeatureValueUpdate(false, false, false)
    def strategiesUpdate = new MultiFeatureValueUpdate(false, [], [], [])
    def featureMessagingUpdate = new FeatureMessagingUpdate()
      .whoUpdated("Alfie")
      .whenUpdated(LocalDateTime.now().atOffset(ZoneOffset.UTC))
      .portfolioId(dbFeatureValue.environment.parentApplication.portfolio.id)
      .environmentId(dbFeatureValue.environment.id)
      .applicationId(dbFeatureValue.environment.parentApplication.id)
      .featureValueUpdated(new MessagingFeatureValueUpdate()
        .updated(dbFeatureValue.defaultValue)
        .previous(oldFeatureValue))
    def featureMessagingParameter = new FeatureMessagingParameter(dbFeatureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategiesUpdate)

    when:
    featureMessagingCloudEventPublisher.publishFeatureMessagingUpdate(featureMessagingParameter)

    then:
    1 * executor.submit(_) >> { Runnable task -> task.run() }
    1 * featureMessagingConverter.toFeatureMessagingUpdate(featureMessagingParameter) >> featureMessagingUpdate
    1 * cloudEventPublisher.publish(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, featureMessagingUpdate, _)
    0 * _
  }

  def "should not publish FeatureMessagingUpdate when messaging publish is not enabled"() {
    given: "i have enabled the message publisher for changes"
    ThreadLocalConfigurationSource.createContext(["messaging.publish.enabled": "false", "messaging.publisher.thread-pool": "17"])
    and:
    featureMessagingCloudEventPublisher = new FeatureMessagingCloudEventPublisherImpl(featureMessagingConverter, cloudEventPublisher,executorSupplier)
    def defaultValueUpdate = new SingleNullableFeatureValueUpdate<String>(
      true, dbFeatureValue.defaultValue, "old")
    def lockUpdate = new SingleFeatureValueUpdate(false, false, false)
    def retiredUpdate = new SingleFeatureValueUpdate(false, false, false)
    def strategiesUpdate = new MultiFeatureValueUpdate(false, [], [], [])
    def featureMessagingUpdate = new FeatureMessagingUpdate()
      .whoUpdated("Alfie")
      .whenUpdated(LocalDateTime.now().atOffset(ZoneOffset.UTC))
      .portfolioId(dbFeatureValue.environment.parentApplication.portfolio.id)
      .environmentId(dbFeatureValue.environment.id)
      .applicationId(dbFeatureValue.environment.parentApplication.id)
      .featureValueUpdated(new MessagingFeatureValueUpdate()
        .updated(dbFeatureValue.defaultValue)
        .previous("old"))
    def featureMessagingParameter = new FeatureMessagingParameter(dbFeatureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategiesUpdate)

    when:
    featureMessagingCloudEventPublisher.publishFeatureMessagingUpdate(featureMessagingParameter)
    then:
    0 * executor.submit(_) >> { Runnable task -> task.run() }
    0 * featureMessagingConverter.toFeatureMessagingUpdate(featureMessagingParameter) >> featureMessagingUpdate
    0 * cloudEventPublisher.publish(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, featureMessagingUpdate, _)


  }

}
