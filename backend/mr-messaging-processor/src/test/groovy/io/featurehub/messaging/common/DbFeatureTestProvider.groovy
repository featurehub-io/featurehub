package io.featurehub.messaging.common

import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.mr.model.FeatureValueType
import org.apache.commons.lang3.RandomStringUtils

import java.time.LocalDateTime

class DbFeatureTestProvider {

  static DbFeatureValue provideFeatureValue() {
    def applicationId = UUID.randomUUID()
    def featureId = UUID.randomUUID()
    def featureValueId = UUID.randomUUID()
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
      .name(RandomStringUtils.randomAlphanumeric(10))
      .build()
    application.setId(applicationId)
    def environment = new DbEnvironment.Builder()
      .parentApplication(application)
      .name(RandomStringUtils.randomAlphanumeric(10))
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
      .parentApplication(application)
      .valueType(FeatureValueType.STRING)
      .build()
    feature.setId(featureId)
    def dbFeatureValue = new DbFeatureValue(person, false, feature, environment, newFeatureValue).with {
      it.id = featureValueId
      it.whenUpdated = whenUpdated
      it
    }
    return dbFeatureValue
  }

}
