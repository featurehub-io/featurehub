package io.featurehub.mr.events.service

import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.api.SingleNullableFeatureValueUpdate
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.events.CloudEventPublisher
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.messaging.model.MessagingFeatureValueUpdate
import io.featurehub.mr.events.common.DbFeatureTestProvider
import io.featurehub.mr.events.common.converter.FeatureMessagingConverter
import io.featurehub.mr.events.common.converter.FeatureMessagingParameter
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneOffset

class FeatureMessagingCloudEventPublisherImplSpec extends Specification {
  FeatureMessagingCloudEventPublisherImpl featureMessagingCloudEventPublisher
  CloudEventPublisher cloudEventPublisher
  FeatureMessagingConverter featureMessagingConverter
  DbFeatureValue dbFeatureValue

  def setup() {
    cloudEventPublisher = Mock()
    featureMessagingConverter = Mock()
    featureMessagingCloudEventPublisher = new FeatureMessagingCloudEventPublisherImpl(featureMessagingConverter, cloudEventPublisher)
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
    1 * featureMessagingConverter.toFeatureMessagingUpdate(featureMessagingParameter) >> featureMessagingUpdate
    1 * cloudEventPublisher.publish(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, featureMessagingUpdate, _)
    0 * _
  }
}
