package io.featurehub.mr.events.converter

import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.messaging.model.MessagingRolloutStrategy
import io.featurehub.messaging.model.MessagingRolloutStrategyAttribute
import io.featurehub.messaging.model.StrategyUpdateType
import io.featurehub.mr.events.common.converter.FeatureMessagingConverter
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneOffset

class FeatureMessagingConverterImplSpec extends Specification {
    FeatureMessagingConverterImpl featureMessagingConverter

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
      given:
      def applicationId = UUID.randomUUID()
      def environmentId = UUID.randomUUID()
      def dbFeatureId = UUID.randomUUID()
      def orgId = UUID.randomUUID()
      def portfolioId = UUID.randomUUID()
      def org = new DbOrganization.Builder().name("OrgA").build()
      org.setId(orgId)
      def portfolio = new DbPortfolio.Builder()
        .name("portfolio")
        .organization(org).build()
      portfolio.setId(portfolioId)
      def application = new DbApplication.Builder()
        .portfolio(portfolio)
        .build()
      application.setId(applicationId)
      def environment = new DbEnvironment.Builder()
        .parentApplication(application)
        .build()
      environment.setId(environmentId)

      def person = new DbPerson.Builder().name("Alfie").build()

      def featureKey = "featureKey"
      def featureName = "slackUpdate"
      def oldFeatureValue = "old"
      def newFeatureValue = "new"
      def whenUpdated = LocalDateTime.now()
      def feature = new DbApplicationFeature.Builder()
        .name(featureName)
        .key(featureKey)
        .valueType(FeatureValueType.STRING)
        .build()
      def dbFeatureValue =  Mock(DbFeatureValue)
      dbFeatureValue.environment >> environment
      dbFeatureValue.defaultValue >> newFeatureValue
      dbFeatureValue.feature >> feature
      dbFeatureValue.whoUpdated >> person
      dbFeatureValue.whenUpdated >> whenUpdated
      dbFeatureValue.id >> dbFeatureId

      def lockUpdate = new SingleFeatureValueUpdate<Boolean>(
        true, true, false)

      def defaultValueUpdate = new SingleFeatureValueUpdate<String>(
        true, newFeatureValue, oldFeatureValue)
      def retiredUpdate = new SingleFeatureValueUpdate<Boolean>(
        true, false, true)
      def newStrategy = createRolloutStrategy()
      def addStrategyUpdate = new RolloutStrategyUpdate(
        StrategyUpdateType.ADDED.name(), null, newStrategy)
      def strategyUpdates = new MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>(true, [addStrategyUpdate], [], [])

      when:
      def featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(
        dbFeatureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates
      )
      then:
      featureMessagingUpdate.whoUpdated == dbFeatureValue.whoUpdated.name
      featureMessagingUpdate.featureKey == featureKey
      featureMessagingUpdate.applicationId == applicationId
      featureMessagingUpdate.environmentId == environmentId
      featureMessagingUpdate.portfolioId == portfolioId
      featureMessagingUpdate.organizationId == orgId
      featureMessagingUpdate.featureValueUpdated.valueType == FeatureValueType.STRING
      featureMessagingUpdate.featureValueUpdated.updated == newFeatureValue
      featureMessagingUpdate.featureValueUpdated.previous == oldFeatureValue
      featureMessagingUpdate.whenUpdated == whenUpdated.atOffset(ZoneOffset.UTC)
      featureMessagingUpdate.lockUpdated.updated
      !featureMessagingUpdate.lockUpdated.previous
      !featureMessagingUpdate.retiredUpdated.updated
      featureMessagingUpdate.retiredUpdated.previous
      featureMessagingUpdate.strategiesUpdated[0].newStrategy == toMessagingRolloutStrategy(newStrategy)
      featureMessagingUpdate.strategiesUpdated[0].updateType == StrategyUpdateType.ADDED

    }

  def "should convert strategy reorder data to FeatureMessagingUpdate"() {
    given:
    def applicationId = UUID.randomUUID()
    def environmentId = UUID.randomUUID()
    def dbFeatureId = UUID.randomUUID()
    def orgId = UUID.randomUUID()
    def portfolioId = UUID.randomUUID()
    def org = new DbOrganization.Builder().name("OrgA").build()
    org.setId(orgId)
    def portfolio = new DbPortfolio.Builder()
      .name("portfolio")
      .organization(org).build()
    portfolio.setId(portfolioId)
    def application = new DbApplication.Builder()
      .portfolio(portfolio)
      .build()
    application.setId(applicationId)
    def environment = new DbEnvironment.Builder()
      .parentApplication(application)
      .build()
    environment.setId(environmentId)

    def person = new DbPerson.Builder().name("Alfie").build()

    def featureKey = "featureKey"
    def featureName = "slackUpdate"
    def defaultValue = "same"
    def whenUpdated = LocalDateTime.now()
    def feature = new DbApplicationFeature.Builder()
      .name(featureName)
      .key(featureKey)
      .valueType(FeatureValueType.STRING)
      .build()
    def dbFeatureValue =  Mock(DbFeatureValue)
    dbFeatureValue.environment >> environment
    dbFeatureValue.defaultValue >> defaultValue
    dbFeatureValue.feature >> feature
    dbFeatureValue.whoUpdated >> person
    dbFeatureValue.whenUpdated >> whenUpdated
    dbFeatureValue.id >> dbFeatureId

    def one = createRolloutStrategy("1")
    def two = createRolloutStrategy("2")
    def reordered = [two, one]
    def previous = [one, two]
    def strategyUpdates = new MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>(true, [], reordered, previous)

    when:
    def featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(
      dbFeatureValue, null, null, null, strategyUpdates
    )
    then:
    featureMessagingUpdate.whoUpdated == dbFeatureValue.whoUpdated.name
    featureMessagingUpdate.featureKey == featureKey
    featureMessagingUpdate.applicationId == applicationId
    featureMessagingUpdate.environmentId == environmentId
    featureMessagingUpdate.portfolioId == portfolioId
    featureMessagingUpdate.organizationId == orgId

    featureMessagingUpdate.featureValueUpdated == null
    featureMessagingUpdate.whenUpdated == whenUpdated.atOffset(ZoneOffset.UTC)
    featureMessagingUpdate.lockUpdated == null
    featureMessagingUpdate.retiredUpdated == null

    def messagingRolloutStrategyOne = toMessagingRolloutStrategy(one)
    def messagingRolloutStrategyTwo = toMessagingRolloutStrategy(two)
    def expectedReordered = [messagingRolloutStrategyTwo, messagingRolloutStrategyOne]
    def expectedPrevious = [ messagingRolloutStrategyOne, messagingRolloutStrategyTwo]
    featureMessagingUpdate.strategiesReordered.reordered == expectedReordered
    featureMessagingUpdate.strategiesReordered.previous == expectedPrevious

  }
}
