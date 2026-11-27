package io.featurehub.events.local

import io.featurehub.events.DefaultEventingConnection
import io.featurehub.events.EventingConnection
import io.featurehub.lifecycle.LifecycleListeners
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

/**
 * Registers the in-process eventing platform. It is never combined with an external platform, otherwise
 * events would be delivered twice (once in-process, once through the stream).
 */
class LocalEventingFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object : AbstractBinder() {
      override fun configure() {
        // always connected, so Dacha2's dump-on-reconnect cache never dumps
        bind(DefaultEventingConnection::class.java).to(EventingConnection::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.wrap(LocalConfiguredSource::class.java, context)

    return true
  }
}
