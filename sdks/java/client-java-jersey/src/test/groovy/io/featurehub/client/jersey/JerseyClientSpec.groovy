package io.featurehub.client.jersey

import cd.connect.openapi.support.ApiClient
import io.featurehub.client.ClientContext
import io.featurehub.client.ClientFeatureRepository
import io.featurehub.client.EdgeFeatureHubConfig
import io.featurehub.client.FeatureHubConfig
import io.featurehub.sse.api.FeatureService
import io.featurehub.sse.model.FeatureStateUpdate
import io.featurehub.sse.model.SSEResultState
import org.glassfish.jersey.media.sse.EventInput
import org.glassfish.jersey.media.sse.InboundEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

import javax.ws.rs.client.Client
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget
import java.util.concurrent.Executor

class JerseyClientSpec extends Specification {
  private static final Logger log = LoggerFactory.getLogger(JerseyClientSpec.class)
  def targetUrl
  def basePath
  FeatureHubConfig sdkPartialUrl
  FeatureService mockFeatureService
  ClientFeatureRepository mockRepository
  WebTarget mockEventSource

  def "basic initialization test works as expect"() {
    given: "i have a valid url"
      def url = new EdgeFeatureHubConfig("http://localhost:80/", "sdk-url")
    when: "i initialize with a valid kind of sdk url"
      def client = new JerseyClient(url, new ClientFeatureRepository(1)) {
        @Override
        protected WebTarget makeEventSourceTarget(Client client, String sdkUrl) {
          targetUrl = sdkUrl
          return super.makeEventSourceTarget(client, sdkUrl)
        }

        @Override
        protected FeatureService makeFeatureServiceClient(ApiClient apiClient) {
          basePath = apiClient.basePath
          sdkPartialUrl = fhConfig
          return super.makeFeatureServiceClient(apiClient)
        }
      }
    then: "the urls are correctly initialize"
      targetUrl == url.url
      basePath == 'http://localhost:80'
      sdkPartialUrl.sdkKey() == 'sdk-url'
  }

  def "test the set feature sdk call"() {
    given: "I have a mock feature service"
      mockFeatureService = Mock(FeatureService)
      def url = new EdgeFeatureHubConfig("http://localhost:80/", "sdk-url")
    and: "I have a client and mock the feature service url"
      def client = new JerseyClient(url, false, new ClientFeatureRepository(1), null) {
        @Override
        protected FeatureService makeFeatureServiceClient(ApiClient apiClient) {
          return mockFeatureService
        }
      }
    and: "i have a feature state update"
      def update = new FeatureStateUpdate().lock(true)
    when: "I call to set a feature"
      client.setFeatureState("key", update)
    then:
      mockFeatureService != null
      1 * mockFeatureService.setFeatureState("sdk-url", "key", update)
  }

  def "test the set feature sdk call using a Feature"() {
    given: "I have a mock feature service"
      mockFeatureService = Mock(FeatureService)
    and: "I have a client and mock the feature service url"
      def client = new JerseyClient(new EdgeFeatureHubConfig("http://localhost:80/", "sdk-url2"),
          false, new ClientFeatureRepository(1), null) {
        @Override
        protected FeatureService makeFeatureServiceClient(ApiClient apiClient) {
          return mockFeatureService
        }
      }
    and: "i have a feature state update"
      def update = new FeatureStateUpdate().lock(true)
    when: "I call to set a feature"
      client.setFeatureState(InternalFeature.FEATURE, update)
    then:
      mockFeatureService != null
      1 * mockFeatureService.setFeatureState("sdk-url2", "FEATURE", update)
  }

  def "a client side evaluation header does not trigger the context header to be set"() {
    given: "i have a client with a client eval url"
        def client = new JerseyClient(new EdgeFeatureHubConfig("http://localhost:80/", "sdk*url2"),
          false, new ClientFeatureRepository(1), null)
    when: "i set attributes"
        client.contextChange(["fred": ["mary", "susan"]])
    then:
        client.featurehubContextHeader == null
  }

  def "a server side evaluation header does trigger the context header to be set"() {
    given: "i have a client with a client eval url"
        def client = new JerseyClient(new EdgeFeatureHubConfig("http://localhost:80/", "sdk-url2"),
          false, new ClientFeatureRepository(1), null)
    when: "i set attributes"
        client.contextChange(["fred": ["mary", "susan"]])
    then:
        client.featurehubContextHeader != null
  }

}
