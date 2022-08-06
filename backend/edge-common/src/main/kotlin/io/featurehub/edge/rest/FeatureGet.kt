package io.featurehub.edge.rest

import io.featurehub.edge.KeyParts
import io.featurehub.edge.features.DachaFeatureRequestSubmitter
import io.featurehub.edge.features.ETagSplitter.Companion.makeEtags
import io.featurehub.edge.features.ETagSplitter.Companion.splitTag
import io.featurehub.edge.features.FeatureRequestResponse
import io.featurehub.edge.features.FeatureRequestSuccess
import io.featurehub.edge.stats.StatRecorder
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import io.featurehub.utils.FeatureHubConfig
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.core.Response
import java.util.function.Consumer

interface FeatureGet {
  fun processGet(
    response: AsyncResponse,
    sdkUrls: List<String>?,
    apiKeys: List<String>?,
    featureHubAttrs: List<String>?,
    etagHeader: String?,
    statRecorder: StatRecorder?
  )
}

class FeatureGetProcessor @Inject constructor(
  private val getOrchestrator: DachaFeatureRequestSubmitter,
  @FeatureHubConfig("edge.cache-control.header")
  private val cacheControlHeader: String?,
  @FeatureHubConfig("edge.cache.fastly.key")
  private val fastlyAuth: String?
  ) : FeatureGet {

  override fun processGet(
    response: AsyncResponse,
    sdkUrls: List<String>?,
    apiKeys: List<String>?,
    featureHubAttrs: List<String>?,
    etagHeader: String?,
    statRecorder: StatRecorder?
  ) {
    if ((sdkUrls == null || sdkUrls.isEmpty()) && (apiKeys == null || apiKeys.isEmpty())) {
      response.resume(BadRequestException())
      return
    }

    inout.inc()

    val timer: Histogram.Timer = pollSpeedHistogram.startTimer()

    val realApiKeys = (if (sdkUrls == null || sdkUrls.isEmpty()) apiKeys else sdkUrls)!!
      .asSequence()
      .distinct() // we want unique ones
      .map { KeyParts.fromString(it) }
      .filterNotNull()
      .toList()

    if (realApiKeys.isEmpty()) {
      response.resume(NotFoundException())
      return
    }

    val clientContext = ClientContext.decode(featureHubAttrs, realApiKeys)
    val etags = splitTag(etagHeader, realApiKeys, clientContext.makeEtag())

    val environments = getOrchestrator.request(realApiKeys, clientContext, etags)

    if (statRecorder != null) {
      // record the result
      environments.forEach(Consumer { resp: FeatureRequestResponse ->
        with(statRecorder) {
          recordHit(
            resp.key, mapSuccess(resp.success),
            EdgeHitSourceType.POLL
          )
        }
      })
    }

    timer.observeDuration()

    inout.dec()

    if (environments[0].success === FeatureRequestSuccess.NO_CHANGE && environments.size == 1) {
      response.resume(wrapCacheControl(environments, Response.status(304).header("etag", etagHeader)).build())
    } else if (environments.all { it.success == FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE }) {
      response.resume(wrapCacheControl(environments, Response.status(404)).build())
    } else if (environments.all { it.success == FeatureRequestSuccess.DACHA_NOT_READY }) {
      // all the SDKs fail on a 400 level error and just stop.
      response.resume(Response.status(503).entity("cache layer not ready, try again shortly").build())
    } else {
      val newEtags = makeEtags(
        etags,
        environments.map(FeatureRequestResponse::etag))

      response.resume(
        wrapCacheControl(
          environments,
          Response.status(200)
            .header("etag", "\"${newEtags}\"")
            .entity(environments.map(FeatureRequestResponse::environment))
        )
          .build()
      )
    }
  }

  private fun wrapCacheControl(environments: List<FeatureRequestResponse>, builder: Response.ResponseBuilder): Response.ResponseBuilder {
    var bld = builder;
    if (cacheControlHeader?.isNotEmpty() == true) {
      bld = bld.header("Cache-Control", cacheControlHeader)
    }

    /**
     * We set the surrogate key to include the environments that have been returned. This means that Dacha can send a request to
     * break any of these caches so the next time client requests it will come to us instead of the cache. The cache cannot be used without
     * a streaming platform. This will work regardless of apikey, all environments using client evaluated or server evaluated keys will have
     * their cache broken on the next poll.
     *
     * Clients using server evaluation needs to be upgraded to ensure that the SHA of their context data is included as
     * a parameter. This will mean they can break their own cache if they change their context data.
     */
    if (fastlyAuth != null && environments.isNotEmpty()) {
      bld.header("Surrogate-Key", environments.map { it.environment.id.toString() }.joinToString(" "))
    }

    return bld
  }

  private fun mapSuccess(success: FeatureRequestSuccess): EdgeHitResultType {
    return when (success) {
      FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE -> EdgeHitResultType.MISSED
      FeatureRequestSuccess.SUCCESS -> EdgeHitResultType.SUCCESS
      FeatureRequestSuccess.NO_CHANGE -> EdgeHitResultType.NO_CHANGE
      FeatureRequestSuccess.DACHA_NOT_READY -> EdgeHitResultType.MISSED
    }
  }

  companion object {
    val inout = Gauge.build("edge_get_req", "how many GET requests").register()
    val pollSpeedHistogram = Histogram.build(
      "edge_conn_length_poll", "The length of " +
        "time that the connection is open for Polling clients"
    ).register()
  }

}
