package io.featurehub.events

import io.featurehub.events.local.LocalEventingFeature
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.utils.FallbackPropertyConfig
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
        bind(CloudEventPublisherRegistryProcessor::class.java).to(CloudEventPublisherRegistry::class.java).`in`(Singleton::class.java)
        bind(CloudEventDynamicPublisherRegistryImpl::class.java).to(CloudEventDynamicPublisherRegistry::class.java).`in`(Singleton::class.java)
        bind(CloudEventConfigDiscoveryService::class.java).to(CloudEventConfigDiscovery::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.starter(WebDynamicPublisher::class.java, context)

    // discover eventing (if any)
    findEventingLayer(context)

    return true
  }

  /**
   * This determines if we have an eventing layer, finding each of the eventing feature source classes and asking it
   * if it is configured. If none are, and local eventing is allowed, we use the in-process loopback.
   */
  private fun findEventingLayer(context: FeatureContext) {
    when (val platform = eventingPlatform()) {
      is EventingPlatform.External -> platform.sources.forEach { source ->
        source.featureSource?.let { context.register(it) }
      }
      EventingPlatform.Local -> context.register(LocalEventingFeature::class.java)
      EventingPlatform.None -> {}
    }
  }

  sealed class EventingPlatform {
    class External(val sources: List<EventingFeatureSource>) : EventingPlatform()
    object Local : EventingPlatform()
    object None : EventingPlatform()
  }

  companion object {
    const val LOCAL_ENABLED_PROPERTY = "cloudevents.local.enabled"

    private fun externalSources(): List<EventingFeatureSource> =
      ServiceLoader.load(EventingFeatureSource::class.java).filter { it.enabled }

    /**
     * Local (in-process) eventing is only allowed if explicitly turned on, which only the party-server does. This
     * stops standalone mr, dacha2 and edge from falling into local mode and silently not talking to each other.
     */
    fun localEventingAllowed(): Boolean =
      FallbackPropertyConfig.getConfig(LOCAL_ENABLED_PROPERTY, "false").trim().lowercase() == "true"

    /**
     * An external platform always wins. Local is never combined with one, as that would double deliver.
     */
    fun eventingPlatform(): EventingPlatform {
      val external = externalSources()

      return if (external.isNotEmpty()) {
        EventingPlatform.External(external)
      } else if (localEventingAllowed()) {
        EventingPlatform.Local
      } else {
        EventingPlatform.None
      }
    }

    /**
     * true if cloud events will be published somewhere, either through an external platform or in-process.
     */
    fun publishingEnabled(): Boolean = eventingPlatform() != EventingPlatform.None
  }
}
