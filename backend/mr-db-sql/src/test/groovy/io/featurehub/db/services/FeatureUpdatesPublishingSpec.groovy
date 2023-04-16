package io.featurehub.db.services

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
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
  ApplicationSqlApi appApi
  UUID envIdApp1
  UUID appId

  def setup() {
    perms = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK] as Set<RoleType>)
    cacheSource = Mock()
    rsValidator = Mock()
    rsValidator.validateStrategies(_, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    rsValidator.validateStrategies(_, _, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    ThreadLocalConfigurationSource.createContext(['auditing.enable': 'true'])

    rsv = Mock(RolloutStrategyValidator)
    rsv.validateStrategies(_, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    featureMessagingCloudEventPublisher = Mock()
    featureSqlApi = new FeatureSqlApi(db, convertUtils, Mock(CacheSource), rsv, featureMessagingCloudEventPublisher)
    portfolioSqlApi = new PortfolioSqlApi(db, convertUtils, archiveStrategy)

    p1 = portfolioSqlApi.getPortfolio("basic")

    if (p1 == null) {
      p1 = portfolioSqlApi.createPortfolio(new Portfolio().name("basic").description("basic"), Opts.empty(), superPerson)
    }

    applicationSqlApi = new ApplicationSqlApi(db, convertUtils, cacheSource, archiveStrategy, featureSqlApi)
    app = applicationSqlApi.getApplication(p1.id, "app1")

    if (app == null) {
      app = applicationSqlApi.createApplication(p1.id, new Application().name("app1").description("desc1"), superPerson)
    }
    appId = app.id

    db.currentTransaction().commit()

    environmentSqlApi = new EnvironmentSqlApi(db, convertUtils, cacheSource, archiveStrategy)
    env = environmentSqlApi.getEnvironment(app.id, "dev")

    if (env == null) {
      env = environmentSqlApi.create(new  Environment().name("dev").description("dev"), app, superPerson)
    }
    envIdApp1 = env.id
    appApi = new ApplicationSqlApi(db, convertUtils, Mock(CacheSource), archiveStrategy, featureSqlApi)


  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "feature updates are published when i update the feature value"() {

    given: "i have a feature"
    String featureKey = "FEATURE_FV1"
    def features = appApi.createApplicationFeature(appId, new Feature().name("x").key(featureKey).valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
    def pers = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK, RoleType.LOCK] as Set<RoleType>)

    when: "i set the feature lock"
    def f = featureSqlApi.getFeatureValueForEnvironment(envIdApp1, featureKey)
    // it already exists, so we have  to unlock it
    f = featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, f.locked(false), pers)

    then: "feature update is published"
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
    def f2 = featureSqlApi.updateFeatureValueForEnvironment(envIdApp1, featureKey, f.valueBoolean(true).locked(true), pers)

    then: "feature update is published"
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
}
