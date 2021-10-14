package io.featurehub.edge.features

import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.mr.model.DachaKeyDetailsResponse
import io.featurehub.sse.model.Environment
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

class FeatureRequestCollection(
  private val requestCount: Int,
  private val featureTransformer: FeatureTransformer,
  private val clientContext: ClientContext,
  private val future: CompletableFuture<List<FeatureRequestResponse>>,
  private val etags: EtagStructureHolder
) : FeatureRequestCompleteNotifier {
  private val completed: MutableCollection<FeatureRequester> = ConcurrentLinkedQueue()

  // this gets called on each requester each time a response comes back. Once it matches
  // the number that went in, we can process and complete the future
  override fun complete(key: FeatureRequester) {
    completed.add(key)

    if (completed.size == requestCount) {
      // determine first if any of the environments failed its etag match
      val sendFullResults = !etags.validEtag || completed.find { !etags.environmentTags[it.key].equals(it.details?.etag) } != null
      future.complete(completed.map { req -> transformFeatures(req.details, req.key, sendFullResults) }.toList())
    }
  }

  private fun transformFeatures(details: DachaKeyDetailsResponse?, key: KeyParts, sendFullResults: Boolean): FeatureRequestResponse {
    val env = Environment().id(key.environmentId)

    if (details == null) {
      return FeatureRequestResponse(env, FeatureRequestSuccess.FAILED, key, "0")
    }

    if (!sendFullResults) {
      return FeatureRequestResponse(env, FeatureRequestSuccess.NO_CHANGE, key, "")
    }

    return FeatureRequestResponse(env
      .features( featureTransformer.transform(details.features, clientContext)), FeatureRequestSuccess.SUCCESS, key, details.etag!!)
  }
}
