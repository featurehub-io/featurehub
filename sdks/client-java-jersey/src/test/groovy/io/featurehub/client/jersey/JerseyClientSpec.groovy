package io.featurehub.client.jersey

import cd.connect.openapi.support.ApiClient
import io.featurehub.client.ClientFeatureRepository
import io.featurehub.client.Feature
import io.featurehub.sse.api.FeaturesService
import io.featurehub.sse.model.FeatureStateUpdate
import io.featurehub.sse.model.SSEResultState
import org.glassfish.jersey.media.sse.EventInput
import org.glassfish.jersey.media.sse.InboundEvent
import spock.lang.Specification

import javax.ws.rs.client.Client
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget

class JerseyClientSpec extends Specification {
  def targetUrl
  def basePath
  def sdkPartialUrl
  FeaturesService mockFeatureService
  ClientFeatureRepository mockRepository
  WebTarget mockEventSource

  def "basic initialization test works as expect"() {
    given: "i have a valid url"
      def url = "http://localhost:80/features/sdk-url"
    when: "i initialize with a valid kind of sdk url"
      def client = new JerseyClient(url, false, new ClientFeatureRepository(1)) {
        @Override
        protected WebTarget makeEventSourceTarget(Client client, String sdkUrl) {
          targetUrl = sdkUrl
          return super.makeEventSourceTarget(client, sdkUrl)
        }

        @Override
        protected FeaturesService makeFeatureServiceClient(ApiClient apiClient) {
          basePath = apiClient.basePath
          sdkPartialUrl = sdkUrl
          return super.makeFeatureServiceClient(apiClient)
        }
      }
    then: "the urls are correctly initialize"
      targetUrl == url
      basePath == 'http://localhost:80'
      sdkPartialUrl == 'sdk-url'
  }

  def "test the set feature sdk call"() {
    given: "I have a mock feature service"
      mockFeatureService = Mock(FeaturesService)
    and: "I have a client and mock the feature service url"
      def client = new JerseyClient("http://localhost:80/features/sdk-url", false, new ClientFeatureRepository(1)) {
        @Override
        protected FeaturesService makeFeatureServiceClient(ApiClient apiClient) {
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
      mockFeatureService = Mock(FeaturesService)
    and: "I have a client and mock the feature service url"
      def client = new JerseyClient("http://localhost:80/features/sdk-url2", false, new ClientFeatureRepository(1)) {
        @Override
        protected FeaturesService makeFeatureServiceClient(ApiClient apiClient) {
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

  int counter
  def "ensure we can listen for events and they are passed off correctly to the client feature repository"() {
    given: "we have a mock repository"
      mockRepository = Mock(ClientFeatureRepository)
    and: "a mock target"
      mockEventSource = Mock(WebTarget)
      Invocation.Builder builder = Mock(Invocation.Builder)
      mockEventSource.request() >> builder
      EventInput eventInput = Mock(EventInput)
      builder.get(EventInput) >> eventInput
      counter = 0
      eventInput.isClosed() >> {
        counter ++; print("counter is $counter");
        return counter != 1; }  // only run it once
    when: "i create the client"
      def client = new JerseyClient("http://localhost:80/features/sdk-url2", false, mockRepository) {
        @Override
        protected WebTarget makeEventSourceTarget(Client client, String sdkUrl) {
          return mockEventSource
        }
      }
    and: "i shut it down"
      client.shutdown()
    and: "set the data to be some value"
      InboundEvent event = new InboundEvent.Builder(null, null, null, null).name(SSEResultState.FEATURE.name()).write("features".bytes).build()
      eventInput.read() >> event
    and: "now initialize it, so it starts and then runs once and shuts down"
      client.init()
    then: "mock repository should have been called with a FEATURE event and the text 'features'"
      1 * mockRepository.notify(SSEResultState.FEATURE, "features")
  }
}
