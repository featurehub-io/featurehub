package io.featurehub.edge.rest

import io.featurehub.edge.KeyParts
import io.featurehub.edge.StreamingFeatureController
import io.featurehub.edge.bucket.BucketService
import io.featurehub.edge.client.TimedBucketClientFactory
import io.featurehub.edge.stats.StatRecorder
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import jakarta.inject.Inject
import jakarta.ws.rs.InternalServerErrorException
import org.glassfish.jersey.media.sse.EventOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

interface FeatureSse {
  fun process(
    namedCache: String,
    envId: UUID,
    apiKey: String,
    featureHubAttrs: List<String>?,
    browserHubAttrs: String?,
    etag: String?,
    extraContext: String?
  ): EventOutput
}

class FeatureSseProcessor @Inject constructor(
  private val bucketService: BucketService, private val serverConfig: StreamingFeatureController,
  private val statRecorder: StatRecorder, private val timedBucketFactory: TimedBucketClientFactory
) : FeatureSse {
  private val log: Logger = LoggerFactory.getLogger(FeatureSseProcessor::class.java)

  override fun process(
    namedCache: String,
    envId: UUID,
    apiKey: String,
    featureHubAttrs: List<String>?,
    browserHubAttrs: String?,
    etag: String?,
    extraContext: String?
  ): EventOutput {
    val outputStream = EventOutput()

    val apiKey = KeyParts(namedCache, envId, apiKey)

    try {
      val bucket = timedBucketFactory.createBucket(
        outputStream, apiKey, browserHubAttrs?.let { listOf(it) } ?: featureHubAttrs,
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
