package io.featurehub.db.services

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.db.api.Opts
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.services.strategies.StrategyDiffer
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Portfolio

class FeatureAuditingSpec extends Base2Spec {
  PortfolioSqlApi portfolioSqlApi
  Portfolio p1
  ApplicationSqlApi applicationSqlApi
  Application app
  CacheSource cacheSource
  Feature feature
  FeatureSqlApi featureSqlApi
  RolloutStrategyValidator rsValidator
  StrategyDiffer strategyDiffer

  def setup() {
    cacheSource = Mock()
    rsValidator = Mock()
    strategyDiffer = Mock()
    ThreadLocalConfigurationSource.createContext(['auditing.enable': 'true'])
    featureSqlApi = new FeatureSqlApi(db, convertUtils, cacheSource, rsValidator, strategyDiffer)
    portfolioSqlApi = new PortfolioSqlApi(db, convertUtils, archiveStrategy)
    p1 = portfolioSqlApi.createPortfolio(new Portfolio().name("basic").description("basic"), Opts.empty(), superPerson)
    applicationSqlApi = new ApplicationSqlApi(db, convertUtils, cacheSource, archiveStrategy, featureSqlApi)
    app = applicationSqlApi.createApplication(p1.id, new Application().name("app1").description("desc1"), superPerson)
    feature = applicationSqlApi.createApplicationFeature(app.id,
      new Feature().name("bool-feature").description("bool-feature").key("FBOOL").valueType(FeatureValueType.BOOLEAN), superPerson, Opts.empty())
      .find { it.key == 'FBOOL' }
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "i can create a value for a feature"() {
    when: "i create a feature value"

  }
}
