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
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import jakarta.annotation.PostConstruct
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.core.Response
import org.glassfish.hk2.api.IterableProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

interface EdgeGetResponseWrapper {
  // this lets mutate the status as the response builder is not queryable
  fun wrapResponse(environments: List<FeatureRequestResponse>, builder: Response.ResponseBuilder, status: Int): Int
}

class FeatureGetProcessor @Inject constructor(
  private val getOrchestrator: DachaFeatureRequestSubmitter,
  private val sourceResponseWrapper: IterableProvider<EdgeGetResponseWrapper>,
  ) : FeatureGet {

  private val log: Logger = LoggerFactory.getLogger(FeatureGetProcessor::class.java)
  private val responseWrappers = mutableListOf <EdgeGetResponseWrapper>()

  @PostConstruct
  fun postConstruct() {
    responseWrappers.addAll(sourceResponseWrapper.toList())
    log.debug("there are {} post response wrappers for GET requests: {}", responseWrappers.size, responseWrappers.map { it.javaClass.name })
  }

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
      response.resume(wrapResponse(environments, Response.status(304).header("etag", etagHeader), 304).build())
    } else if (environments.all { it.success == FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE }) {
      response.resume(wrapResponse(environments, Response.status(404), 404).build())
    } else if (environments.all { it.success == FeatureRequestSuccess.DACHA_NOT_READY }) {
      // all the SDKs fail on a 400 level error and just stop.
      response.resume(Response.status(503).entity("cache layer not ready, try again shortly").build())
    } else {
      val newEtags = makeEtags(
        etags,
        environments.map(FeatureRequestResponse::etag))

      response.resume(
        wrapResponse(
          environments,
          Response.status(200)
            .header("etag", "\"${newEtags}\"")
            .entity(environments.map(FeatureRequestResponse::environment)), 200
        )
          .build()
      )
    }
  }


  private fun wrapResponse(environments: List<FeatureRequestResponse>, builder: Response.ResponseBuilder, httpStatus: Int): Response.ResponseBuilder {
    var status = httpStatus

    responseWrappers.forEach { wrapper -> status = wrapper.wrapResponse(environments, builder, status) }

    return builder
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
