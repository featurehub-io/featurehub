package io.featurehub.mr.sdk

import io.featurehub.admin.ApiClient
import io.featurehub.admin.api.AuthServiceApi
import io.featurehub.admin.api.FeatureServiceApi
import io.featurehub.admin.api.PortfolioServiceApi
import io.featurehub.admin.model.SortOrder
import io.featurehub.admin.model.UserCredentials
import spock.lang.Specification

class ClientIntegrationSpecs extends Specification {
  def "I can connect and list the existing features"() {
    given: "i have a connection"
      def api = new ApiClient()
      api.setBasePath("http://localhost:8085")
    and: "i login"
      def authApi = new AuthServiceApi(api)
      def loggedIn = false
      def count = 0
      while (!loggedIn && count < 30) {
        try {
          def tp = authApi.login(new UserCredentials().email("test@mailinator.com").password("password123"))
          api.setBearerToken(tp.accessToken)
          loggedIn = true
          count = 0
        } catch (Exception ignored) {
          print("docker container not ready, sleeping for 2 seconds")
          Thread.sleep(2000)
          count ++
        }
      }
      if (!loggedIn) throw new RuntimeException("Failed to connect to docker server")
    when: "i can get a list of portfolios i can access"
      def portfolioApi = new PortfolioServiceApi(api)
      def portfolios = portfolioApi.findPortfolios(false, true, SortOrder.ASC, null, null)
//      println("portfolios are $portfolios")
    and:
      def featureApi = new FeatureServiceApi(api)
      def features = featureApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(portfolios[0].applications[0].id)
//      println("features are $features")
    then:
      portfolios.size() == 1
      portfolios[0].name == 'Test Portfolio'
      portfolios[0].applications.size() == 1
      portfolios[0].applications[0].name == 'Test Application'
      features.features.size() == 5
  }
}
