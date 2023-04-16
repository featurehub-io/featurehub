package io.featurehub.mr.events.service

import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.events.CloudEventPublisher
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.messaging.model.MessagingFeatureValueUpdate
import io.featurehub.mr.events.common.FeatureSetup
import io.featurehub.mr.events.common.converter.FeatureMessagingConverter
import io.featurehub.mr.events.common.converter.FeatureMessagingParameter
import spock.lang.Shared
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneOffset

class FeatureMessagingCloudEventPublisherImplSpec extends Specification {
  FeatureMessagingCloudEventPublisherImpl featureMessagingCloudEventPublisher
  CloudEventPublisher cloudEventPublisher
  FeatureMessagingConverter featureMessagingConverter
  @Shared
  FeatureSetup featureSetup

  def setupSpec() {
    featureSetup = new FeatureSetup()
  }

  void setup() {
    cloudEventPublisher = Mock(CloudEventPublisher)
    featureMessagingConverter = Mock(FeatureMessagingConverter)
    featureMessagingCloudEventPublisher = new FeatureMessagingCloudEventPublisherImpl(featureMessagingConverter, cloudEventPublisher)
  }


  def "should publish FeatureMessagingUpdate cloud event"() {
    given:
    def dbFeature = featureSetup.createFeature()
    def oldFeatureValue = "old"
    def defaultValueUpdate = new SingleFeatureValueUpdate<String>(
      true, dbFeature.defaultValue, oldFeatureValue)
    def featureMessagingUpdate = new FeatureMessagingUpdate()
      .whoUpdated("Alfie")
      .whenUpdated(LocalDateTime.now().atOffset(ZoneOffset.UTC))
      .portfolioId(dbFeature.environment.parentApplication.portfolio.id)
      .environmentId(dbFeature.environment.id)
      .applicationId(dbFeature.environment.parentApplication.id)
      .featureValueUpdated(new MessagingFeatureValueUpdate()
        .updated(dbFeature.defaultValue)
        .previous(oldFeatureValue))
    def featureMessagingParameter = new FeatureMessagingParameter(dbFeature, null, defaultValueUpdate, null, null)

    when:
    featureMessagingCloudEventPublisher.publishFeatureMessagingUpdate(featureMessagingParameter)

    then:
    1 * featureMessagingConverter.toFeatureMessagingUpdate(featureMessagingParameter) >> featureMessagingUpdate
    1 * cloudEventPublisher.publish(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, featureMessagingUpdate, _)
  }
}
