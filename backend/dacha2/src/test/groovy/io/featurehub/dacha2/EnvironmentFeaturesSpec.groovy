package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
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
}
