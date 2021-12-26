package io.featurehub.edge.features

import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.strategies.ClientContext
import io.prometheus.client.Gauge
import jakarta.inject.Inject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

open class DachaRequestOrchestrator @Inject constructor(
  private val featureTransformer: FeatureTransformer, private val dachaApi: DachaClientServiceRegistry,
  private val executor: EdgeConcurrentRequestPool
) : DachaFeatureRequestSubmitter {
  private val getMap = ConcurrentHashMap<KeyParts, FeatureRequester>()

  companion object {
    val inflightGauge = Gauge.build("edge_get_inflight_requests", "Inflight GET request Counter").register()
  }

  override fun request(
    keys: List<KeyParts>,
    context: ClientContext,
    etags: EtagStructureHolder
  ): List<FeatureRequestResponse> {

    // we need at least one for it to work
    if (keys.isEmpty()) {
      return arrayListOf();
    }

    inflightGauge.inc()

    val future = CompletableFuture<List<FeatureRequestResponse>>()

    // get an existing or create a new one for each of the sdk urls
    val getters = keys
      .map { key ->
        getMap.computeIfAbsent(key) { createInflightRequest(key) }
      }.toList()

    // now create a collector for the requests to notify
    val action = getRequestCollector(getters, context, future, etags)

    // and tell them to go get the data or add us to their list
    getters.forEach { getter -> getter.add(action) }

    val result = future.get()

    inflightGauge.dec()

    return result
  }

  protected open fun createInflightRequest(key: KeyParts): FeatureRequester =
    FeatureRequesterSource(dachaApi.getApiKeyService(key.cacheName), key, executor, this)

  protected open fun getRequestCollector(
    getters: List<FeatureRequester>,
    context: ClientContext,
    future: CompletableFuture<List<FeatureRequestResponse>>,
    etags: EtagStructureHolder
  ): FeatureRequestCompleteNotifier = FeatureRequestCollection(getters.size, featureTransformer, context, future, etags)


  override fun requestForKeyComplete(key: KeyParts) {
    getMap.remove(key)
  }
}
