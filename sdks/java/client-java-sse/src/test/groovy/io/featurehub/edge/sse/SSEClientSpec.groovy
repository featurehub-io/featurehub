package io.featurehub.edge.sse

import io.featurehub.client.ClientFeatureRepository
import io.featurehub.client.FeatureHubConfig
import io.featurehub.client.FeatureStore
import io.featurehub.client.Readyness
import io.featurehub.client.edge.EdgeConnectionState
import io.featurehub.client.edge.EdgeRetryService
import io.featurehub.sse.model.SSEResultState
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import spock.lang.Specification

class SSEClientSpec extends Specification {
  EventSource mockEventSource
  EdgeRetryService retry
  FeatureStore repository
  FeatureHubConfig config
  EventSourceListener esListener
  SSEClient client

  def setup() {
    mockEventSource = Mock(EventSource)
    retry = Mock(EdgeRetryService)
    repository = Mock(FeatureStore)
    config = Mock(FeatureHubConfig)
    config.realtimeUrl >> "http://special"

    client = new SSEClient(repository, config, retry) {
      @Override
      protected EventSource makeEventSource(Request request, EventSourceListener listener) {
        esListener = listener
        return mockEventSource
      }
    }
  }

  def "success lifecycle"() {
    when: "i poll"
      client.poll()
      esListener.onEvent(mockEventSource, '1', "features", "sausage")
    then:
      1 * repository.notify(SSEResultState.FEATURES, "sausage")
      1 * retry.edgeResult(EdgeConnectionState.SUCCESS, client)
  }

  def "success then bye but not close lifecycle"() {
    when: "i poll"
      client.poll()
      esListener.onEvent(mockEventSource, '1', "features", "sausage")
      esListener.onEvent(mockEventSource, '1', "bye", "sausage")

    then:
      1 * repository.notify(SSEResultState.FEATURES, "sausage")
      1 * repository.notify(SSEResultState.BYE, "sausage")
      1 * retry.edgeResult(EdgeConnectionState.SUCCESS, client)
      0 * retry.edgeResult(EdgeConnectionState.SERVER_SAID_BYE, client)
  }

  def "success then bye then close lifecycle"() {
    when: "i poll"
      client.poll()
      esListener.onEvent(mockEventSource, '1', "features", "sausage")
      esListener.onEvent(mockEventSource, '1', "bye", "sausage")
      esListener.onClosed(mockEventSource)
    then:
      1 * repository.notify(SSEResultState.FEATURES, "sausage")
      1 * repository.notify(SSEResultState.BYE, "sausage")
      1 * retry.edgeResult(EdgeConnectionState.SUCCESS, client)
      1 * retry.edgeResult(EdgeConnectionState.SERVER_SAID_BYE, client)
      1 * repository.notify(SSEResultState.FAILURE, null)
      1 * repository.readyness >> Readyness.NotReady
  }

  def "success then close with no bye"() {
    when: "i poll"
      client.poll()
      esListener.onEvent(mockEventSource, '1', "features", "sausage")
      esListener.onClosed(mockEventSource)
    then:
      1 * repository.notify(SSEResultState.FEATURES, "sausage")
      1 * retry.edgeResult(EdgeConnectionState.SUCCESS, client)
      1 * retry.edgeResult(EdgeConnectionState.SERVER_WAS_DISCONNECTED, client)
      1 * repository.notify(SSEResultState.FAILURE, null)
      1 * repository.readyness >> Readyness.NotReady
  }

  def "open then immediate failure"() {
    when: "i poll"
      client.poll()
//      esListener.onOpen(mockEventSource, Mock(Response))
      esListener.onFailure(mockEventSource, null, null)
    then:
      1 * repository.readyness >> Readyness.NotReady
      1 * repository.notify(SSEResultState.FAILURE, null)
      1 * retry.edgeResult(EdgeConnectionState.SERVER_WAS_DISCONNECTED, client)
  }

  def "when i context change with a client side key, it gives me a future which resolves readyness"() {
    when: "i change context"
      def future = client.contextChange("header")
      esListener.onEvent(mockEventSource, "1", "features", "data")
    then:
      1 * repository.notify(SSEResultState.FEATURES, "data")
      1 * repository.readyness >> Readyness.Failed
      1 * retry.edgeResult(EdgeConnectionState.SUCCESS, client)
      future.get() == Readyness.Failed
  }
}
