package io.featurehub.client.jersey

import cd.connect.openapi.support.ApiClient
import io.featurehub.client.ClientFeatureRepository
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
  def sdkPartialUrl
  FeatureService mockFeatureService
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
        protected FeatureService makeFeatureServiceClient(ApiClient apiClient) {
          basePath = apiClient.basePath
          sdkPartialUrl = sdkUrl
          return super.makeFeatureServiceClient(apiClient)
        }
      }
    then: "the urls are correctly initialize"
      targetUrl == url
      basePath == 'http://localhost:80'
      sdkPartialUrl == 'features/sdk-url'
  }

  def "test the set feature sdk call"() {
    given: "I have a mock feature service"
      mockFeatureService = Mock(FeatureService)
    and: "I have a client and mock the feature service url"
      def client = new JerseyClient("http://localhost:80/features/sdk-url", false, new ClientFeatureRepository(1)) {
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
      1 * mockFeatureService.setFeatureState("features/sdk-url", "key", update)
  }

  def "test the set feature sdk call using a Feature"() {
    given: "I have a mock feature service"
      mockFeatureService = Mock(FeatureService)
    and: "I have a client and mock the feature service url"
      def client = new JerseyClient("http://localhost:80/features/sdk-url2", false, new ClientFeatureRepository(1)) {
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
      1 * mockFeatureService.setFeatureState("features/sdk-url2", "FEATURE", update)
  }

  int counter
  Executor executor
  def "ensure we can listen for events and they are passed off correctly to the client feature repository"() {
    given: "we have a mock repository"
      mockRepository = Mock(ClientFeatureRepository)
    and: "we have an executor"
      executor = new Executor() {
        @Override
        void execute(Runnable command) {
          if (counter == 0) { // ignore subsequent attempts to restart
            command.run();
          }
        }
      }
    and: "a mock target"
      mockEventSource = Mock(WebTarget)
      Invocation.Builder builder = Mock(Invocation.Builder)
      mockEventSource.request() >> builder
      EventInput eventInput = Mock(EventInput)
      builder.get(EventInput) >> eventInput
      counter = 0
      eventInput.isClosed() >> {
        counter ++; log.info("counter is $counter");
        return counter != 1; }  // only run it once
    when: "i create the client"
      def client = new JerseyClient("http://localhost:80/features/sdk-url2", false, mockRepository) {
        @Override
        protected WebTarget makeEventSourceTarget(Client client, String sdkUrl) {
          return mockEventSource
        }

        @Override
        protected Executor makeExecutor() {
          return executor
        }
      }
    and: "set the data to be some value"
      InboundEvent event = Mock(InboundEvent)
      event.name >> SSEResultState.FEATURE.getValue()
      event.readData() >> "features"
      eventInput.read() >> event
    and: "now initialize it, so it starts and then runs once and shuts down"
      client.init()
    then: "mock repository should have been called with a FEATURE event and the text 'features'"
      1 * mockRepository.notify(SSEResultState.FEATURE, "features")
  }
}
