package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.dacha.model.CacheServiceAccountPermission
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.mr.model.Dacha2Environment
import io.featurehub.mr.model.Dacha2ServiceAccount
import io.featurehub.mr.model.RoleType
import spock.lang.Specification

class Dacha2CacheImplSpec extends Specification {
  Dacha2ServiceClient api
  Dacha2CacheImpl cache
  FeatureValuesFactory fvFactory

  def setup() {
    api = Mock()
    fvFactory = new FeatureValuesFactoryImpl()
    cache = new Dacha2CacheImpl(api, fvFactory)
  }

  def "basic successful get test"() {
    given: "we have mocked the apikey and env id"
      def envId = UUID.randomUUID()
      def apiKey = "1234"
      def pubEnv = new Dacha2Environment().env(new PublishEnvironment()
        .environment(new CacheEnvironment().id(envId))
        .featureValues([]))
      def serviceAccount = new Dacha2ServiceAccount().serviceAccount(new CacheServiceAccount().id(UUID.randomUUID())
        .apiKeyClientSide(apiKey).apiKeyServerSide("5678").permissions([
          new CacheServiceAccountPermission()
            .environmentId(envId)
            .permissions([RoleType.CHANGE_VALUE])
        ]))
    when:
      def result = cache.getFeatureCollection(envId, apiKey)
    then:
      result.features.environment.environment.id == envId
      1 * api.getEnvironment(envId, null) >> pubEnv
      1 * api.getServiceAccount(apiKey, null) >> serviceAccount
  }
}
