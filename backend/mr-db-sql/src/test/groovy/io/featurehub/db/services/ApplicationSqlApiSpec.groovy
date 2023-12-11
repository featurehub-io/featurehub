//package io.featurehub.db.services
//
//import io.ebean.DB
//import io.ebean.Database
//import io.featurehub.db.api.PortfolioApi
//import io.featurehub.db.model.DbApplication
//import io.featurehub.db.model.DbPortfolio
//import io.featurehub.mr.events.common.CacheSource
//import io.featurehub.mr.model.Application
//import io.featurehub.mr.model.Group
//import io.featurehub.mr.model.Person
//import spock.lang.Shared
//import spock.lang.Specification
//
//class ApplicationSqlApiSpec extends BaseSpec {
//
//  @Shared PersonSqlApi personSqlApi
//  @Shared DbPortfolio portfolio1
//  @Shared DbPortfolio portfolio2
//  @Shared ApplicationSqlApi appApi
//  @Shared EnvironmentSqlApi environmentSqlApi
//  @Shared Person portfolioPerson
//  @Shared Group p1AdminGroup
//  @Shared PortfolioApi portfolioApi
//
//  def setup() {
//    baseSetupSpec()
//    personSqlApi = new PersonSqlApi(database, convertUtils, archiveStrategy, Mock(InternalGroupSqlApi))
//    environmentSqlApi = new EnvironmentSqlApi(database, convertUtils, Mock(CacheSource), archiveStrategy, Mock(WebhookEncryptionService))
//    appApi = new ApplicationSqlApi(convertUtils, Mock(CacheSource), archiveStrategy, Mock(InternalFeatureSqlApi))
//
//  }
//
//  def "GetApplication"() {
//    given:
//    def application =  appApi.createApplication(portfolio1.id, new Application().name("ghost").description("some desc"), superPerson)
//
//  }
//}
