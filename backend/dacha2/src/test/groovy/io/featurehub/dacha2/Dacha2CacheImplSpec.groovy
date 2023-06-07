package io.featurehub.dacha2

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.CacheFeatureValue
import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.dacha.model.CacheServiceAccountPermission
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.mr.model.Dacha2Environment
import io.featurehub.mr.model.Dacha2ServiceAccount
import io.featurehub.mr.model.RoleType
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ServerErrorException
import spock.lang.Specification

class Dacha2CacheImplSpec extends Specification {
  Dacha2ServiceClient api
  Dacha2CacheImpl cache
  FeatureValuesFactory fvFactory
  UUID envId
  UUID serviceAccountId
  String apiKeyClientSide
  String apiKeyServerSide


  def setup() {
    api = Mock()
    fvFactory = new FeatureValuesFactoryImpl()
    cache = new Dacha2CacheImpl(api, fvFactory)

    envId = UUID.randomUUID()
    serviceAccountId = UUID.randomUUID()
    apiKeyClientSide = "1234*1"
    apiKeyServerSide = "1234"
  }

  CacheServiceAccount basicServiceAccount() {
    return new CacheServiceAccount().id(serviceAccountId)
      .apiKeyClientSide(apiKeyClientSide).apiKeyServerSide(apiKeyServerSide)
      .version(1)
  }

  CacheServiceAccount saNoPermissions() {
    return basicServiceAccount().permissions([
      new CacheServiceAccountPermission()
        .environmentId(envId)
        .permissions([])
    ])
  }

  CacheServiceAccount saWithPermission() {
    return basicServiceAccount().permissions([
      new CacheServiceAccountPermission()
        .environmentId(envId)
        .permissions([RoleType.CHANGE_VALUE])
    ])
  }

  def "basic successful get test with key"() {
    given:
      def key = 'interpol'
      cache.apiKey = key
    and: "we have mocked the apikey and env id"
      def pubEnv = new Dacha2Environment().env(new PublishEnvironment()
        .environment(new CacheEnvironment().id(envId))
        .featureValues([]))
      def serviceAccount = new Dacha2ServiceAccount().serviceAccount(saWithPermission())
    when:
      def result = cache.getFeatureCollection(envId, apiKeyClientSide)
    then:
      result.features.environment.environment.id == envId
      1 * api.getEnvironment(envId, key) >> pubEnv
      1 * api.getServiceAccount(apiKeyClientSide, key) >> serviceAccount
  }

  def "basic successful get test"() {
    given: "we have mocked the apikey and env id"
      def pubEnv = new Dacha2Environment().env(new PublishEnvironment()
        .environment(new CacheEnvironment().id(envId))
        .featureValues([]))
      def serviceAccount = new Dacha2ServiceAccount().serviceAccount(saWithPermission())
    when:
      def result = cache.getFeatureCollection(envId, apiKeyClientSide)
    then:
      result.features.environment.environment.id == envId
      1 * api.getEnvironment(envId, null) >> pubEnv
      1 * api.getServiceAccount(apiKeyClientSide, null) >> serviceAccount
  }

  def "when we ask for a feature and then receive an update for a feature it still is not available"() {
    when: "we ask for a feature"
      def result = cache.getFeatureCollection(envId, apiKeyClientSide)
    and: "then we publish an update"
      cache.updateFeature(new PublishFeatureValue()
        .action(PublishAction.CREATE)
        .environmentId(envId).feature(new CacheEnvironmentFeature().feature(new CacheFeature().id(UUID.randomUUID()).key("fred"))))
    and: "get the update again"
      def result2 = cache.getFeatureCollection(envId, apiKeyClientSide)
    then:
      result == null
      result2 == null
      1 * api.getServiceAccount(apiKeyClientSide, null) >> { throw new NotFoundException() }
  }

  def "we ask for a valid environment service-account and then receive a valid feature update and the update flows through"() {
    given: "we have mocked the apikey and env id"
      def featureId = UUID.randomUUID()
      def pubEnv = new Dacha2Environment().env(new PublishEnvironment()
        .environment(new CacheEnvironment().id(envId))
        .featureValues([new CacheEnvironmentFeature().feature(new CacheFeature().id(featureId).key("fred").version(1))]))
      def serviceAccount = new Dacha2ServiceAccount().serviceAccount(saWithPermission())
    when: "we ask using the client side key"
      def result = cache.getFeatureCollection(envId, apiKeyClientSide)
      def features1 = result.features.features[0].copy()    // internals will change, so we ask for a copy
    and: "we then send an update to the data"
      cache.updateFeature(new PublishFeatureValue()
        .action(PublishAction.UPDATE)
        .environmentId(envId).feature(new CacheEnvironmentFeature()
        .feature(new CacheFeature().key("fred").id(featureId).version(1))
        .value(new CacheFeatureValue().value("hello").version(1))))
    and: "we ask for the results using the server side key"
      def result2 = cache.getFeatureCollection(envId, apiKeyServerSide)
      def features2 = result2.features.features[0]
    then:
      result.features.toString() != null // just test this works
      result.perms.permissions == [RoleType.CHANGE_VALUE]
      result.serviceAccountId == serviceAccountId
      result.features.environment.environment.id == envId
      result.features.features.size() == 1
      features1.value == null
      features2.value.value == 'hello'
      features2.value.version == 1
      1 * api.getEnvironment(envId, null) >> pubEnv
      1 * api.getServiceAccount(apiKeyClientSide, null) >> serviceAccount
  }

  def "we ask for a valid environment service-account and then receive a invalid feature update and the update doesnt through"() {
    given: "we have mocked the apikey and env id"
      def featureId = UUID.randomUUID()
      def pubEnv = new Dacha2Environment().env(new PublishEnvironment()
        .environment(new CacheEnvironment().id(envId))
        .featureValues([new CacheEnvironmentFeature()
                          .feature(
                            new CacheFeature().id(featureId).key("fred").version(featureVersion1)
                          ).value(new CacheFeatureValue().value("hello").version(1))]))
      def serviceAccount = new Dacha2ServiceAccount().serviceAccount(saWithPermission())
    when: "we ask using the client side key"
      cache.getFeatureCollection(envId, apiKeyClientSide)
    and: "we then send an older feature but newer feature values"
      cache.updateFeature(new PublishFeatureValue()
        .action(PublishAction.UPDATE)
        .environmentId(envId).feature(new CacheEnvironmentFeature()
        .feature(new CacheFeature().id(featureId).key("fred").version(featureVersion2))
        .value(new CacheFeatureValue().value("hello").version(1))))
    and: "we ask for the results using the server side key"
      def result = cache.getFeatureCollection(envId, apiKeyServerSide)
    then:
      result.features.features[0].feature.version == featureVersion
      result.features.features[0].value.version == 1
      result.features.features[0].value.value == 'hello'
      1 * api.getEnvironment(envId, null) >> pubEnv
      1 * api.getServiceAccount(apiKeyClientSide, null) >> serviceAccount
    where:
      featureVersion1 | featureVersion2 | featureVersion
      1               | 0               | 1
      1               | 2               | 2
  }

  def "we ask for a valid environment service-account and then valid or invalid updates"() {
    given: "we have mocked the apikey and env id"
      def featureId = UUID.randomUUID()
      def pubEnv = new Dacha2Environment().env(new PublishEnvironment()
        .environment(new CacheEnvironment().id(envId))
        .featureValues([new CacheEnvironmentFeature()
                          .feature(new CacheFeature().id(featureId).key("fred").version(1))
                          .value(new CacheFeatureValue().value("pook").version(version1))]))

      def serviceAccount = new Dacha2ServiceAccount().serviceAccount(saWithPermission())
    when: "we ask using the client side key"
      def originalEtag = cache.getFeatureCollection(envId, apiKeyClientSide).features.etag
    and: "we then send an older feature but newer feature values"
      cache.updateFeature(new PublishFeatureValue()
        .action(PublishAction.UPDATE)
        .environmentId(envId).feature(new CacheEnvironmentFeature()
        .feature(new CacheFeature().id(featureId).key("fred").version(1))
        .value(new CacheFeatureValue().value("hello").version(version2))))
    and: "we ask for the results using the server side key"
      def result = cache.getFeatureCollection(envId, apiKeyServerSide)
    then:
      result.features.features[0].feature.version == 1
      result.features.features[0].value.version == version
      result.features.features[0].value.value == value
      etagsSame ? (originalEtag == result.features.etag) : (originalEtag != result.features.etag)  // it didn't change
      1 * api.getEnvironment(envId, null) >> pubEnv
      1 * api.getServiceAccount(apiKeyClientSide, null) >> serviceAccount

    where:
      version1 | version2 | version | value   | etagsSame
      2        | 1        | 2       | "pook"  | true
      1        | 2        | 2       | "hello" | false
  }

  def "we ask for a valid environment service-account and then receive a valid feature delete and the update flows through, etags change"() {
    given: "we have mocked the apikey and env id"
      def featureId = UUID.randomUUID()
      def envFeature = new CacheEnvironmentFeature()
          .feature(new CacheFeature().id(featureId).key("fred").version(1))
      def pubEnv = new Dacha2Environment().env(new PublishEnvironment()
        .environment(new CacheEnvironment().id(envId))
        .featureValues([envFeature]))
      def serviceAccount = new Dacha2ServiceAccount().serviceAccount(saWithPermission())
    when: "we test if the feature is there"
      def originalFeatures = cache.getFeatureCollection(envId, apiKeyServerSide)
      def originaletag = originalFeatures.features.etag
      def foundOnCreation = originalFeatures?.features?.features?.find({it.feature.id == featureId}) != null
    and: "we send an update to delete it"
      cache.updateFeature(new PublishFeatureValue().action(PublishAction.DELETE).environmentId(envId).feature(envFeature))
      def featureCache = cache.getFeatureCollection(envId, apiKeyServerSide)
      def newetag = featureCache.features.etag
    then:
      foundOnCreation
      featureCache.features.features.find({it.feature.id == featureId}) == null
      originaletag != null
      originaletag != newetag
      1 * api.getEnvironment(envId, null) >> pubEnv
      1 * api.getServiceAccount(apiKeyServerSide, null) >> serviceAccount
  }

  def "we update an environment and then do so again and it update"() {
    given: "we have mocked the apikey and env id"
      def pubEnv = new PublishEnvironment()
        .action(PublishAction.CREATE)
        .environment(new CacheEnvironment().id(envId).version(version1))
        .featureValues([])
    and: "we have an update ready"
      def pub = new PublishEnvironment()
        .action(PublishAction.UPDATE)
        .environment(new CacheEnvironment().id(envId).version(version2))
        .featureValues([])
    and: "we have a service account"
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.UPDATE).serviceAccount(saWithPermission()))
    when: "i send an update"
      cache.updateEnvironment(pubEnv)
    and: "send a bad update"
      cache.updateEnvironment(pub)
    then: "we are still on version 2"
      cache.getFeatureCollection(envId, apiKeyServerSide).features.environment.environment.version == result
    where:
      version1 | version2 | result
      2        | 1        | 2
      1        | 2        | 2
  }

  def "we ask for a valid environment service-account, which is then deleted, and then re-added"() {
    given: "we have mocked the apikey and env id"
      def featureId = UUID.randomUUID()
      def envFeature = new CacheEnvironmentFeature()
        .feature(new CacheFeature().id(featureId).key("fred").version(1))
      def pubEnv = new Dacha2Environment().env(new PublishEnvironment()
        .environment(new CacheEnvironment().id(envId))
        .featureValues([envFeature]))
      def serviceAccount = new Dacha2ServiceAccount().serviceAccount(saWithPermission())
    when: "we get the collection, causing the cache to fill"
      cache.getFeatureCollection(envId, apiKeyServerSide)
    and: "we send a delete for the environment"
      cache.updateEnvironment(new PublishEnvironment().action(PublishAction.DELETE)
          .environment(new CacheEnvironment().id(envId)))
      def result = cache.isEnvironmentPresent(envId)
    and: "we republish the environment"
      cache.updateEnvironment(new PublishEnvironment().action(PublishAction.UPDATE)
          .environment(new CacheEnvironment().id(envId))
          .featureValues([envFeature])
      )
      def resultWith = cache.getFeatureCollection(envId, apiKeyServerSide)
    then:
      !result
      resultWith != null
      1 * api.getEnvironment(envId, null) >> pubEnv
      1 * api.getServiceAccount(apiKeyServerSide, null) >> serviceAccount
  }

  def "sending updates to service accounts works as expected"() {
    given: "we have an environment"
      def pubEnv = new PublishEnvironment()
        .action(PublishAction.CREATE)
        .environment(new CacheEnvironment().id(envId).version(1))
        .featureValues([])
    and: "a service account"
      def sa = saWithPermission()
    and: "a remove permission variant"
      def saNoPerm = saNoPermissions()
    when: "we update the cache"
      cache.updateEnvironment(pubEnv)
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.CREATE).serviceAccount(sa))
      def result = cache.getFeatureCollection(envId, sa.apiKeyServerSide)
    and: "then send the update removing access"
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.UPDATE).serviceAccount(saNoPerm))
      def result2 = cache.getFeatureCollection(envId, sa.apiKeyClientSide)
      def result3 = cache.getFeatureCollection(envId, sa.apiKeyServerSide)
    and: "then send another update adding it back"
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.UPDATE).serviceAccount(saWithPermission().version(2)))
      def result4 = cache.getFeatureCollection(envId, sa.apiKeyServerSide)
    then:
      result != null
      result2 == null
      result3 == null
      result4 != null
      0 * _
  }

  def "we delete a service account that exists and confirm we cannot access it"() {
    given: "we have an environment"
      def pubEnv = new PublishEnvironment()
        .action(PublishAction.CREATE)
        .environment(new CacheEnvironment().id(envId).version(1))
        .featureValues([])
    and: "a service account"
      def sa = saWithPermission()
    when: "we update the cache"
      cache.updateEnvironment(pubEnv)
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.CREATE).serviceAccount(sa))
      def result = cache.getFeatureCollection(envId, sa.apiKeyServerSide)
    and: "then delete the service account"
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.DELETE).serviceAccount(sa))
      def result2 = cache.getFeatureCollection(envId, sa.apiKeyClientSide)
      def result3 = cache.getFeatureCollection(envId, sa.apiKeyServerSide)
    then:
      result != null
      result2 == null
      result3 == null
      0 * _
  }


  def "we ask for a valid environment service-account but have no permission and then permission follows and we can get the data"() {
    given: "we have an environment"
      def pubEnv = new PublishEnvironment()
        .action(PublishAction.CREATE)
        .environment(new CacheEnvironment().id(envId).version(1))
        .featureValues([])
    and: "a service account"
      def sa = basicServiceAccount()
    when: "we update the cache"
      cache.updateEnvironment(pubEnv)
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.CREATE).serviceAccount(sa))
      def result1 = cache.getFeatureCollection(envId, apiKeyServerSide)
    and: "then we send a permission variant"
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.CREATE).serviceAccount(saWithPermission()))
      def result2 = cache.getFeatureCollection(envId, apiKeyServerSide)
    then:
      result1 == null
      result2 != null
  }

  def "an existing environment receives a new feature"() {
    given: "we have an environment"
      def pubEnv = new PublishEnvironment()
        .action(PublishAction.CREATE)
        .environment(new CacheEnvironment().id(envId).version(1))
        .featureValues([])
    and: "a service account"
      def sa = saWithPermission()
    when: "we update the cache"
      cache.updateEnvironment(pubEnv)
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.CREATE).serviceAccount(sa))
    and: "we send a feature update which adds a feature"
      cache.updateFeature(new PublishFeatureValue()
        .action(PublishAction.CREATE)
        .environmentId(envId).feature(new CacheEnvironmentFeature().feature(new CacheFeature().id(UUID.randomUUID()).key("fred"))))
    then:
      cache.getFeatureCollection(envId, apiKeyServerSide).features.features.find({it.feature.key == 'fred'})
      cache.findEnvironment(envId) != null
  }

  def "getting an environment by API fails to find it - not found"() {
    given: "a service account"
      def sa = saWithPermission()
    when: "we update the cache"
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.CREATE).serviceAccount(sa))
      def result1 = cache.getFeatureCollection(envId, apiKeyServerSide)
      def result2 = cache.getFeatureCollection(envId, apiKeyServerSide)  // do it twice to confirm we are in the  miss cache
    then:
      result1 == null
      result2 == null
      1 * api.getEnvironment(envId, null) >> { throw new NotFoundException() }
      0 * _
  }

  def "getting an environment by API fails with server error"() {
    given: "a service account"
      def sa = saWithPermission()
    when: "we update the cache"
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.CREATE).serviceAccount(sa))
      def result1 = cache.getFeatureCollection(envId, apiKeyServerSide)
      def result2 = cache.getFeatureCollection(envId, apiKeyServerSide)  // do it twice to confirm we are not in the  miss cache
    then:
      result1 == null
      result2 == null
      2 * api.getEnvironment(envId, null) >> { throw new ServerErrorException(503) }
      0 * _
  }

  def "changing the service account api key causes inability to get data"() {
    given:
      def pubEnv = new Dacha2Environment().env(new PublishEnvironment()
        .environment(new CacheEnvironment().id(envId))
        .featureValues([]))
      def sa =  saWithPermission()
      def serviceAccount = new Dacha2ServiceAccount().serviceAccount(sa)
    when:
      def result = cache.getFeatureCollection(envId, apiKeyClientSide)
    then:
      result != null
      1 * api.getEnvironment(envId, null) >> pubEnv
      1 * api.getServiceAccount(apiKeyClientSide, null) >> serviceAccount
    when:
      def saReset = sa.copy().apiKeyClientSide("1").version(2)
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.UPDATE).serviceAccount(saReset))
    then:
      cache.getFeatureCollection(envId, apiKeyClientSide) == null
      cache.getFeatureCollection(envId, "1") != null
      cache.getFeatureCollection(envId, apiKeyServerSide) != null
    when:
      def saServerReset = saReset.copy().apiKeyServerSide("2").version(3)
      cache.updateServiceAccount(new PublishServiceAccount().action(PublishAction.UPDATE).serviceAccount(saServerReset))
    then:
      cache.getFeatureCollection(envId, "2") != null
      cache.getFeatureCollection(envId, apiKeyServerSide) == null

  }
}
