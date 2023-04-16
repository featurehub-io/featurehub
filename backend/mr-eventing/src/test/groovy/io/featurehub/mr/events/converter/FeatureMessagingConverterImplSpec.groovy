package io.featurehub.mr.events.converter

import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.messaging.model.MessagingRolloutStrategy
import io.featurehub.messaging.model.MessagingRolloutStrategyAttribute
import io.featurehub.messaging.model.StrategyUpdateType
import io.featurehub.mr.events.common.FeatureSetup
import io.featurehub.mr.events.common.converter.FeatureMessagingParameter
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import spock.lang.Shared
import spock.lang.Specification

import java.time.ZoneOffset

class FeatureMessagingConverterImplSpec extends Specification {
    FeatureMessagingConverterImpl featureMessagingConverter
    @Shared FeatureSetup featureSetup

    def setupSpec(){
      featureSetup = new FeatureSetup()
    }

    def setup() {
      featureMessagingConverter = new FeatureMessagingConverterImpl()
    }

    def createRolloutStrategy(String id = null) {
      return new RolloutStrategy()
        .id(id ?: "id123")
        .addAttributesItem(
          new RolloutStrategyAttribute()
          .id("some-id")
          .conditional(RolloutStrategyAttributeConditional.ENDS_WITH)
          .type(RolloutStrategyFieldType.BOOLEAN)
          .fieldName("someFieldName")
          .values(["values"]))
        .name("strategyName")
        .addPercentageAttributesItem("attributeItem")
        .value("strategyValue")
        .percentage(50)
    }

    def toMessagingRolloutStrategy(RolloutStrategy rolloutStrategy) {
      return new MessagingRolloutStrategy()
      .id(rolloutStrategy.id)
      .percentage(rolloutStrategy.percentage)
      .percentageAttributes(rolloutStrategy.percentageAttributes)
      .value(rolloutStrategy.value)
      .attributes([new MessagingRolloutStrategyAttribute()
        .fieldName(rolloutStrategy.attributes[0].fieldName)
        .conditional(rolloutStrategy.attributes[0].conditional)
        .type(rolloutStrategy.attributes[0].type)
        .values(rolloutStrategy.attributes[0].values)])
    }

    def "should convert data to FeatureMessagingUpdate"() {
      given: "i have the db feature value"
        def oldFeatureValue = "old"
        def dbFeatureValue =  featureSetup.createFeature()
      and: "i have the feature messaging parameter"
        def lockUpdate = new SingleFeatureValueUpdate<Boolean>(
          true, true, false)

        def defaultValueUpdate = new SingleFeatureValueUpdate<String>(
          true, dbFeatureValue.defaultValue, oldFeatureValue)
        def retiredUpdate = new SingleFeatureValueUpdate<Boolean>(
          true, false, true)
        def newStrategy = createRolloutStrategy()
        def addStrategyUpdate = new RolloutStrategyUpdate(
          StrategyUpdateType.ADDED.name(), null, newStrategy)
        def strategyUpdates = new MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>(true, [addStrategyUpdate], [], [])
        def featureMessagingParameter = new FeatureMessagingParameter(dbFeatureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates)

      when:
        def featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(
          featureMessagingParameter
        )
      then:
        with(featureMessagingUpdate) {
          whoUpdated == dbFeatureValue.whoUpdated.name
          featureKey == dbFeatureValue.feature.key
          applicationId == dbFeatureValue.environment.parentApplication.id
          environmentId == dbFeatureValue.environment.id
          portfolioId == dbFeatureValue.environment.parentApplication.portfolio.id
          organizationId == dbFeatureValue.environment.parentApplication.portfolio.organization.id
          featureValueUpdated.valueType == FeatureValueType.STRING
          featureValueUpdated.updated == dbFeatureValue.defaultValue
          featureValueUpdated.previous == oldFeatureValue
          whenUpdated == dbFeatureValue.whenUpdated.atOffset(ZoneOffset.UTC)
          lockUpdated.updated
          !lockUpdated.previous
          !retiredUpdated.updated
          retiredUpdated.previous
          strategiesUpdated[0].newStrategy == toMessagingRolloutStrategy(newStrategy)
          strategiesUpdated[0].updateType == StrategyUpdateType.ADDED
        }
    }

  def "should convert strategy reorder data to FeatureMessagingUpdate"() {
    given: "i have the db feature value"
      def dbFeatureValue = featureSetup.createFeature()
    and: "i have the rollout startegies"
      def one = createRolloutStrategy("1")
      def two = createRolloutStrategy("2")
    and: "i have the feature messaging parameter"
      def reordered = [two, one]
      def previous = [one, two]
      def strategyUpdates = new MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>(true, [], reordered, previous)
      def featureMessagingParameter = new FeatureMessagingParameter(dbFeatureValue, null, null, null, strategyUpdates)
    when:
      def featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(featureMessagingParameter)
    then:
    with(featureMessagingUpdate) {
      whoUpdated == dbFeatureValue.whoUpdated.name
      featureKey == dbFeatureValue.feature.key
      featureKey == dbFeatureValue.feature.key
      applicationId == dbFeatureValue.environment.parentApplication.id
      environmentId == dbFeatureValue.environment.id
      portfolioId == dbFeatureValue.environment.parentApplication.portfolio.id
      organizationId == dbFeatureValue.environment.parentApplication.portfolio.organization.id
      featureValueUpdated == null
      whenUpdated == dbFeatureValue.whenUpdated.atOffset(ZoneOffset.UTC)
      lockUpdated == null
      retiredUpdated == null
    }

    def messagingRolloutStrategyOne = toMessagingRolloutStrategy(one)
    def messagingRolloutStrategyTwo = toMessagingRolloutStrategy(two)
    def expectedReordered = [messagingRolloutStrategyTwo, messagingRolloutStrategyOne]
    def expectedPrevious = [ messagingRolloutStrategyOne, messagingRolloutStrategyTwo]
    featureMessagingUpdate.strategiesReordered.reordered == expectedReordered
    featureMessagingUpdate.strategiesReordered.previous == expectedPrevious

  }
}
