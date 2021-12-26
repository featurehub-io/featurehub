package io.featurehub.edge.features

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.sse.model.FeatureEnvironmentCollection
import org.jetbrains.annotations.NotNull
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class FeatureRequesterSpec extends Specification {
  int inflightRequestCounter = 0
  FeatureRequester inflightRequest

  def setup() {
    inflightRequestCounter = 0
    inflightRequest = Mock(FeatureRequester)
  }

  def "when i pass no keys, i get no responses"() {
    given: "i have an orchestrator"
      def orch = new DachaRequestOrchestrator(Mock(FeatureTransformer), Mock(DachaClientServiceRegistry), Mock(EdgeConcurrentRequestPool))
    and: "i have no etags"
      def etags = new EtagStructureHolder([:], "", false)
    when: "i pass in no keys"
      def result = orch.request([], new ClientContext(false), etags)
    then:
      result.isEmpty()
  }

  def "when i pass 3 keys, i get 3 environments back"() {
    given: "I have 3 environments"
       def envs = [new FeatureEnvironmentCollection().id(UUID.randomUUID()), new FeatureEnvironmentCollection().id(UUID.randomUUID()), new FeatureEnvironmentCollection().id(UUID.randomUUID())]
    and: "a mock notifier"
      def notifier = Mock(FeatureRequestCompleteNotifier)
    and: "a mock dacha registry"
      def dacha = Mock(DachaClientServiceRegistry)
    and: "a mocked inflight request"
    and: "i have an overridden orchestrator"
      def orch = new DachaRequestOrchestrator(Mock(FeatureTransformer), dacha, Mock(EdgeConcurrentRequestPool)) {
        @Override
        protected FeatureRequester createInflightRequest(@NotNull KeyParts key) {
          dacha.getApiKeyService(key.cacheName)
          inflightRequestCounter ++
          return inflightRequest
        }

        @Override
        protected FeatureRequestCompleteNotifier getRequestCollector(@NotNull List<? extends FeatureRequester> getters,
                                                                     @NotNull ClientContext context,
                                                                     @NotNull CompletableFuture<List<FeatureEnvironmentCollection>> future,
        @NotNull EtagStructureHolder etags) {
          future.complete(envs)
          return notifier
        }
      }
    and: "I have a fake etags"
      def etags = new EtagStructureHolder([:], "", true)
    when:
      def result = orch.request([new KeyParts("default", UUID.randomUUID(), "x"),
                    new KeyParts("fred", UUID.randomUUID(), "x"),
                    new KeyParts("mary", UUID.randomUUID(), "x")], new ClientContext(false), etags)
    then:
      1 * dacha.getApiKeyService("default") >> Mock(DachaApiKeyService)
      1 * dacha.getApiKeyService("fred") >> Mock(DachaApiKeyService)
      1 * dacha.getApiKeyService("mary") >> Mock(DachaApiKeyService)
      3 == inflightRequestCounter
      3 * inflightRequest.add(notifier)
      result == envs

  }
}
