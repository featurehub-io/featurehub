package io.featurehub.edge.client

import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.bucket.BucketService
import io.featurehub.edge.stats.StatRecorder
import jakarta.inject.Inject
import org.glassfish.jersey.media.sse.EventOutput

interface TimedBucketClientFactory {
  fun createBucket(output: EventOutput, apiKey: KeyParts,
                   featureHubAttributes: List<String>?,
                   etag: String?,
                   extraContext: String?): TimedBucketClientConnection
}

class TimedBucketClientFactoryImpl @Inject constructor(
  private val featureTransformer: FeatureTransformer, private val bucketService: BucketService, private val statRecorder: StatRecorder
) : TimedBucketClientFactory {
  override fun createBucket(
    output: EventOutput,
    apiKey: KeyParts,
    featureHubAttributes: List<String>?,
    etag: String?,
    extraContext: String?
  ): TimedBucketClientConnection =
    TimedBucketClientConnection(InternalEventOutput(output), apiKey, featureTransformer,
      statRecorder, featureHubAttributes, etag, extraContext, bucketService)
}
