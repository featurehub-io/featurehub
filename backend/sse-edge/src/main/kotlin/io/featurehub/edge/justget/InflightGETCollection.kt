package io.featurehub.edge.justget

import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.mr.model.DachaKeyDetailsResponse
import io.featurehub.sse.model.Environment
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

interface InflightGETNotifier {
  fun complete(key: InflightGETRequest)
}

class InflightGETCollection(
  private val requests: List<InflightGETRequest>,
  private val featureTransformer: FeatureTransformer,
  private val clientContext: ClientContext,
  private val future: CompletableFuture<List<Environment>>
) : InflightGETNotifier {
  val completed: MutableCollection<InflightGETRequest> = ConcurrentLinkedQueue()

  // this gets called on each requester each time a response comes back. Once it matches
  // the number that went in, we can process and complete the future
  override fun complete(key: InflightGETRequest) {
    completed.add(key)

    if (completed.size == requests.size) {
      future.complete(completed.map { req -> transformFeatures(req.details, req.key) }.toList())
    }
  }

  private fun transformFeatures(details: DachaKeyDetailsResponse?, key: KeyParts): Environment {
    return Environment().id(key.environmentId)
      .features(if (details == null) null else featureTransformer.transform(details.features, clientContext))
  }
}
