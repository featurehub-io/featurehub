package io.featurehub.events

import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class CloudEventsFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(CloudEventReceiverRegistryProcessor::class.java).to(CloudEventReceiverRegistry::class.java).`in`(Singleton::class.java)
        bind(CloudEventsTelemetryWriterImpl::class.java).to(CloudEventsTelemetryWriter::class.java).`in`(Singleton::class.java)
        bind(CloudEventsTelemetryReaderImpl::class.java).to(CloudEventsTelemetryReader::class.java).`in`(Singleton::class.java)
      }
    })
    return true
  }
}
