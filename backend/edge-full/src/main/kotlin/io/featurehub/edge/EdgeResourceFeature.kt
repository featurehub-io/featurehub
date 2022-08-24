package io.featurehub.edge

import io.featurehub.edge.client.TimedBucketClientFactory
import io.featurehub.edge.client.TimedBucketClientFactoryImpl
import io.featurehub.edge.rest.EventStreamResource
import io.featurehub.edge.rest.SSEHeaderFilter
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class EdgeResourceFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context
      .register(EventStreamResource::class.java)
      .register(SSEHeaderFilter::class.java)

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(TimedBucketClientFactoryImpl::class.java).to(TimedBucketClientFactory::class.java).`in`(Singleton::class.java)
      }
    })

    return true
  }
}
