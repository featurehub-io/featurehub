package io.featurehub.edge.justget

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.sse.model.Environment
import org.jetbrains.annotations.NotNull
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class InflightGETOrchestratorSpec extends Specification {
  int inflightRequestCounter = 0
  InflightRequest inflightRequest

  def setup() {
    inflightRequestCounter = 0
    inflightRequest = Mock(InflightRequest)
  }

  def "when i pass 3 keys, i get 3 environments back"() {
    given: "I have 3 environments"
       def envs = [new Environment().id(UUID.randomUUID()), new Environment().id(UUID.randomUUID()), new Environment().id(UUID.randomUUID())]
    and: "a mock notifier"
      def notifier = Mock(InflightGETNotifier)
    and: "a mock dacha registry"
      def dacha = Mock(DachaClientServiceRegistry)
    and: "a mocked inflight request"
    and: "i have an overridden orchestrator"
      def orch = new InflightGETOrchestrator(Mock(FeatureTransformer), dacha, Mock(EdgeConcurrentRequestPool)) {
        @Override
        protected InflightRequest createInflightRequest(@NotNull KeyParts key) {
          inflightRequestCounter ++
          return inflightRequest
        }

        @Override
        protected InflightGETNotifier getRequestCollector(@NotNull List<? extends InflightRequest> getters, @NotNull ClientContext context, @NotNull CompletableFuture<List<Environment>> future) {
          future.complete(envs)
          return notifier
        }
      }
    when:
      def result = orch.request([new KeyParts("default", UUID.randomUUID(), "x"),
                    new KeyParts("fred", UUID.randomUUID(), "x"),
                    new KeyParts("mary", UUID.randomUUID(), "x")], new ClientContext(false))
    then:
      1 * dacha.getApiKeyService("default") >> Mock(DachaApiKeyService)
      1 * dacha.getApiKeyService("fred") >> Mock(DachaApiKeyService)
      1 * dacha.getApiKeyService("mary") >> Mock(DachaApiKeyService)
      3 == inflightRequestCounter
      3 * inflightRequest.add(notifier)
      result == envs

  }
}
