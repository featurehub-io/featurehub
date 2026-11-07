package io.featurehub.dacha.caching

import cd.connect.app.config.ThreadLocalConfigurationSource
import io.featurehub.dacha.model.CacheEnvironment
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.Invocation
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.Response
import spock.lang.Specification
import spock.lang.Unroll

class FastlyPublisherSpec extends Specification {
  static final String KEY = "fastly-api-key"
  static final String SERVICE_ID = "fastly-service-id"

  Client client
  WebTarget target
  Invocation.Builder builder
  FastlyPublisher publisher
  UUID envId

  def setup() {
    client = Mock()
    target = Mock()
    builder = Mock()
    envId = UUID.randomUUID()
    publisher = new FastlyPublisher(KEY, SERVICE_ID, client)
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  PublishEnvironment env(PublishAction action) {
    return new PublishEnvironment()
      .action(action)
      .environment(new CacheEnvironment().id(envId).version(1))
      .featureValues([])
      .count(0)
  }

  PublishFeatureValue featureValue(PublishAction action) {
    return new PublishFeatureValue()
      .action(action)
      .environmentId(envId)
      .feature(new CacheEnvironmentFeature().feature(new CacheFeature().id(UUID.randomUUID()).key("FEATURE_ONE")))
  }

  // asserts exactly one purge of the expected environment, with the Fastly key header, and that the
  // response is always closed so connections are not leaked
  void expectPurgeOf(UUID id, int status = 200) {
    def response = Mock(Response)
    1 * client.target("https://api.fastly.com/service/${SERVICE_ID}/purge/${id}".toString()) >> target
    1 * target.request() >> builder
    1 * builder.header("Fastly-Key", KEY) >> builder
    1 * builder.post(null) >> response
    (1.._) * response.getStatus() >> status
    1 * response.close()
  }

  @Unroll
  def "a purge that Fastly rejects with HTTP #status is closed and does not propagate"() {
    when:
      publisher.updateFeature(featureValue(PublishAction.UPDATE))
    then:
      interaction { expectPurgeOf(envId, status) }
      0 * _
      noExceptionThrown()
    where:
      status << [401, 404, 500, 503]
  }

  @Unroll
  def "a feature value change with action #action purges the environment"() {
    when:
      publisher.updateFeature(featureValue(action))
    then:
      interaction { expectPurgeOf(envId) }
      0 * _
    where:
      action << PublishAction.values()
  }

  @Unroll
  def "an environment #action purges the environment"() {
    when:
      publisher.updateEnvironment(env(action))
    then:
      interaction { expectPurgeOf(envId) }
      0 * _
    where:
      action << [PublishAction.UPDATE, PublishAction.DELETE]
  }

  @Unroll
  def "an environment #action does not purge"() {
    when:
      publisher.updateEnvironment(env(action))
    then:
      0 * _
    where:
      action << [PublishAction.CREATE, PublishAction.EMPTY]
  }

  def "a service account change never purges"() {
    when:
      publisher.updateServiceAccount(new PublishServiceAccount().action(PublishAction.UPDATE))
    then:
      0 * _
  }

  @Unroll
  def "no purge is attempted when key is #key and service id is #serviceId"() {
    given:
      def unconfigured = new FastlyPublisher(key, serviceId, client)
    when:
      unconfigured.updateFeature(featureValue(PublishAction.UPDATE))
      unconfigured.updateEnvironment(env(PublishAction.DELETE))
      unconfigured.updateEnvironment(env(PublishAction.UPDATE))
    then:
      0 * _
    where:
      key  | serviceId
      null | SERVICE_ID
      KEY  | null
      null | null
  }

  def "a failure talking to Fastly does not propagate to the cache"() {
    given:
      client.target(_ as String) >> target
      target.request() >> builder
      builder.header(_, _) >> builder
      builder.post(null) >> { throw new ProcessingException("fastly is down") }
    when:
      publisher.updateFeature(featureValue(PublishAction.UPDATE))
      publisher.updateEnvironment(env(PublishAction.DELETE))
    then:
      noExceptionThrown()
  }

  def "each environment is purged separately"() {
    given:
      def otherEnvId = UUID.randomUUID()
    when:
      publisher.updateFeature(featureValue(PublishAction.UPDATE))
      publisher.updateFeature(new PublishFeatureValue().action(PublishAction.UPDATE).environmentId(otherEnvId)
        .feature(new CacheEnvironmentFeature().feature(new CacheFeature().id(UUID.randomUUID()).key("FEATURE_TWO"))))
    then:
      interaction { expectPurgeOf(envId) }
      interaction { expectPurgeOf(otherEnvId) }
  }

  @Unroll
  def "fastly is enabled only when both key and service id are configured: #config"() {
    given:
      ThreadLocalConfigurationSource.createContext(config)
    expect:
      FastlyPublisher.@Companion.fastlyEnabled() == enabled
    where:
      config                                                                               | enabled
      ['edge.cache.fastly.key': KEY, 'edge.cache.fastly.service-id': SERVICE_ID]          | true
      ['edge.cache.fastly.key': KEY]                                                       | false
      ['edge.cache.fastly.service-id': SERVICE_ID]                                         | false
      [:]                                                                                  | false
  }
}
