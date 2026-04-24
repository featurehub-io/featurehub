package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.CacheFeatureValue
import io.featurehub.dacha.model.PublishEnvironment
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class EnvironmentFeaturesSpec extends Specification {
  def "multi-threaded access to EnvironmentFeatures does not trigger a concurrent modification exception"() {
    given: "we have an environment features object with no data"
      def env = new EnvironmentFeatures(new PublishEnvironment().featureValues([]))
    and: "we have 20 threads"
      def futures = (1..20).collect { new FeatureHolderThread(env,
        new CacheEnvironmentFeature().feature(new CacheFeature().version(1).id(UUID.randomUUID()))) }
    when: "we update with 20 threads"
      futures.each { it.start() }
    and: "we wait for them to finish"
      futures.each { it.future.join() }
    then:
      1 == 1
  }

  def "MD5 etag test"() {
    given:
      def env = [
            new CacheEnvironmentFeature()
              .feature(new CacheFeature().id(UUID.fromString('9cd1a3ef-d06e-4292-95d4-86ec02a3fd5b')).version(1))
              .value(new CacheFeatureValue().id(UUID.fromString('ff550b40-04af-477e-86fb-5bfb30f378fa')).version(16)),
            new CacheEnvironmentFeature()
              .feature(new CacheFeature().id(UUID.fromString('76b612ae-a782-4780-aeaa-597870a8c3ea')).version(12))
              .value(new CacheFeatureValue().id(UUID.fromString('2002b94a-7624-4894-8dfa-56b408c61832')).version(1)),
            new CacheEnvironmentFeature()
              .feature(new CacheFeature().id(UUID.fromString('95f6c09a-20e7-41e2-9014-1421682b71b1')).version(7))
              .value(new CacheFeatureValue().id(UUID.fromString('91c424b1-79ea-41e1-af5d-df876e1209eb')).version(4)),

          ]
    when: "i calculate the etag"
      def etag = EnvironmentFeatures.@Companion.etagCalculator(env, 'none')
    then:
      etag == "5e3eafc18a57bba2080e6ce87b059b78"
    when: "i change the version of the first item to 17 the MD5 changes"
      env[0].value.version(17)
      etag = EnvironmentFeatures.@Companion.etagCalculator(env, 'none')
    then:
      etag != "5e3eafc18a57bba2080e6ce87b059b78"
  }
}
