package io.featurehub.edge.features

import io.featurehub.dacha.model.DachaKeyDetailsResponse
import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.sse.model.FeatureEnvironmentCollection
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import java.net.ConnectException
import java.net.SocketTimeoutException
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
  // the number that went in, we can process and complete the future. This is the number of
  // API Keys we are asking for, and for most people it will be 1
  override fun complete(key: FeatureRequester) {
    completed.add(key)

    if (completed.size == requestCount) {
      // determine first if any of the environments failed its etag match
      val sendFullResults =
        !etags.validEtag || completed.find { !etags.environmentTags[it.key].equals(it.details?.etag) } != null
      future.complete(completed.map { req -> transformFeatures(req.details, req.key, sendFullResults, req.failure) }
        .toList())
    }
  }

  private fun decodeWebFailure(failure: WebApplicationException, key: KeyParts, env: FeatureEnvironmentCollection): FeatureRequestResponse? {
    if (failure.response == null || failure.response.status == 412) {
      return FeatureRequestResponse(env, FeatureRequestSuccess.DACHA_NOT_READY, key, "", null)
    } else if (failure.response.status == 404) {
      return FeatureRequestResponse(env, FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE, key, "", null)
    }

    return null
  }
  private fun transformFeatures(
    details: DachaKeyDetailsResponse?,
    key: KeyParts,
    sendFullResults: Boolean,
    failure: Exception?
  ): FeatureRequestResponse {
    val env = FeatureEnvironmentCollection().id(key.environmentId)

    if (failure != null) {
      if (failure is WebApplicationException) {
        val resp = decodeWebFailure(failure, key, env)
        if (resp != null) {
          return resp
        }
      } else if (failure is ProcessingException) {
        failure.cause?.let { cause ->
          if (cause is ConnectException || cause is SocketTimeoutException) {
            return FeatureRequestResponse(env, FeatureRequestSuccess.DACHA_NOT_READY, key, "", null)
          } else if (cause is WebApplicationException) {
            val resp = decodeWebFailure(cause, key, env)
            if (resp != null) {
              return resp
            }
          }
        }
      }
    }

    if (details == null) {
      return FeatureRequestResponse(env, FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE, key, "0", null)
    }

    if (!sendFullResults) {
      return FeatureRequestResponse(env, FeatureRequestSuccess.NO_CHANGE, key, "", details.environmentInfo)
    }

    return FeatureRequestResponse(
      env
        .features(featureTransformer.transform(details.features, clientContext)),
      FeatureRequestSuccess.SUCCESS,
      key,
      details.etag!!,
      details.environmentInfo
    )
  }
}
