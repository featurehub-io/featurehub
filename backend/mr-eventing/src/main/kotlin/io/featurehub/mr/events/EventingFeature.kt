package io.featurehub.mr.events

import io.featurehub.db.publish.DbCacheSource
import io.featurehub.db.publish.DummyPublisher
import io.featurehub.db.publish.nats.NatsDachaEventingFeature
import io.featurehub.events.CloudEventsFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.common.CloudEventCacheBroadcaster
import io.featurehub.mr.events.common.CloudEventsCommonFeature
import io.featurehub.mr.events.common.listeners.FeatureUpdateListener
import io.featurehub.mr.events.dacha2.CacheApi
import io.featurehub.mr.events.dacha2.kinesis.KinesisMRFeature
import io.featurehub.mr.events.dacha2.pubsub.PubsubMRFeature
import io.featurehub.mr.events.service.FeatureUpdateListenerImpl
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class EventingFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    var amPublishing = GoogleEventFeature.isEnabled() || NatsDachaEventingFeature.isEnabled()

    if (amPublishing) {
      context.register(CloudEventsCommonFeature::class.java)
    }

    context.register(CloudEventsFeature::class.java)
    context.register(PubsubMRFeature::class.java) // this will in fact not register anything if it is not enabled
    context.register(KinesisMRFeature::class.java)

    if (NatsDachaEventingFeature.isEnabled()) {
      context.register(NatsDachaEventingFeature::class.java)
    }

    context.register(object: AbstractBinder() {
      override fun configure() {
        if (amPublishing) {
          // the broadcaster will determine if dacha2 is enabled and not publish to that channel if not
          bind(CloudEventCacheBroadcaster::class.java).to(CacheBroadcast::class.java).`in`(Singleton::class.java)

          bind(FeatureUpdateListenerImpl::class.java).to(FeatureUpdateListener::class.java).`in`(Singleton::class.java)
          bind(DbCacheSource::class.java).to(CacheSource::class.java).to(CacheApi::class.java)
            .`in`(
              Singleton::class.java
            )
        } else {
          bind(DummyPublisher::class.java).to(CacheSource::class.java).`in`(
            Singleton::class.java
          )
          bind(DbCacheSource::class.java).to(CacheApi::class.java).`in`(Singleton::class.java)
        }
      }
    })

    return true
  }
}
