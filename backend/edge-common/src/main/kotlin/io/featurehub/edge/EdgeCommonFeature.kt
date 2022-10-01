package io.featurehub.edge

import io.featurehub.edge.features.DachaFeatureRequestSubmitter
import io.featurehub.edge.features.DachaRequestOrchestrator
import io.featurehub.edge.features.EdgeConcurrentRequestPool
import io.featurehub.edge.rest.*
import io.featurehub.edge.utils.*
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class EdgeCommonFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (FastlyResponseWrapper.fastlyConfigured()) {
      context.register(FastlySSEContainerResponseFilter::class.java)
    }

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(FeatureGetProcessor::class.java).to(FeatureGet::class.java).`in`(Singleton::class.java)
        bind(FeatureUpdateProcessor::class.java).to(FeatureUpdate::class.java).`in`(Singleton::class.java)
        bind(FeatureTransformerUtils::class.java)
          .to(FeatureTransformer::class.java)
          .`in`(Singleton::class.java)
        bind(ConcurrentRequestPool::class.java)
          .to(EdgeConcurrentRequestPool::class.java)
          .`in`(Singleton::class.java)
        bind(DachaRequestOrchestrator::class.java)
          .to(DachaFeatureRequestSubmitter::class.java)
          .`in`(Singleton::class.java)
        bind(UpdateFeatureMapper::class.java)
          .to(UpdateMapper::class.java)
          .`in`(Singleton::class.java)
        bind(CacheControlResponseWrapper::class.java)
          .to(EdgeGetResponseWrapper::class.java)
          .`in`(Singleton::class.java)
        if (FastlyResponseWrapper.fastlyConfigured()) {
          bind(FastlyResponseWrapper::class.java)
            .to(EdgeGetResponseWrapper::class.java)
            .`in`(Singleton::class.java)
        }
      }
    })

    return true
  }
}
