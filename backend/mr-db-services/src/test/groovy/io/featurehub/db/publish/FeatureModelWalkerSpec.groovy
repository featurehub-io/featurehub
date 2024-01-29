package io.featurehub.db.publish

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.test.DbSpecification
import io.featurehub.mr.model.FeatureValueType

class FeatureModelWalkerSpec extends DbSpecification {
  DbOrganization org
  DbPortfolio portfolio
  DbApplication app
  DbApplicationFeature feature
  DbFeatureValue dbFeatureValue
  DbEnvironment environment

  UUID featureId = UUID.fromString('10481d19-f71e-4380-87b4-c74e9526669')

  def setup() {
    org = new DbOrganization.Builder().name("org-name").build()
    portfolio = new DbPortfolio.Builder().name("mushroom").description("portobello").organization(org).build()
    app = new DbApplication.Builder().name("fred").portfolio(portfolio).build()
    feature = new DbApplicationFeature.Builder().description("app-feature")
      .key('mixed-doubles')
      .valueType(FeatureValueType.BOOLEAN)
      .name("mary").metaData('{"valid": true, "category": { "name": "champion"}}')
      .parentApplication(app).build()
    feature.id = featureId
    feature.version = 1
    def person = new DbPerson()
    environment = new DbEnvironment.Builder().parentApplication(app).name("production").build()
    dbFeatureValue = new DbFeatureValue(person, false, feature, environment, "true")
  }

  def "a basic template will walk the model and provide the right results"() {
    setup:
      ThreadLocalConfigurationSource.createContext(['sdk.feature.enhance.map': 'name={{feature.name}},category={{metadata.category.name}},portfolio={{feature.parentApplication.portfolio.name}}'])
    when:
      def walker = new FeatureModelWalkerService()
    then:
      walker.exists('name')
      walker.exists('category')
      walker.exists('portfolio')
    when:
      def result = walker.walk(feature, null, new CacheFeature(), null, [])
    then:
      result['name'] == 'mary'
      result['category'] == 'champion'
      result['portfolio'] == 'mushroom'
  }

  def "reading from a file produces the expected results"() {
    setup:
      def tmp = File.createTempFile('enhance', '.hbs')
      tmp.text = '{ "featureId": {{feature.id}}", "portfolioName": "{{ feature.parentApplication.portfolio.name }}, "environment": "{{ featureValue.environment.name }}, "valid": {{ metadata.valid }} }'
      ThreadLocalConfigurationSource.createContext(['sdk.feature.enhance.map': "info=#${tmp.absolutePath}".toString()])
    when:
      def walker = new FeatureModelWalkerService()
    then:
      walker.exists('info')
    when:
      def result = walker.walk(feature, dbFeatureValue, new CacheFeature(), null, [])
    then:
      result['info'] == '{ "featureId": 10481d19-f71e-4380-87b4-0c74e9526669", "portfolioName": "mushroom, "environment": "production, "valid": true }'
    cleanup:
      tmp.delete()
  }

  def "changing the metadata is possible"() {
    setup:
      ThreadLocalConfigurationSource.createContext(['sdk.feature.enhance.map': 'category={{metadata.category.name}}'])
    when:
      def walker = new FeatureModelWalkerService()
      def result = walker.walk(feature, null, new CacheFeature(), null, [])
    then:
      result['category'] == 'champion'
    when:
      feature.metaData = '{"category": { "name": "motorcross"}}'
      result = walker.walk(feature, null, new CacheFeature(), null, [])
    then: "we haven't changed the version"
      result['category'] == 'champion'
    when:
      feature.version = 2
      result = walker.walk(feature, null, new CacheFeature(), null, [])
    then:
      result['category'] == 'motorcross'
  }

  def "you can ask for the raw metadata"() {
    setup:
    ThreadLocalConfigurationSource.createContext(['sdk.feature.enhance.map': 'meta={{{feature.metaData}}}'])
    when:
      def walker = new FeatureModelWalkerService()
      def result = walker.walk(feature, null, new CacheFeature(), null, [])
    then:
      result['meta'] == feature.metaData
  }
}
