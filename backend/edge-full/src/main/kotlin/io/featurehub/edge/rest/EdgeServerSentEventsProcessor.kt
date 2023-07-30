package io.featurehub.edge.rest

import io.featurehub.edge.KeyParts
import io.featurehub.edge.StreamingFeatureController
import io.featurehub.edge.bucket.BucketService
import io.featurehub.edge.client.TimedBucketClientFactory
import io.featurehub.edge.model.SSEResponse
import io.featurehub.edge.stats.StatRecorder
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import jakarta.inject.Inject
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.core.MediaType
import org.glassfish.jersey.media.sse.EventOutput
import org.glassfish.jersey.media.sse.OutboundEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface EdgeServerSentEventsProcessor {
  fun process(apiKeys: List<String>?, queryParameters: Map<String, List<String>>, etag: String?, extraConfig: String?): EventOutput
}

class EdgeServerSentEventsProcess @Inject constructor(private val bucketService: BucketService, private val serverConfig: StreamingFeatureController,
                                                      private val statRecorder: StatRecorder, private val timedBucketFactory: TimedBucketClientFactory
) : EdgeServerSentEventsProcessor {
  private val log: Logger = LoggerFactory.getLogger(EdgeServerSentEventsProcess::class.java)

  override fun process(
    apiKeys: List<String>?,
    queryParameters: Map<String, List<String>>,
    etag: String?,
    extraConfig: String?
  ): EventOutput {
    val outputStream = EventOutput()

    if (apiKeys.isNullOrEmpty() || apiKeys.size > 1) {
      outputStream.write(OutboundEvent.Builder().name(SSEResponse.ERROR.value)
        .mediaType(MediaType.APPLICATION_JSON_TYPE).data("{\"error\":601, \"desc\": \"invalid api keys\"").build())
      return outputStream
    }

    val apiKey = KeyParts.fromString(apiKeys[0])

    if (apiKey == null) {
      outputStream.write(OutboundEvent.Builder().name(SSEResponse.ERROR.value)
        .mediaType(MediaType.APPLICATION_JSON_TYPE).data("{\"error\":602, \"desc\": \"invalid api key format\"").build())
      return outputStream
    }

    try {
      val bucket = timedBucketFactory.createBucket(
        outputStream, apiKey, ClientContext.decode(queryParameters, listOf(apiKey)),
        etag, extraContext,
      )
      if (bucket.discovery()) {
        serverConfig.requestFeatures(bucket)
        bucketService.putInBucket(bucket)
      } else {
        statRecorder.recordHit(apiKey, EdgeHitResultType.FAILED_TO_WRITE_ON_INIT, EdgeHitSourceType.EVENTSOURCE)
      }
    } catch (e: Exception) {
      statRecorder.recordHit(apiKey, EdgeHitResultType.FAILED_TO_PROCESS_REQUEST, EdgeHitSourceType.EVENTSOURCE)
      log.error("failed to write feature states")
      throw InternalServerErrorException(e)
    }

    return outputStream

  }

}
