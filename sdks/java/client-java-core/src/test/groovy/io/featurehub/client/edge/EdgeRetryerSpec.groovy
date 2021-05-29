package io.featurehub.client.edge

import spock.lang.Specification

import java.util.concurrent.ExecutorService

class EdgeRetryerSpec extends Specification {
  ExecutorService mockExecutor
  EdgeRetryer retryer
  int backoffBaseTime
  boolean backoffAdjustBackoff
  EdgeReconnector reconnector

  def setup() {
    mockExecutor = Mock(ExecutorService)
    reconnector = Mock(EdgeReconnector)
    retryer = new EdgeRetryer(100, 100, 100, 10, 100) {
      @Override
      protected ExecutorService makeExecutorService() {
        return mockExecutor
      }

      @Override
      protected void backoff(int baseTime, boolean adjustBackoff) {
        // do nothing
        backoffBaseTime = baseTime
        backoffAdjustBackoff = adjustBackoff
      }
    }

  }

  def "a new backoff should always be greater than the backoff multiplier"() {
    when: "i ask for a new backup 100 times"
      def backoffs =
        (1..100).collect { retryer.newBackoff(it) }
    then: "it is always greater"
      (1..100).each { it < backoffs[it-1] }
  }

  def "the backoff will never go above the maximum backoff"() {
    when: "i calculate the new backoff"
      def newBackoff = retryer.calculateBackoff(90, 1000)
    then:
      newBackoff <= 100
  }

  // can't test any randomness

  def "if the server says 'bye' then we will backoff with the bye timeout and not adjust backoff"() {
    when: "i send a bye event"
      retryer.edgeResult(EdgeConnectionState.SERVER_SAID_BYE, reconnector )
    then:
      !backoffAdjustBackoff
      backoffBaseTime == retryer.serverByeReconnectMs
      1 * reconnector.reconnect()
      1 * mockExecutor.submit({ Runnable task -> task.run()})
  }

  def "if the server says 'disconnect' then we will backoff with the disconnect timeout and adjust backoff"() {
    when: "i send a disconnected event"
      retryer.edgeResult(EdgeConnectionState.SERVER_WAS_DISCONNECTED, reconnector )
    then:
      backoffAdjustBackoff
      backoffBaseTime == retryer.serverDisconnectRetryMs
      1 * reconnector.reconnect()
      1 * mockExecutor.submit({ Runnable task -> task.run()})
  }

  def "if the server says 'connect timeout' then we will backoff with the connect timeout and adjust backoff"() {
    when: "i send a connect timeout event"
      retryer.edgeResult(EdgeConnectionState.SERVER_CONNECT_TIMEOUT, reconnector )
    then:
      backoffAdjustBackoff
      backoffBaseTime == retryer.serverConnectTimeoutMs
      1 * reconnector.reconnect()
      1 * mockExecutor.submit({ Runnable task -> task.run()})
  }

  def "if the server says success we shouldn't schedule any tasks"() {
    when: "i send a success event"
      retryer.edgeResult(EdgeConnectionState.SUCCESS, reconnector )
    then:
      0 * reconnector.reconnect()
      0 * mockExecutor.submit(_)
  }

  def "if the executor service is shut down, no calls are ignored"() {
    when: "i send a connect timeout event"
      retryer.edgeResult(EdgeConnectionState.SERVER_CONNECT_TIMEOUT, reconnector )
    then:
      1 * mockExecutor.isShutdown() >> true
      0 * reconnector.reconnect()
  }

  def "if the not found is sent, then further requests are ignored"() {
    when: "i send the api not found event"
      retryer.edgeResult(EdgeConnectionState.API_KEY_NOT_FOUND, reconnector )
    and:  "i send a connect timeout event"
      retryer.edgeResult(EdgeConnectionState.SERVER_CONNECT_TIMEOUT, reconnector )
    then:
      1 * mockExecutor.isShutdown() >> false
      0 * reconnector.reconnect()

  }
}
