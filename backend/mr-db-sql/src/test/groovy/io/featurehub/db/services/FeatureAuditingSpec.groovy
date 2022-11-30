package io.featurehub.db.services

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.mr.model.RoleType
import io.featurehub.db.services.strategies.StrategyDiffer
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Portfolio

class FeatureAuditingSpec extends Base2Spec {
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
  StrategyDiffer strategyDiffer

  static UUID portfolioId = UUID.fromString('16364b24-b4ef-4052-9c33-5eb66b0d1baf')
//  static UUID

  def setup() {
    cacheSource = Mock()
    rsValidator = Mock()
    strategyDiffer = Mock()
    rsValidator.validateStrategies(_, _) >> new RolloutStrategyValidator.ValidationFailure()
    rsValidator.validateStrategies(_, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    ThreadLocalConfigurationSource.createContext(['auditing.enable': 'true'])
    featureSqlApi = new FeatureSqlApi(db, convertUtils, cacheSource, rsValidator, strategyDiffer)
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

    environmentSqlApi = new EnvironmentSqlApi(db, convertUtils, cacheSource, archiveStrategy)
    env = environmentSqlApi.getEnvironment(app.id, "dev")

    if (env == null) {
      env = environmentSqlApi.create(new  Environment().name("dev").description("dev"), app, superPerson)
    }

    db.currentTransaction().commit()
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "when i have a string feature and i add alternating strategies at the same history point they work"() {

  }

  def "when i update a boolean feature value and then update it again with the historical version, nothing changes"() {
    given: "i create a feature"
      def feature = applicationSqlApi.createApplicationFeature(app.id,
        new Feature().name("bool-feature").description("bool-feature").key("FBOOL").valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
        .find { it.key == 'FBOOL' }
    and: "i get the feature value"
      def fv = featureSqlApi.getFeatureValueForEnvironment(env.id, feature.key)
      def fv2 = featureSqlApi.getFeatureValueForEnvironment(env.id, feature.key)
      def perms = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK] as Set<RoleType>)
    when: "i update the  feature"
      fv.valueBoolean( false).locked(false)
      def firstUpdate = featureSqlApi.updateFeatureValueForEnvironment(env.id, feature.key, fv, perms)
    and: "i update the historical record but don't change anything, which should have it detect there are no changed and ignore it"
      featureSqlApi.updateFeatureValueForEnvironment(env.id, feature.key, fv2, perms)
    then:
      !featureSqlApi.getFeatureValueForEnvironment(env.id, feature.key).valueBoolean
      featureSqlApi.getFeatureValueForEnvironment(env.id, feature.key).version == firstUpdate.version
  }
}
