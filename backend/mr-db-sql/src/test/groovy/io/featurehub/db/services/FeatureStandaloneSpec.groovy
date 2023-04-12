package io.featurehub.db.services

import io.ebean.DB
import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.Opts
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbPortfolio
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Feature

class FeatureStandaloneSpec extends Base2Spec {
  ApplicationSqlApi appApi

  def setup() {
    appApi = new ApplicationSqlApi(convertUtils, Mock(CacheSource), archiveStrategy, Mock(InternalFeatureSqlApi))
  }

  def "i cannot overwrite another feature with the same name when i update"() {
    given: "i have a new portfolio + app setup" // have to for this test

      def port = new DbPortfolio.Builder().name("p7-app-feature").whoCreated(dbSuperPerson).organization(findOrganization()).build()
      db.save(port)
      def app = new DbApplication.Builder().whoCreated(dbSuperPerson).portfolio(port).name("feature-app-1").build()
      db.save(app)

    and: "i have two features"
      appApi.createApplicationFeature(app.id, new Feature().name("x").key("FEATURE_UPD2"), superPerson, Opts.empty())

      // we now have to save this or the next operational can't find it
      DB.currentTransaction().commit()
      DB.beginTransaction()

      def features = appApi.createApplicationFeature(app.id,
        new Feature().name('FEATURE_UPD3').key("FEATURE_UPD3"), superPerson, Opts.empty())

      Feature f2 = features.find({ it -> it.key == 'FEATURE_UPD3'})
    when: "i update the second to the same name as the first"
      appApi.updateApplicationFeature(app.id, 'FEATURE_UPD3', f2.key('FEATURE_UPD2'), Opts.empty())
    then:
      thrown ApplicationApi.DuplicateFeatureException
  }

}
