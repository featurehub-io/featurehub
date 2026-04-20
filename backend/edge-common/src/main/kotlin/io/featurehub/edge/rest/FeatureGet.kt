package io.featurehub.edge.rest

import io.featurehub.edge.KeyParts
import io.featurehub.edge.features.DachaFeatureRequestSubmitter
import io.featurehub.edge.features.ETagSplitter.Companion.makeEtags
import io.featurehub.edge.features.ETagSplitter.Companion.splitTag
import io.featurehub.edge.features.EtagStructureHolder
import io.featurehub.edge.features.FeatureRequestResponse
import io.featurehub.edge.features.FeatureRequestSuccess
import io.featurehub.edge.stats.StatRecorder
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.metrics.MetricsCollector
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
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

interface FeatureGet {
  fun processGet(
    response: AsyncResponse,
    sdkUrls: List<String>?,
    apiKeys: List<String>?,
    featureHubAttrs: List<String>?,
    etagHeader: String?
  )
}

interface EdgeGetResponseWrapper {
  // this lets mutate the status as the response builder is not queryable
  fun wrapResponse(environments: List<FeatureRequestResponse>, builder: Response.ResponseBuilder, status: Int): Int
}

abstract class BaseFeatureGetProcessor @Inject constructor(
  protected val getOrchestrator: DachaFeatureRequestSubmitter,
  protected val sourceResponseWrapper: IterableProvider<EdgeGetResponseWrapper>,
  protected val statRecorder: StatRecorder
) {
  protected val responseWrappers = mutableListOf <EdgeGetResponseWrapper>()
  private val log: Logger = LoggerFactory.getLogger(BaseFeatureGetProcessor::class.java)

  @PostConstruct
  fun postConstruct() {
    responseWrappers.addAll(sourceResponseWrapper.toList())
    log.debug("there are {} post response wrappers for GET requests: {}", responseWrappers.size, responseWrappers.map { it.javaClass.name })
  }

  fun recordStatResponse(environments: List<FeatureRequestResponse>) {
    // record the result
    environments.forEach { resp: FeatureRequestResponse ->
      statRecorder.recordHit(resp.key, mapSuccess(resp.success), EdgeHitSourceType.POLL)
    }
  }


  fun createResponse(response: AsyncResponse, environments: List<FeatureRequestResponse>, etagHeader: String?, etags: EtagStructureHolder) {
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
            .entity(buildResponseObject(environments)), 200
        )
          .build()
      )
    }
  }

  abstract fun buildResponseObject(environments: List<FeatureRequestResponse>): Any

  fun wrapResponse(environments: List<FeatureRequestResponse>, builder: Response.ResponseBuilder, httpStatus: Int): Response.ResponseBuilder {
    var status = httpStatus

    responseWrappers.forEach { wrapper -> status = wrapper.wrapResponse(environments, builder, status) }

    return builder
  }

  companion object {
    val inout = MetricsCollector.gauge("edge_get_req", "how many GET requests")

    fun mapSuccess(success: FeatureRequestSuccess): EdgeHitResultType {
      return when (success) {
        FeatureRequestSuccess.NO_SUCH_KEY_IN_CACHE -> EdgeHitResultType.MISSED
        FeatureRequestSuccess.SUCCESS -> EdgeHitResultType.SUCCESS
        FeatureRequestSuccess.NO_CHANGE -> EdgeHitResultType.NO_CHANGE
        FeatureRequestSuccess.DACHA_NOT_READY -> EdgeHitResultType.MISSED
      }
    }

    val pollSpeedHistogram = MetricsCollector.histogram(
      "edge_conn_length_poll", "The length of time that the connection is open for Polling clients"
    )
  }
}

class FeatureGetProcessor @Inject constructor(
  getOrchestrator: DachaFeatureRequestSubmitter,
  sourceResponseWrapper: IterableProvider<EdgeGetResponseWrapper>,
  statRecorder: StatRecorder
) : BaseFeatureGetProcessor(getOrchestrator, sourceResponseWrapper, statRecorder), FeatureGet {

  override fun buildResponseObject(environments: List<FeatureRequestResponse>): Any {
    return environments.map(FeatureRequestResponse::environment)
  }

  override fun processGet(
    response: AsyncResponse,
    sdkUrls: List<String>?,
    apiKeys: List<String>?,
    featureHubAttrs: List<String>?,
    etagHeader: String?
  ) {
    if (sdkUrls.isNullOrEmpty() && apiKeys.isNullOrEmpty()) {
      response.resume(BadRequestException())
      return
    }

    inout.inc()

    val realApiKeys = (if (sdkUrls.isNullOrEmpty()) apiKeys else sdkUrls)!!
      .distinct().mapNotNull { KeyParts.fromString(it) }

    if (realApiKeys.isEmpty()) {
      response.resume(NotFoundException())
      inout.dec()
      return
    }

    val timer: Histogram.Timer = pollSpeedHistogram.startTimer()

    try {
      val clientContext = ClientContext.decode(featureHubAttrs, realApiKeys)
      val etags = splitTag(etagHeader, realApiKeys, clientContext.makeEtag())

      val environments = getOrchestrator.request(realApiKeys, clientContext, etags)

      recordStatResponse(environments)

      createResponse(response, environments, etagHeader, etags)
    } finally {
      timer.observeDuration()

      inout.dec()
    }
  }




}
