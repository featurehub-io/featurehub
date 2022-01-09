package io.featurehub.edge.features

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.edge.KeyParts
import io.featurehub.dacha.model.DachaKeyDetailsResponse
import spock.lang.Specification

class FeatureRequesterSourceSpec extends Specification {
  DachaApiKeyService api
  EdgeConcurrentRequestPool executor
  DachaFeatureRequestSubmitter submitter

  def setup() {
    api = Mock(DachaApiKeyService)
    executor = Mock(EdgeConcurrentRequestPool)
    submitter = Mock(DachaFeatureRequestSubmitter)
  }

  def "I should be able to submit a bunch of notifiers and after the first one, it should trigger an execution event"() {
    given: "i have a key"
      def key = new KeyParts("default", UUID.randomUUID(), "1234")
    and: "a service"
      def service = new FeatureRequesterSource(api, key, executor, submitter)
    when:
      service.add(Mock(FeatureRequestCompleteNotifier))
    then:
      1 * executor.execute(_)
  }

  def "Given I make 10 threaded requests to get the data, only one causes an execution"() {
    given: "i have a key"
      def key = new KeyParts("default", UUID.randomUUID(), "1234")
    and: "a service"
      def service = new FeatureRequesterSource(api, key, executor, submitter)
      def notifier = Mock(FeatureRequestCompleteNotifier)
    when:
      List<Thread> threads = []
      (1..10).each {
        threads.add(new Thread() {
          @Override
          void run() {
            service.add(notifier)
          }
        })
      }
      threads.each { it.start() }
      Thread.sleep(1000)
    then:
      1 * executor.execute(_)
      10 == service.notifyListener.size()
  }

  def "When the executor completes the submitted request, the submitter gets asked to remove the key and "() {
    given: "i have a key"
      def key = new KeyParts("default", UUID.randomUUID(), "1234")
    and: "a service"
      def service = new FeatureRequesterSource(api, key, executor, submitter)
      def notifier = Mock(FeatureRequestCompleteNotifier)
    and:
      def org = UUID.randomUUID()
      def port = UUID.randomUUID()
      def app = UUID.randomUUID()
      def sid = UUID.randomUUID()
    when:
      service.add(notifier)
    then:
      1 * executor.execute({ Runnable task ->
        task.run()
      })
      1 * api.getApiKeyDetails(key.environmentId, key.serviceKey) >>
        new DachaKeyDetailsResponse().portfolioId(port).organizationId(org).applicationId(app).serviceKeyId(sid)
      1 * submitter.requestForKeyComplete(key)
      1 * notifier.complete(service)
      key.organisationId == org
      key.portfolioId == port
      key.applicationId == app
      key.serviceKeyId == sid
      service.notifyListener.size() == 0
  }

  def "When a call fails to get the requisite information about the request, it will still clean up the key"() {
    given: "i have a key"
      def key = new KeyParts("default", UUID.randomUUID(), "1234")
    and: "a service"
      def service = new FeatureRequesterSource(api, key, executor, submitter)
      def notifier = Mock(FeatureRequestCompleteNotifier)
    when:
      service.add(notifier)
    then:
      1 * executor.execute({ Runnable task ->
        task.run()
      })
      1 * api.getApiKeyDetails(key.environmentId, key.serviceKey) >>
        { -> throw new RuntimeException() }
      1 * submitter.requestForKeyComplete(key)
      1 * notifier.complete(service)
      key.organisationId == null
      key.portfolioId == null
      key.applicationId == null
      key.serviceKeyId == null
      service.notifyListener.size() == 0

  }
}
