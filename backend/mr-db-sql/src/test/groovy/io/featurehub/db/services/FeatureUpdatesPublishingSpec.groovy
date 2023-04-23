package io.featurehub.db.services

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.common.FeatureMessagingCloudEventPublisher
import io.featurehub.mr.events.common.converter.FeatureMessagingParameter
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.utils.ExecutorSupplier
import java.util.concurrent.ExecutorService

class FeatureUpdatesPublishingSpec extends Base2Spec {
  PortfolioSqlApi portfolioSqlApi
  Portfolio p1
  ApplicationSqlApi applicationSqlApi
  Application app
  CacheSource cacheSource
//  Feature feature
  FeatureSqlApi featureSqlApi
  EnvironmentSqlApi environmentSqlApi
  Environment env
  RolloutStrategyValidator rsValidator
  PersonFeaturePermission perms
  RolloutStrategyValidator rsv
  FeatureMessagingCloudEventPublisher featureMessagingCloudEventPublisher
  UUID envIdApp1
  UUID appId
  ExecutorSupplier executorSupplier
  ExecutorService executor

  def setup() {
    perms = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK] as Set<RoleType>)
    cacheSource = Mock()
    rsValidator = Mock()
    rsValidator.validateStrategies(_, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    rsValidator.validateStrategies(_, _, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    ThreadLocalConfigurationSource.createContext(['auditing.enable': 'true', 'messaging.publisher.thread-pool': "1"])

    rsv = Mock()
    rsv.validateStrategies(_, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    featureMessagingCloudEventPublisher = Mock()
    executorSupplier = Mock()
    executor = Mock()
    1 * executorSupplier.executorService(_) >> executor
    featureSqlApi = new FeatureSqlApi(db, convertUtils, cacheSource, rsv, featureMessagingCloudEventPublisher, executorSupplier)
    portfolioSqlApi = new PortfolioSqlApi(db, convertUtils, archiveStrategy)
    environmentSqlApi = new EnvironmentSqlApi(db, convertUtils, cacheSource, archiveStrategy)
    applicationSqlApi = new ApplicationSqlApi(db, convertUtils, cacheSource, archiveStrategy, featureSqlApi)

    p1 = portfolioSqlApi.getPortfolio("basic")

    if (p1 == null) {
      p1 = portfolioSqlApi.createPortfolio(new Portfolio().name("basic").description("basic"), Opts.empty(), superPerson)
    }

    app = applicationSqlApi.getApplication(p1.id, "app1")

    if (app == null) {
      app = applicationSqlApi.createApplication(p1.id, new Application().name("app1").description("desc1"), superPerson)
    }
    appId = app.id

    db.currentTransaction().commit()

    env = environmentSqlApi.getEnvironment(app.id, "dev")

    if (env == null) {
      env = environmentSqlApi.create(new  Environment().name("dev").description("dev"), app, superPerson)
    }
    envIdApp1 = env.id

  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "feature updates are published when i create the feature value"() {
    given: "i have a feature"
    String featureKey = "FEATURE_FV1"
    applicationSqlApi.createApplicationFeature(appId, new Feature().name("x").key(featureKey).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)

    when: "i set the feature value"
    def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, featureKey)

    // it already exists, so we have  to unlock it
    featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, f.valueBoolean(true).locked(false), pers)

    then: "feature update is published"
    1 * executor.submit(_) >> { Runnable task -> task.run() }
    1 * featureMessagingCloudEventPublisher.publishFeatureMessagingUpdate({ FeatureMessagingParameter param ->
      with(param) {
        !lockUpdate.updated
        lockUpdate.previous
        lockUpdate.hasChanged
        defaultValueUpdate.previous == "false"
        defaultValueUpdate.updated == "true"
        defaultValueUpdate.hasChanged
        !retiredUpdate.hasChanged
        !strategyUpdates.hasChanged
      }
    })
    1 * cacheSource.publishFeatureChange(_)

  }


  def "feature updates are published when i update the feature value"() {
    given: "i have a feature"
    String featureKey = "FEATURE_FV2"
    def features = applicationSqlApi.createApplicationFeature(appId, new Feature().name("x").key(featureKey).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)

    when: "i unlock the feature"
    def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, featureKey)
    // it already exists, so we have  to unlock it
    f = featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, f.locked(false), pers)

    then: "feature update is published"
    1 * executor.submit(_) >> { Runnable task -> task.run() }
    1 * featureMessagingCloudEventPublisher.publishFeatureMessagingUpdate({ FeatureMessagingParameter param ->
      with(param) {
        !lockUpdate.updated
        lockUpdate.previous
        lockUpdate.hasChanged
        defaultValueUpdate.previous == null
        defaultValueUpdate.updated == null
        !defaultValueUpdate.hasChanged
        !retiredUpdate.hasChanged
        !strategyUpdates.hasChanged
      }
    })

    when: "i set the feature value "
    featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, f.valueBoolean(true).locked(true), pers)

    then: "feature update is published"
    1 * executor.submit(_) >> { Runnable task -> task.run() }
    1 * featureMessagingCloudEventPublisher.publishFeatureMessagingUpdate({ FeatureMessagingParameter param ->
      with(param) {
        lockUpdate.updated
        !lockUpdate.previous
        lockUpdate.hasChanged
        defaultValueUpdate.previous == "false"
        defaultValueUpdate.updated
        defaultValueUpdate.hasChanged
        !retiredUpdate.hasChanged
        !strategyUpdates.hasChanged
      }
    })
    when: "i update the feature value"
    def fv = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, featureKey)
    fv.valueBoolean(false)
    fv.locked(false)
    featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, fv, pers)

    then: "feature update is published"
    1 * executor.submit(_) >> { Runnable task -> task.run() }
    1 * featureMessagingCloudEventPublisher.publishFeatureMessagingUpdate({ FeatureMessagingParameter param ->
      with(param) {
        !lockUpdate.updated
        lockUpdate.previous
        lockUpdate.hasChanged
        defaultValueUpdate.previous
        defaultValueUpdate.updated == "false"
        defaultValueUpdate.hasChanged
        !retiredUpdate.hasChanged
        !strategyUpdates.hasChanged
      }
    })
  }

  def "feature updates are published when i add rollout strategies"() {
    given: "i have a feature"
    String featureKey = "FEATURE_FV3"
    applicationSqlApi.createApplicationFeature(appId, new Feature().name("x").key(featureKey).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)

    and: "i have a rollout strategy"
    def rolloutStrategy = new RolloutStrategy().name('freddy').percentage(20).percentageAttributes(['company'])
      .value(Boolean.FALSE).attributes([
      new RolloutStrategyAttribute()
        .values(['ios'])
        .fieldName('platform')
        .conditional(RolloutStrategyAttributeConditional.EQUALS)
        .type(RolloutStrategyFieldType.STRING)
    ])
    def rolloutStrategyUpdate = new RolloutStrategyUpdate("add", null, rolloutStrategy)
    when: "i unlock the feature"
    def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, featureKey)
    // it already exists, so we have  to unlock it
    f = featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, f.locked(false), pers)

    then: "feature update is published"
    1 * executor.submit(_) >> { Runnable task -> task.run() }
    1 * featureMessagingCloudEventPublisher.publishFeatureMessagingUpdate({ FeatureMessagingParameter param ->
      with(param) {
        !lockUpdate.updated
        lockUpdate.previous
        lockUpdate.hasChanged
        defaultValueUpdate.previous == null
        defaultValueUpdate.updated == null
        !defaultValueUpdate.hasChanged
        !retiredUpdate.hasChanged
        !strategyUpdates.hasChanged
      }
    })
    when: "i add new rollout strategy"
    def fv = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, featureKey)

    fv.locked(false)
    fv.rolloutStrategies([rolloutStrategy])
    featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, fv, perms)

    then: "feature update is published"
    1 * executor.submit(_) >> { Runnable task -> task.run() }
    1 * featureMessagingCloudEventPublisher.publishFeatureMessagingUpdate({ FeatureMessagingParameter param ->
      with(param) {
        !lockUpdate.updated
        !lockUpdate.previous
        !lockUpdate.hasChanged
        defaultValueUpdate.previous == null
        defaultValueUpdate.updated == null
        !defaultValueUpdate.hasChanged
        !retiredUpdate.hasChanged
        strategyUpdates.hasChanged
      }
      param.strategyUpdates.updated[0]  == rolloutStrategyUpdate
    })
  }
}
