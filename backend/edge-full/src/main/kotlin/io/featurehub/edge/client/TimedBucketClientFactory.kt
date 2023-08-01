package io.featurehub.edge.client

import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.edge.bucket.BucketService
import io.featurehub.edge.stats.StatRecorder
import io.featurehub.edge.strategies.ClientContext
import jakarta.inject.Inject
import org.glassfish.jersey.media.sse.EventOutput

enum class BucketProtocolVersion {
  V1, V2
}

interface TimedBucketClientFactory {
  fun createBucket(output: EventOutput,
                   apiKey: KeyParts,
                   context: ClientContext,
                   bucketProtocolVersion: BucketProtocolVersion,
                   etag: String?,
                   extraContext: String?): TimedBucketClientConnection
}

class TimedBucketClientFactoryImpl @Inject constructor(
  private val featureTransformer: FeatureTransformer, private val bucketService: BucketService, private val statRecorder: StatRecorder
) : TimedBucketClientFactory {
  override fun createBucket(
    output: EventOutput,
    apiKey: KeyParts,
    context: ClientContext,
    bucketProtocolVersion: BucketProtocolVersion,
    etag: String?,
    extraContext: String?
  ): TimedBucketClientConnection {
    return TimedBucketClientConnection(output, apiKey, featureTransformer, statRecorder, context, etag, extraContext, bucketService, bucketProtocolVersion)
  }
}
