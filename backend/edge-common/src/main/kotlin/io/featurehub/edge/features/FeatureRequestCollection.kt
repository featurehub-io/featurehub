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
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

class FeatureRequestCollection @JvmOverloads constructor(
  private val requestCount: Int,
  private val featureTransformer: FeatureTransformer,
  private val clientContext: ClientContext,
  private val future: CompletableFuture<List<FeatureRequestResponse>>,
  private val etags: EtagStructureHolder,
  private val clock: Clock = Clock.systemUTC()
) : FeatureRequestCompleteNotifier {
  private val completed: MutableCollection<FeatureRequester> = ConcurrentLinkedQueue()

  // this gets called on each requester each time a response comes back. Once it matches
  // the number that went in, we can process and complete the future. This is the number of
  // API Keys we are asking for, and for most people it will be 1
  override fun complete(key: FeatureRequester) {
    completed.add(key)

    if (completed.size == requestCount) {
      // one instant for the whole request, taken before anything is evaluated: the etags' clock part and the
      // evaluated values (fhubWhenActive, "now") must agree, or a boundary passing between them would leave a client
      // with values from one side of it and an etag from the other
      val now = clock.instant()

      // determine first if any of the environments failed its etag match
      val effectiveEtags = completed.associate { it.key to effectiveEtag(it.details, now) }
      val sendFullResults =
        !etags.validEtag || completed.find { !etags.environmentTags[it.key].equals(effectiveEtags[it.key]) } != null
      future.complete(completed.map { req ->
        transformFeatures(req.details, req.key, sendFullResults, req.failure, effectiveEtags[req.key], now) }
        .toList())
    }
  }

  /**
   * Server evaluated results can change with Edge's clock (fhubWhenActive, "now") with no change to the feature data,
   * so for those the etag also reflects which side of each such moment the clock is on. Client evaluated keys are sent
   * the strategies and evaluate them against their own clock, so Dacha's etag is enough.
   */
  private fun effectiveEtag(details: DachaKeyDetailsResponse?, now: Instant): String? {
    val dachaEtag = details?.etag ?: return null

    return if (clientContext.isClientEvaluation) dachaEtag
      else TimeBoundaryEtag.etag(dachaEtag, details.features, now)
  }

  private fun decodeWebFailure(failure: WebApplicationException, key: KeyParts, env: FeatureEnvironmentCollection): FeatureRequestResponse? {
    if (failure.response == null || failure.response.status == 412) {
      return FeatureRequestResponse(env, FeatureRequestSuccess.DACHA_NOT_READY, key, "", null, false)
    } else if (failure.response.status == 404) {
      return FeatureRequestResponse(env, FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE, key, "", null, false)
    }

    return null
  }
  private fun transformFeatures(
    details: DachaKeyDetailsResponse?,
    key: KeyParts,
    sendFullResults: Boolean,
    failure: Exception?,
    etag: String?,
    now: Instant
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
            return FeatureRequestResponse(env, FeatureRequestSuccess.DACHA_NOT_READY, key, "", null, false)
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
      return FeatureRequestResponse(env, FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE, key, "0", null, false)
    }

    if (!sendFullResults) {
      return FeatureRequestResponse(env, FeatureRequestSuccess.NO_CHANGE, key, "", details.environmentInfo, false)
    }

    env.features(featureTransformer.transform(details.features, clientContext, details.extendedDataAllowed ?: false, now))

    // only present when the environment actually overrides the interval
    pollInterval(details.environmentInfo)?.let { env.pollInterval(it) }

    return FeatureRequestResponse(
      env,
      FeatureRequestSuccess.SUCCESS,
      key,
      etag ?: details.etag,
      details.environmentInfo,
      details.extendedDataAllowed ?: false
    )
  }

  companion object {
    const val POLL_INTERVAL = "pollInterval"
    const val MGMT_POLL_INTERVAL = "mgmt.$POLL_INTERVAL"
    /** SDKs never poll more often than this, so Edge never asks them to */
    const val MINIMUM_POLL_INTERVAL = 5

    /**
     * The environment's polling interval override in seconds - the ops (mgmt.) setting wins over the team's own.
     * A value that isn't a whole number is ignored, as if not set. If the chosen value is below the minimum
     * interval there is no override (null) - it does not fall back to the team's setting.
     */
    @JvmStatic
    fun pollInterval(environmentInfo: Map<String, String>?): Int? {
      if (environmentInfo == null) return null

      return listOf(MGMT_POLL_INTERVAL, POLL_INTERVAL)
        .firstNotNullOfOrNull { key -> environmentInfo[key]?.trim()?.toIntOrNull() }
        ?.takeIf { it >= MINIMUM_POLL_INTERVAL }
    }
  }
}
