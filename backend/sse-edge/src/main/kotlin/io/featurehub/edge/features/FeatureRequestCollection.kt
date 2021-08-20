package io.featurehub.edge.features

import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.mr.model.DachaKeyDetailsResponse
import io.featurehub.sse.model.Environment
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

class FeatureRequestCollection(
  private val requests: List<FeatureRequester>,
  private val featureTransformer: FeatureTransformer,
  private val clientContext: ClientContext,
  private val future: CompletableFuture<List<FeatureRequestResponse>>
) : FeatureRequestCompleteNotifier {
  private val completed: MutableCollection<FeatureRequesterSource> = ConcurrentLinkedQueue()

  // this gets called on each requester each time a response comes back. Once it matches
  // the number that went in, we can process and complete the future
  override fun complete(key: FeatureRequesterSource) {
    completed.add(key)

    if (completed.size == requests.size) {
      future.complete(completed.map { req -> transformFeatures(req.details, req.key) }.toList())
    }
  }

  private fun transformFeatures(details: DachaKeyDetailsResponse?, key: KeyParts): FeatureRequestResponse {
    val env = Environment().id(key.environmentId)
    if (details == null)
      return FeatureRequestResponse(env, false, key)

    return FeatureRequestResponse(env
      .features(featureTransformer.transform(details.features, clientContext)), true, key)
  }
}
