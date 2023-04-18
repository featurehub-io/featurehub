package io.featurehub.dacha

import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.CacheFeatureValue
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.mr.model.FeatureValueType
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class InternalMemoryCacheSpec extends Specification {
  InMemoryCache cache

  def setup() {
    cache = new InMemoryCache()
  }

  def "when we have a single published environment and add the same feature with a same key and get the published environments"() {
    given:
      def featureId = UUID.randomUUID()
      def valueId = UUID.randomUUID()
      def envId = UUID.randomUUID()
      def cacheFeature = new CacheEnvironmentFeature()
        .feature(
          new CacheFeature().id(featureId).version(1).valueType(FeatureValueType.BOOLEAN).key("shop_carton"))
        .value(new CacheFeatureValue().id(valueId).value(false).key("shop_carton").locked(false).retired(false).version(9))

      def pe = new PublishEnvironment()
        .action(PublishAction.CREATE)
        .organizationId(UUID.randomUUID()).portfolioId(UUID.randomUUID()).applicationId(UUID.randomUUID())
          .count(1)
          .environment(new CacheEnvironment().id(envId).version(1))
          .featureValues([
            cacheFeature
          ])

      cache.updateEnvironment(pe)
    when: "i send a new value for the feature it replaces it in the environment"
      def c = {
        cache.updateFeatureValue(new PublishFeatureValue()
          .action(PublishAction.UPDATE)
          .environmentId(envId)
          .feature(new CacheEnvironmentFeature()
            .feature(
              new CacheFeature().id(featureId).version(1).valueType(FeatureValueType.BOOLEAN).key("shop_carton"))
            .value(new CacheFeatureValue().id(valueId).value(false).key("shop_carton").locked(false).retired(false).version(10))))
      }
      def c1 = new CompletableFuture<Boolean>();
      def c2 = new CompletableFuture<Boolean>();
      def t1 = new Thread() {
        @Override
        void run() {
          c()
          c1.complete(true);
        }
      }
      def t2 = new Thread() {
        @Override
        void run() {
          c()
          c2.complete(true);
        }
      }
      t1.start()
      t2.start()
      c1.get()
      c2.get()
    then: "published environments is 1 with 1 feature and feature value"
      cache.environments().findFirst().get().featureValues.size() == 1
  }
}
