package io.featurehub.db.services

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.mr.events.common.FeatureMessagingCloudEventPublisher
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.utils.ExecutorSupplier

import java.util.concurrent.ExecutorService

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
  PersonFeaturePermission perms
  FeatureMessagingCloudEventPublisher featureMessagingCloudEventPublisher
  ExecutorSupplier executorSupplier
  ExecutorService executor

  static UUID portfolioId = UUID.fromString('16364b24-b4ef-4052-9c33-5eb66b0d1baf')
//  static UUID

  def setup() {
    perms = new PersonFeaturePermission(superPerson, [RoleType.CHANGE_VALUE, RoleType.UNLOCK] as Set<RoleType>)
    cacheSource = Mock()
    rsValidator = Mock()
    rsValidator.validateStrategies(_, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    rsValidator.validateStrategies(_, _, _, _) >> new RolloutStrategyValidator.ValidationFailure()
    ThreadLocalConfigurationSource.createContext(['auditing.enable': 'true'])
    featureMessagingCloudEventPublisher = Mock()
    executorSupplier = Mock()
    executor = Mock()
    1 * executorSupplier.executorService(_) >> executor
    featureSqlApi = new FeatureSqlApi(db, convertUtils, cacheSource, rsValidator, featureMessagingCloudEventPublisher, executorSupplier)
    portfolioSqlApi = new PortfolioSqlApi(db, convertUtils, archiveStrategy)
    p1 = portfolioSqlApi.getPortfolio("basic")

    if (p1 == null) {
      p1 = portfolioSqlApi.createPortfolio(new Portfolio().name("basic").description("basic"), Opts.empty(), superPerson)
    }

    applicationSqlApi = new ApplicationSqlApi(convertUtils, cacheSource, archiveStrategy, featureSqlApi)
    app = applicationSqlApi.getApplication(p1.id, "app1")

    if (app == null) {
      app = applicationSqlApi.createApplication(p1.id, new Application().name("app1").description("desc1"), superPerson)
    }

    db.currentTransaction().commit()

    environmentSqlApi = new EnvironmentSqlApi(db, convertUtils, cacheSource, archiveStrategy)
    env = environmentSqlApi.getEnvironment(app.id, "dev")

    if (env == null) {
      env = environmentSqlApi.create(new  Environment().name("dev").description("dev"), app, superPerson)
    }
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "when i have a string feature and i add alternating strategies at the same history point they work"() {
    given: "i create a string feature"
      def feature = applicationSqlApi.createApplicationFeature(app.id,
        new Feature().name("str1-feature").description("str1-feature").key("FSTR1").valueType(FeatureValueType.STRING), superPerson, Opts.empty())
        .find { it.key == 'FSTR1' }
    and: "i set the value of the feature"
      def fv1 = featureSqlApi.updateFeatureValueForEnvironment(env.id, feature.key, new FeatureValue().locked(false).valueString("fred"), perms)
      def fv2 = featureSqlApi.getFeatureValueForEnvironment(env.id, feature.key)
    when: "i update the value of the feature and update the rollout strategies"
      def fvUpdated = featureSqlApi.updateFeatureValueForEnvironment(env.id, feature.key, fv1.valueString("mary").rolloutStrategies([
        new RolloutStrategy().name("rs1").percentage(20).value(10)
      ]), perms)
    and: "i update the value of the feature and add another rollout strategy"
      def fvFromHistory = featureSqlApi.updateFeatureValueForEnvironment(env.id, feature.key, fv2.rolloutStrategies([
        new RolloutStrategy().name("rs2").percentage(15).value(6)
      ]), perms)
    then:
      fvFromHistory.rolloutStrategies.size() == 2
      fvFromHistory.rolloutStrategies.find({it.name == 'rs1'}).percentage == 20
      fvFromHistory.rolloutStrategies.find({it.name == 'rs1'}).value == 10
      fvFromHistory.rolloutStrategies.find({it.name == 'rs2'}).value == 6
      fvFromHistory.rolloutStrategies.find({it.name == 'rs2'}).percentage == 15
      fvFromHistory.valueString == 'mary'
      fvUpdated.valueString == 'mary'
  }

  def "when i update a boolean feature value and then update it again with the historical version, nothing changes"() {
    given: "i create a feature"
      def feature = applicationSqlApi.createApplicationFeature(app.id,
        new Feature().name("bool-feature").description("bool-feature").key("FBOOL").valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
        .find { it.key == 'FBOOL' }
    and: "i get the feature value"
      def fv = featureSqlApi.getFeatureValueForEnvironment(env.id, feature.key)
      def fv2 = featureSqlApi.getFeatureValueForEnvironment(env.id, feature.key)
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
