package io.featurehub.db.services

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.CreateApplication
import io.featurehub.mr.model.CreateEnvironment
import io.featurehub.mr.model.CreateFeature
import io.featurehub.mr.model.CreatePortfolio
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType

class FeatureUpdatesPublishingSpec extends Base2Spec {
  PortfolioSqlApi portfolioSqlApi
  Portfolio p1
  ApplicationSqlApi applicationSqlApi
  Application app
  CacheSource cacheSource
  FeatureSqlApi featureSqlApi
  EnvironmentSqlApi environmentSqlApi
  Environment env
  RolloutStrategyValidator rsValidator
  PersonFeaturePermission perms
  RolloutStrategyValidator rsv
  FeatureMessagingPublisher featureMessagingCloudEventPublisher
  UUID envIdApp1
  UUID appId

  def setup() {
    perms = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK] as Set<RoleType>)
    cacheSource = Mock()
    rsValidator = Mock()
    rsValidator.validateStrategies(_, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    rsValidator.validateStrategies(_, _, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    ThreadLocalConfigurationSource.createContext(
      ['auditing.enable': 'true','messaging.publisher.thread-pool': "1",'messaging.publish.enabled': 'true'])

    rsv = Mock()
    rsv.validateStrategies(_, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    featureMessagingCloudEventPublisher = Mock()

    featureSqlApi = new FeatureSqlApi( convertUtils, cacheSource, rsv, featureMessagingCloudEventPublisher, Mock(CacheSourceFeatureGroupApi))
    portfolioSqlApi = new PortfolioSqlApi(db, convertUtils, archiveStrategy)
    environmentSqlApi = new EnvironmentSqlApi(db, convertUtils, cacheSource, archiveStrategy, Mock(WebhookEncryptionService))
    applicationSqlApi = new ApplicationSqlApi(convertUtils, cacheSource, archiveStrategy, new InternalFeatureSqlApi())

    p1 = portfolioSqlApi.getPortfolio("basic")

    if (p1 == null) {
      p1 = portfolioSqlApi.createPortfolio(new CreatePortfolio().name("basic").description("basic"), Opts.empty(), superuser)
    }

    app = applicationSqlApi.getApplication(p1.id, "app1")

    if (app == null) {
      app = applicationSqlApi.createApplication(p1.id, new CreateApplication().name("app1").description("desc1"), superPerson)
    }
    appId = app.id

    db.currentTransaction().commit()

    env = environmentSqlApi.getEnvironment(app.id, "dev")

    if (env == null) {
      env = environmentSqlApi.create(new  CreateEnvironment().name("dev").description("dev"), app.id, superPerson)
    }
    envIdApp1 = env.id

  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "feature updates are published when i create the feature value"() {
    given: "i have a feature"
    String featureKey = "FEATURE_FV1"
    applicationSqlApi.createApplicationFeature(appId, new CreateFeature()
      .name("x").description("x").key(featureKey).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)

    when: "i set the feature value"
    def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, featureKey)

    // it already exists, so we have  to unlock it
    featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, f.valueBoolean(true).locked(false), pers)

    then: "feature update is published"

    1 * featureMessagingCloudEventPublisher.publish({ FeatureMessagingParameter param ->
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
    def features = applicationSqlApi.createApplicationFeature(appId,
      new CreateFeature().name("x").description("x").key(featureKey).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)

    when: "i unlock the feature"
    def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, featureKey)
    // it already exists, so we have  to unlock it
    f = featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, f.locked(false), pers)

    then: "feature update is published"
    1 * featureMessagingCloudEventPublisher.publish({ FeatureMessagingParameter param ->
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
    1 * featureMessagingCloudEventPublisher.publish({ FeatureMessagingParameter param ->
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
    1 * featureMessagingCloudEventPublisher.publish({ FeatureMessagingParameter param ->
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
      applicationSqlApi.createApplicationFeature(appId,
        new CreateFeature().name("x").description("x").key(featureKey).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
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
      def rolloutStrategyUpdate = new RolloutStrategyUpdate("added", null, rolloutStrategy)
    when: "i unlock the feature"
      def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, featureKey)
      // it already exists, so we have  to unlock it
      f = featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, f.locked(false), pers)

    then: "feature update is published"
    1 * featureMessagingCloudEventPublisher.publish({ FeatureMessagingParameter param ->
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
    1 * featureMessagingCloudEventPublisher.publish({ FeatureMessagingParameter param ->
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
