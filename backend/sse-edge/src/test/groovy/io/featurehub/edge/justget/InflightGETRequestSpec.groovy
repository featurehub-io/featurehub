package io.featurehub.edge.justget

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.edge.KeyParts
import spock.lang.Specification

class InflightGETRequestSpec extends Specification {
  DachaApiKeyService api
  EdgeConcurrentRequestPool executor
  InflightGETSubmitter submitter

  def setup() {
    api = Mock(DachaApiKeyService)
    executor = Mock(EdgeConcurrentRequestPool)
    submitter = Mock(InflightGETSubmitter)
  }

  def "I should be able to submit a bunch of notifiers and after the first one, it should trigger an execution event"() {
    given: "i have a key"
      def key = new KeyParts("default", UUID.randomUUID(), "1234")
    and: "a service"
      def service = new InflightGETRequest(api, key, executor, submitter)
    when:
      service.add(Mock(InflightGETNotifier))
    then:
      1 * executor.execute(_)
  }

  def "Given I make 10 threaded requests to get the data, only one causes an execution"() {
    given: "i have a key"
      def key = new KeyParts("default", UUID.randomUUID(), "1234")
    and: "a service"
      def service = new InflightGETRequest(api, key, executor, submitter)
      def notifier = Mock(InflightGETNotifier)
    when:
      List<Thread> threads = []
      (1..10).each { threads.add(new Thread() {
        @Override
        void run() {
          service.add(notifier)
        }
      })}
      threads.each { it.start() }
      Thread.sleep(1000)
    then:
      1 * executor.execute(_)
      10 == service.notifyListener.size()
  }

//  def "When the executor completes the submitted request, the submitter gets asked to remove the key and "() {
//
//  }
}
