package io.featurehub.dacha2

import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.CloudEventReceiverRegistryMock
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.model.FeatureValueType
import org.glassfish.hk2.api.IterableProvider
import spock.lang.Specification

import java.time.OffsetDateTime

class Dacha2CloudEventListenerImplSpec extends Specification {
  CloudEventBuilder builder
  Dacha2CloudEventListenerImpl listener
  Dacha2CacheListener cache
  CloudEventReceiverRegistry register
  IterableProvider<Dacha2CacheListener> cacheProvider

  UUID serviceAccountId
  String apiKeyClientSide
  String apiKeyServerSide


  CacheServiceAccount basicServiceAccount() {
    return new CacheServiceAccount().id(serviceAccountId)
      .apiKeyClientSide(apiKeyClientSide).apiKeyServerSide(apiKeyServerSide)
      .version(1)
  }

  def setup() {
    cache = Mock()
    cacheProvider = Mock(IterableProvider)
    cacheProvider.iterator() >> [cache].iterator()
    register = new CloudEventReceiverRegistryMock()
    listener = new Dacha2CloudEventListenerImpl(cacheProvider, register)
    listener.init()
    serviceAccountId = UUID.randomUUID()
    apiKeyClientSide = "1234*1"
    apiKeyServerSide = "1234"

    builder = CloudEventBuilder.v1()
      .withId("0")
      .withSource(new URI("http://localhost"))
      .withTime(OffsetDateTime.now())
  }

  PublishEnvironment createEnv() {
    return new PublishEnvironment().action(PublishAction.CREATE)
      .organizationId(UUID.randomUUID())
      .applicationId(UUID.randomUUID())
      .portfolioId(UUID.randomUUID())
      .count(0)
      .environment(
        new CacheEnvironment().version(1).id(UUID.randomUUID())
      ).featureValues([])
  }

  def "i can send a valid environment"() {
    given:
      builder.withSubject(PublishEnvironment.CLOUD_EVENT_SUBJECT)
      .withType(PublishEnvironment.CLOUD_EVENT_TYPE)
    and: "data to  publish"
      def p =createEnv()
      CacheJsonMapper.toEventData(builder, p, false)
    when:
      register.process(builder.build())
    then:
      1 * cache.updateEnvironment(p)
      0 * _
  }

  def "i can send an invalid environment"() {
    given:
      builder.withSubject(PublishEnvironment.CLOUD_EVENT_SUBJECT)
        .withType("bad-type")
    and: "data to  publish"
      CacheJsonMapper.toEventData(builder, createEnv(), false)
    when:
      register.process(builder.build())
    then:
      0 * _
  }


  def "i can send a valid service account"() {
    given:
      builder.withSubject(PublishServiceAccount.CLOUD_EVENT_SUBJECT)
        .withType(PublishServiceAccount.CLOUD_EVENT_TYPE)
    and: "publish"
      def s = new PublishServiceAccount().count(0).action(PublishAction.CREATE).serviceAccount(basicServiceAccount())
      CacheJsonMapper.toEventData(builder, s, false)
    when:
      register.process(builder.build())
    then:
      1 * cache.updateServiceAccount(s)
      0 * _
  }

  def "i can send an invalid service account"() {
    given:
      builder.withSubject(PublishServiceAccount.CLOUD_EVENT_SUBJECT)
        .withType("bad")
    and: "publish"
      def s = new PublishServiceAccount().count(0).action(PublishAction.CREATE).serviceAccount(basicServiceAccount())
      CacheJsonMapper.toEventData(builder, s, false)
    when:
      register.process(builder.build())
    then:
      0 * _
  }

  PublishFeatureValues features() {
    return new PublishFeatureValues()
      .features([feature("fred"), feature("mary")])
  }

  protected PublishFeatureValue feature(String key) {
    new PublishFeatureValue()
      .action(PublishAction.CREATE)
      .environmentId(UUID.randomUUID()).
      feature(new CacheEnvironmentFeature().feature(new CacheFeature()
        .version(1)
        .id(UUID.randomUUID())
        .valueType(FeatureValueType.BOOLEAN).id(UUID.randomUUID()).key(key)))
  }

  def "i can send a valid feature set"() {
    given:
      builder.withSubject(PublishFeatureValues.CLOUD_EVENT_SUBJECT)
        .withType(PublishFeatureValues.CLOUD_EVENT_TYPE)
    and:
      def f = features()
      CacheJsonMapper.toEventData(builder, f, false)
    when:
      register.process(builder.build())
    then:
      1 * cache.updateFeature(f.features[0])
      1 * cache.updateFeature(f.features[1])
      0 * _
  }

  def "i can send an invalid feature set"() {
    given:
      builder.withSubject(PublishFeatureValues.CLOUD_EVENT_SUBJECT)
        .withType("bad")
    and:
      def f = features()
      CacheJsonMapper.toEventData(builder, f, false)
    when:
      register.process(builder.build())
    then:
      0 * _
  }
}
