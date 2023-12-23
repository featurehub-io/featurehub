package io.featurehub.messaging.converter

import io.featurehub.db.model.DbFeatureValue
import io.featurehub.messaging.MessagingConfig
import io.featurehub.messaging.common.DbFeatureTestProvider
import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.api.SingleNullableFeatureValueUpdate
import io.featurehub.messaging.model.MessagingRolloutStrategy
import io.featurehub.messaging.model.MessagingRolloutStrategyAttribute
import io.featurehub.messaging.model.StrategyUpdateType
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisher
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import spock.lang.Specification

import java.time.ZoneOffset

class FeatureMessagingConverterImplSpec extends Specification {
  FeatureMessagingConverterImpl featureMessagingConverter
  DbFeatureValue dbFeatureValue
  FeatureMessagingCloudEventPublisher publisher
  MessagingConfig config

  def setup() {
    dbFeatureValue = DbFeatureTestProvider.provideFeatureValue()
    publisher = Mock()
    config = Mock()
    featureMessagingConverter = new FeatureMessagingConverterImpl(config, publisher)
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
      .name(rolloutStrategy.name)
      .percentage(rolloutStrategy.percentage)
      .percentageAttributes(rolloutStrategy.percentageAttributes)
      .value(rolloutStrategy.value)
      .attributes([new MessagingRolloutStrategyAttribute()
                     .fieldName(rolloutStrategy.attributes[0].fieldName)
                     .conditional(rolloutStrategy.attributes[0].conditional)
                     .type(rolloutStrategy.attributes[0].type)
                     .values(rolloutStrategy.attributes[0].values)])
  }

  def "should set updated and previous on featureValueUpdated when default value has changed"() {
    given: "i have the db feature value"
    def oldFeatureValue = "old"

    and: "i have the feature messaging parameter"
    def lockUpdate = new SingleFeatureValueUpdate<Boolean>(
      true, true, false)
    def defaultValueUpdate = new SingleNullableFeatureValueUpdate<String>(true, dbFeatureValue.defaultValue, oldFeatureValue)
    def retiredUpdate = new SingleFeatureValueUpdate<Boolean>(
      false, false, false)
    def strategyUpdates = new MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>(false, [], [], [])
    def featureMessagingParameter = new FeatureMessagingParameter(dbFeatureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates)

    when:
    def featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(
      featureMessagingParameter
    )
    then:
    with(featureMessagingUpdate) {
      whoUpdated == dbFeatureValue.whoUpdated.name
      featureKey == dbFeatureValue.feature.key
      featureId == dbFeatureValue.feature.id
      featureValueId == dbFeatureValue.id
      applicationId == dbFeatureValue.environment.parentApplication.id
      environmentId == dbFeatureValue.environment.id
      portfolioId == dbFeatureValue.environment.parentApplication.portfolio.id
      organizationId == dbFeatureValue.environment.parentApplication.portfolio.organization.id
      featureValueType == FeatureValueType.STRING
      featureValueUpdated.updated == dbFeatureValue.defaultValue
      featureValueUpdated.previous == oldFeatureValue
      whenUpdated == dbFeatureValue.whenUpdated.atOffset(ZoneOffset.UTC)
      lockUpdated.updated
      !lockUpdated.previous
      retiredUpdated == null
      strategiesUpdated == null
    }
  }

  def "should set previous and updated on retiredUpdated when retired value has changed"() {
    given: "i have the db feature value"

    and: "i have the feature messaging parameter"
    def lockUpdate = new SingleFeatureValueUpdate<Boolean>(
      false, false, false)
    def defaultValueUpdate = new SingleNullableFeatureValueUpdate<String>(false, dbFeatureValue.defaultValue, dbFeatureValue.defaultValue)
    def retiredUpdate = new SingleFeatureValueUpdate<Boolean>(
      true, true, false)
    def strategyUpdates = new MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>(false, [], [], [])
    def featureMessagingParameter = new FeatureMessagingParameter(dbFeatureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates)

    when:
    def featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(
      featureMessagingParameter
    )
    then:
    with(featureMessagingUpdate) {
      whoUpdated == dbFeatureValue.whoUpdated.name
      featureKey == dbFeatureValue.feature.key
      featureId == dbFeatureValue.feature.id
      featureValueId == dbFeatureValue.id
      applicationId == dbFeatureValue.environment.parentApplication.id
      environmentId == dbFeatureValue.environment.id
      portfolioId == dbFeatureValue.environment.parentApplication.portfolio.id
      organizationId == dbFeatureValue.environment.parentApplication.portfolio.organization.id
      featureValueType == FeatureValueType.STRING
      featureValueUpdated == null
      whenUpdated == dbFeatureValue.whenUpdated.atOffset(ZoneOffset.UTC)
      lockUpdated == null
      retiredUpdated.updated
      !retiredUpdated.previous
      strategiesUpdated == null
    }
  }

  def "should set strategy updates when a new strategy is added"() {
    given: "i have the db feature value"

    def existingStrategy = createRolloutStrategy()
    dbFeatureValue.setRolloutStrategies([existingStrategy])

    and: "i have the feature messaging parameter"
    def lockUpdate = new SingleFeatureValueUpdate<Boolean>(
      false, false, false)
    def defaultValueUpdate = new SingleNullableFeatureValueUpdate<String>(false, dbFeatureValue.defaultValue, dbFeatureValue.defaultValue)
    def retiredUpdate = new SingleFeatureValueUpdate<Boolean>(
      false, false, false)
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
      featureId == dbFeatureValue.feature.id
      featureValueId == dbFeatureValue.id
      applicationId == dbFeatureValue.environment.parentApplication.id
      environmentId == dbFeatureValue.environment.id
      portfolioId == dbFeatureValue.environment.parentApplication.portfolio.id
      organizationId == dbFeatureValue.environment.parentApplication.portfolio.organization.id
      featureValueType == FeatureValueType.STRING
      featureValueUpdated == null
      whenUpdated == dbFeatureValue.whenUpdated.atOffset(ZoneOffset.UTC)
      lockUpdated == null
      retiredUpdated == null
      strategiesUpdated.size() == 1
      strategiesUpdated[0].newStrategy == toMessagingRolloutStrategy(newStrategy)
      strategiesUpdated[0].updateType == StrategyUpdateType.ADDED
      strategiesUpdated[0].oldStrategy == null
      strategiesReordered == null
    }
  }

  def "should set strategy updates when a strategy is changed"() {
    given: "i have the db feature value"

    def existingStrategy = createRolloutStrategy()
    dbFeatureValue.setRolloutStrategies([existingStrategy])

    and: "i have the feature messaging parameter"
    def lockUpdate = new SingleFeatureValueUpdate<Boolean>(
      false, false, false)
    def defaultValueUpdate = new SingleNullableFeatureValueUpdate<String>(false, dbFeatureValue.defaultValue, dbFeatureValue.defaultValue)
    def retiredUpdate = new SingleFeatureValueUpdate<Boolean>(
      false, false, false)
    def changedStrategy = createRolloutStrategy()
    def oldStrategy = createRolloutStrategy()

    def changedStrategyUpdate = new RolloutStrategyUpdate(
      StrategyUpdateType.CHANGED.name(), oldStrategy, changedStrategy)

    def strategyUpdates = new MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>(true, [changedStrategyUpdate], [], [])

    def featureMessagingParameter = new FeatureMessagingParameter(dbFeatureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates)

    when:
    def featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(
      featureMessagingParameter
    )
    then:
    with(featureMessagingUpdate) {
      whoUpdated == dbFeatureValue.whoUpdated.name
      featureKey == dbFeatureValue.feature.key
      featureId == dbFeatureValue.feature.id
      featureValueId == dbFeatureValue.id
      applicationId == dbFeatureValue.environment.parentApplication.id
      environmentId == dbFeatureValue.environment.id
      portfolioId == dbFeatureValue.environment.parentApplication.portfolio.id
      organizationId == dbFeatureValue.environment.parentApplication.portfolio.organization.id
      featureValueType == FeatureValueType.STRING
      featureValueUpdated == null
      whenUpdated == dbFeatureValue.whenUpdated.atOffset(ZoneOffset.UTC)
      lockUpdated == null
      retiredUpdated == null
      strategiesUpdated.size() == 1
      strategiesUpdated[0].newStrategy == toMessagingRolloutStrategy(changedStrategy)
      strategiesUpdated[0].updateType == StrategyUpdateType.CHANGED
      strategiesUpdated[0].oldStrategy == toMessagingRolloutStrategy(oldStrategy)
      strategiesReordered == null
    }
  }

  def "should set strategy updates when strategies are added, updated and deleted"() {
    given: "i have the db feature value"

    and: "i have the feature messaging parameter"
    def lockUpdate = new SingleFeatureValueUpdate<Boolean>(
      false, false, false)
    def defaultValueUpdate = new SingleNullableFeatureValueUpdate<String>(false, dbFeatureValue.defaultValue, dbFeatureValue.defaultValue)
    def retiredUpdate = new SingleFeatureValueUpdate<Boolean>(
      false, false, false)
    def newStrategy = createRolloutStrategy()
    def oldStrategy = createRolloutStrategy()
    def deletedStrategy = createRolloutStrategy()
    def changedStrategy = createRolloutStrategy()
    def addStrategyUpdate = new RolloutStrategyUpdate(
      StrategyUpdateType.ADDED.name(), null, newStrategy)
    def changedStrategyUpdate = new RolloutStrategyUpdate(
      StrategyUpdateType.CHANGED.name(), oldStrategy, changedStrategy)
    def deletedStrategyUpdate = new RolloutStrategyUpdate(
      StrategyUpdateType.DELETED.name(), deletedStrategy, null)
    def strategyUpdates = new MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>(true, [addStrategyUpdate, changedStrategyUpdate, deletedStrategyUpdate], [], [])

    def featureMessagingParameter = new FeatureMessagingParameter(dbFeatureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates)

    when:
    def featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(
      featureMessagingParameter
    )
    then:
    with(featureMessagingUpdate) {
      whoUpdated == dbFeatureValue.whoUpdated.name
      featureKey == dbFeatureValue.feature.key
      featureId == dbFeatureValue.feature.id
      featureValueId == dbFeatureValue.id
      applicationId == dbFeatureValue.environment.parentApplication.id
      environmentId == dbFeatureValue.environment.id
      portfolioId == dbFeatureValue.environment.parentApplication.portfolio.id
      organizationId == dbFeatureValue.environment.parentApplication.portfolio.organization.id
      featureValueType == FeatureValueType.STRING
      featureValueUpdated == null
      whenUpdated == dbFeatureValue.whenUpdated.atOffset(ZoneOffset.UTC)
      lockUpdated == null
      retiredUpdated == null
      strategiesUpdated[0].newStrategy == toMessagingRolloutStrategy(newStrategy)
      strategiesUpdated[0].updateType == StrategyUpdateType.ADDED
      strategiesUpdated[0].oldStrategy == null
      strategiesUpdated[1].newStrategy == toMessagingRolloutStrategy(changedStrategy)
      strategiesUpdated[1].oldStrategy == toMessagingRolloutStrategy(oldStrategy)
      strategiesUpdated[1].updateType == StrategyUpdateType.CHANGED
      strategiesUpdated[2].oldStrategy == toMessagingRolloutStrategy(deletedStrategy)
      strategiesUpdated[2].updateType == StrategyUpdateType.DELETED
      strategiesReordered == null

    }
  }

  def "should set strategyUpdates reordered and previous when strategies are reordered"() {
    given: "i have the db feature value"

    and: "i have the rollout startegies"
    def one = createRolloutStrategy("1")
    def two = createRolloutStrategy("2")
    and: "i have the feature messaging parameter"
    def reordered = [two, one]
    def previous = [one, two]
    def strategyUpdates = new MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>(true, [], reordered, previous)
    def lockUpdate = new SingleFeatureValueUpdate<Boolean>(false, false, false)
    def retiredUpdate = new SingleFeatureValueUpdate<Boolean>(false, false, false)
    def defaultValueUpdate = new SingleNullableFeatureValueUpdate<String>(false, null, null)
    def featureMessagingParameter = new FeatureMessagingParameter(dbFeatureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates)
    when:
    def featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(featureMessagingParameter)
    then:
    with(featureMessagingUpdate) {
      whoUpdated == dbFeatureValue.whoUpdated.name
      featureKey == dbFeatureValue.feature.key
      featureId == dbFeatureValue.feature.id
      featureValueId == dbFeatureValue.id
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
    def expectedPrevious = [messagingRolloutStrategyOne, messagingRolloutStrategyTwo]
    featureMessagingUpdate.strategiesReordered.reordered == expectedReordered
    featureMessagingUpdate.strategiesReordered.previous == expectedPrevious

  }
}
