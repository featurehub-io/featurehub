package io.featurehub.events

import io.featurehub.lifecycle.LifecycleListeners
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import java.util.*

class CloudEventsFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(CloudEventReceiverRegistryProcessor::class.java).to(CloudEventReceiverRegistry::class.java).`in`(Singleton::class.java)
        bind(CloudEventsTelemetryWriterImpl::class.java).to(CloudEventsTelemetryWriter::class.java).`in`(Singleton::class.java)
        bind(CloudEventsTelemetryReaderImpl::class.java).to(CloudEventsTelemetryReader::class.java).`in`(Singleton::class.java)
        bind(CloudEventPublisherRegistry::class.java).to(CloudEventPublisher::class.java).`in`(Singleton::class.java)
        bind(CloudEventDynamicPublisherRegistryImpl::class.java).to(CloudEventDynamicPublisherRegistry::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.starter(WebDynamicPublisher::class.java, context)

    findEventingLayer(context)

    return true
  }

  private fun findEventingLayer(context: FeatureContext) {
    val sources = ServiceLoader.load(EventingFeatureSource::class.java)

    for(source in sources) {
      val featureSource = source.featureSource

      if (featureSource != null) {
        context.register(featureSource)
      }
    }
  }
}
