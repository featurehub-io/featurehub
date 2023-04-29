package io.featurehub.mr.events.common

import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.mr.model.FeatureValueType

import java.time.LocalDateTime

class DbFeatureTestProvider {

  static DbFeatureValue provideFeatureValue() {
    def applicationId = UUID.randomUUID()
    def featureId = UUID.randomUUID()
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
    def newFeatureValue = "new"
    def whenUpdated = LocalDateTime.now()
    def feature = new DbApplicationFeature.Builder()
      .name(featureName)
      .key(featureKey)
      .valueType(FeatureValueType.STRING)
      .build()
    feature.setId(featureId)

    // we can create a full database object without issues, because no
    // one is trying to save it. It won't be checked for integrity unless
    // someone tries to save or update it.
    def dbFeatureValue = new DbFeatureValue()
    dbFeatureValue.environment = environment
    dbFeatureValue.id = dbFeatureId

    dbFeatureValue.defaultValue = newFeatureValue
    dbFeatureValue.feature = feature
    dbFeatureValue.whoUpdated = person
    dbFeatureValue.whenUpdated = whenUpdated
    return dbFeatureValue
  }
}
