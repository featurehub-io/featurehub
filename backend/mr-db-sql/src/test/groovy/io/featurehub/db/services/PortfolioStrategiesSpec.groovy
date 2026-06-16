package io.featurehub.db.services

import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbPortfolioStrategyForFeatureValue
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.db.model.query.QDbPortfolioRolloutStrategy
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.CreateFeature
import io.featurehub.mr.model.CreatePortfolioRolloutStrategy
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.PortfolioRolloutStrategy
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.UpdatePortfolioRolloutStrategy
import spock.lang.Unroll

class PortfolioStrategiesSpec extends Base3Spec {
  PortfolioRolloutStrategySqlApi portfolioRolloutStrategySqlApi

  def setup() {
    cacheSource = Mock(CacheSource)
    archiveStrategy = Mock(ArchiveStrategy)
    internalFeatureApi = Mock(InternalFeatureApi)
    portfolioRolloutStrategySqlApi = new PortfolioRolloutStrategySqlApi(convertUtils, internalFeatureApi)
    UpdateFeatureApi updateFeatureApi = new UpdateFeatureApiImpl(convertUtils, cacheSource, featureMessagingCloudEventPublisher)
    featureSqlApi = new FeatureSqlApi(convertUtils, rsValidator, Mock(CacheSourceFeatureGroupApi), updateFeatureApi)
  }

  String createFeature(FeatureValueType valueType = FeatureValueType.BOOLEAN) {
    def cFeature = new CreateFeature().name(ranName()).key(ranName()).valueType(valueType).description(ranName())
    applicationSqlApi.createApplicationFeature(app1.id, cFeature, superPerson, Opts.empty())
    return cFeature.key
  }

  PersonFeaturePermission allPermissions() {
    return new PersonFeaturePermission.Builder().person(superPerson)
      .roles(io.featurehub.mr.model.RoleType.values().toList().toSet())
      .appRoles(ApplicationRoleType.values().toList().toSet())
      .build()
  }

  PortfolioRolloutStrategy createStrategy() {
    return portfolioRolloutStrategySqlApi.createStrategy(portfolio.id,
      new CreatePortfolioRolloutStrategy().name(ranName()), superPerson.id.id, Opts.empty()
    )
  }

  void associateStrategyWithFeatureValue(String key, PortfolioRolloutStrategy strategy) {
    def fv = featureSqlApi.getFeatureValueForEnvironment(env1.id, key)
    if (fv == null) {
      fv = new FeatureValue().key(key).locked(false)
    }
    featureSqlApi.updateFeatureValueForEnvironment(env1.id, key, fv, allPermissions())

    def dbFV = new QDbFeatureValue().environment.id.eq(env1.id).feature.key.eq(key).findOne()
    def dbRS = new QDbPortfolioRolloutStrategy().id.eq(strategy.id).findOne()

    def link = new DbPortfolioStrategyForFeatureValue.Builder()
      .featureValue(dbFV)
      .rolloutStrategy(dbRS)
      .enabled(true)
      .build()
    dbRS.addSharedRolloutStrategy(link)
    dbRS.save()
  }

  @Unroll
  def "full lifecycle of portfolio strategies"() {
    given: "i have a feature for the application"
      String key = createFeature(featureType)
    and: "i have a portfolio strategy"
      def strategy = createStrategy()
      def updateName = ranName()
    when: "i associate the strategy with the feature value in the default environment"
      associateStrategyWithFeatureValue(key, strategy)
    then:
      1 * cacheSource.publishFeatureChange(_)
      0 * _
    when: "i update the portfolio strategy"
      portfolioRolloutStrategySqlApi.updateStrategy(portfolio.id, strategy.id,
        new UpdatePortfolioRolloutStrategy().percentage(20).name(updateName), superPerson.id.id, Opts.empty())
    then:
      1 * internalFeatureApi.updatedPortfolioStrategy({ DbPortfolioStrategyForFeatureValue pfvStrategy ->
        pfvStrategy.rolloutStrategy.strategy.name == updateName
        pfvStrategy.rolloutStrategy.strategy.percentage == 20
      }, { RolloutStrategy original ->
        original != null
        original.percentage == null
        original.name == strategy.name
      }, _, _)
    where:
      featureType                | _
      FeatureValueType.BOOLEAN   | _
      FeatureValueType.NUMBER    | _
      FeatureValueType.STRING    | _
      FeatureValueType.JSON      | _
  }

  def "when we update or detach the feature strategy it passes the existing rollout strategies so the auditing can be notified"() {
    given: "we create two features"
      def keys = [createFeature(), createFeature()]
    and: "we create one portfolio strategy"
      def strategy = createStrategy()
    and: "we associate the strategy with both features"
      keys.each { associateStrategyWithFeatureValue(it, strategy) }
    and: "we reset the internalFeatureApi mock because we want to track what strategies are sent on change"
      internalFeatureApi = Mock(InternalFeatureApi)
      portfolioRolloutStrategySqlApi = new PortfolioRolloutStrategySqlApi(convertUtils, internalFeatureApi)
    and: "we get the results of the strategy collection from internalFeatureApi"
      def ifApi = new InternalFeatureSqlApi(Mock(Conversions), Mock(CacheSource), Mock(FeatureMessagingPublisher))
      def dbFV1 = new QDbFeatureValue().environment.id.eq(env1.id).feature.key.eq(keys[0]).findOne()
      def dbFV2 = new QDbFeatureValue().environment.id.eq(env1.id).feature.key.eq(keys[1]).findOne()
      def fv1Strategies = ifApi.collectFeatureValueStrategies(dbFV1)
      def fv2Strategies = ifApi.collectFeatureValueStrategies(dbFV2)
    when: "we update the strategy percentage"
      portfolioRolloutStrategySqlApi.updateStrategy(portfolio.id, strategy.id,
        new UpdatePortfolioRolloutStrategy().percentage(1000), superuser, Opts.empty())
    then:
      1 * internalFeatureApi.collectFeatureValueStrategies(dbFV1) >> fv1Strategies
      1 * internalFeatureApi.collectFeatureValueStrategies(dbFV2) >> fv2Strategies
      1 * internalFeatureApi.updatedPortfolioStrategy(_, _, _, { List<RolloutStrategy> existingStrategies ->
        existingStrategies.find({ it.name == strategy.name })
      })
      1 * internalFeatureApi.updatedPortfolioStrategy(_, _, _, { List<RolloutStrategy> existingStrategies ->
        existingStrategies.find({ it.name == strategy.name })
      })
  }

  def "when we update or delete the feature strategy it will update the feature values"() {
    given: "we create two features"
      def keys = [createFeature(), createFeature()]
    and: "we create one strategy"
      def strategy = createStrategy()
    and: "we associate the strategy with both features"
      keys.each { associateStrategyWithFeatureValue(it, strategy) }
    and: "we reset the mock"
      internalFeatureApi = Mock(InternalFeatureApi)
      portfolioRolloutStrategySqlApi = new PortfolioRolloutStrategySqlApi(convertUtils, internalFeatureApi)
    when: "we update the strategy name"
      portfolioRolloutStrategySqlApi.updateStrategy(portfolio.id, strategy.id, new UpdatePortfolioRolloutStrategy().name(ranName()), superuser, Opts.empty())
    then: "there is no interaction with the features as nothing changed that was important"
      0 * internalFeatureApi.updatedPortfolioStrategy(_, _, _, _)
    when: "we update the strategy percentage"
      portfolioRolloutStrategySqlApi.updateStrategy(portfolio.id, strategy.id,
        new UpdatePortfolioRolloutStrategy().percentage(1000), superuser, Opts.empty())
    then:
      2 * internalFeatureApi.updatedPortfolioStrategy({ DbPortfolioStrategyForFeatureValue pfvStrategy ->
        keys.contains(pfvStrategy.featureValue.feature.key)
      }, { RolloutStrategy rs -> rs.percentage == null }, _, _)
    when:
      portfolioRolloutStrategySqlApi.archiveStrategy(portfolio.id, strategy.id, superuser)
    then:
      1 * internalFeatureApi.detachPortfolioStrategy({ DbPortfolioStrategyForFeatureValue pfv ->
        pfv.featureValue.feature.key == keys.first()
      }, { RolloutStrategy rs -> rs.percentage == 1000 }, _, _)
    then:
      1 * internalFeatureApi.detachPortfolioStrategy({ DbPortfolioStrategyForFeatureValue pfv ->
        pfv.featureValue.feature.key == keys.last()
      }, _, _, _)
  }

  def "when we use the real internal feature service and update and delete strategies"() {
    given: "we create one feature"
      def keys = [createFeature()]
    and: "we create one strategy"
      def strategy = createStrategy()
    and: "we associate the strategy with the feature"
      keys.each { associateStrategyWithFeatureValue(it, strategy) }
    and: "we ensure we are not using a mock internal feature service"
      FeatureMessagingPublisher fmp = Mock()
      internalFeatureApi = new InternalFeatureSqlApi(convertUtils, cacheSource, fmp)
      portfolioRolloutStrategySqlApi = new PortfolioRolloutStrategySqlApi(convertUtils, internalFeatureApi)
    when: "we update the strategy percentage"
      portfolioRolloutStrategySqlApi.updateStrategy(portfolio.id, strategy.id,
        new UpdatePortfolioRolloutStrategy().percentage(1000), superuser, Opts.empty())
    then:
      1 * cacheSource.publishFeatureChange({ DbFeatureValue fv ->
        fv.feature.key == keys.first()
      })
      1 * fmp.publish({ FeatureMessagingParameter p ->
        p.applicationStrategyUpdates.hasChanged
        p.applicationStrategyUpdates.updated[0].new.percentage == 1000
        p.applicationStrategyUpdates.updated[0].old.percentage == null
      }, _)
    when: "we delete the portfolio strategy"
      portfolioRolloutStrategySqlApi.archiveStrategy(portfolio.id, strategy.id, superuser)
    then:
      1 * cacheSource.publishFeatureChange({ DbFeatureValue fv ->
        fv.feature.key == keys.first()
      })
      1 * fmp.publish({ FeatureMessagingParameter p ->
        p.applicationStrategyUpdates.hasChanged
        p.applicationStrategyUpdates.updated[0].new == null
        p.applicationStrategyUpdates.updated[0].old.percentage == 1000
      }, _)
  }
}