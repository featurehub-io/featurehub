package io.featurehub.edge

import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.db.api.CacheRefresherApi
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.db.publish.CacheSourceFeatureGroupSqlApi
import io.featurehub.db.publish.FeatureModelWalker
import io.featurehub.db.publish.FeatureModelWalkerService
import io.featurehub.db.services.Conversions
import io.featurehub.db.services.ConvertUtils
import io.featurehub.edge.db.sql.DbDachaCacheSqlApi
import io.featurehub.edge.db.sql.DbFeatureUpdatePublisher
import io.featurehub.edge.db.sql.DummyFeatureMessagingPublisher
import io.featurehub.edge.resources.EdgeResource
import io.featurehub.edge.rest.FeatureUpdatePublisher
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.common.DummyPublisher
import io.featurehub.mr.events.common.listeners.FeatureUpdateListener
import io.featurehub.mr.events.common.listeners.FoundationFeatureUpdateListenerImpl
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class EdgeGetFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(EdgeCommonFeature::class.java)
    context.register(EdgeResource::class.java)
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(ConvertUtils::class.java).to(Conversions::class.java).`in`(
          Singleton::class.java
        )
        bind(DbDachaCacheSqlApi::class.java).to(DachaClientServiceRegistry::class.java).`in`(Singleton::class.java)
        bind(FeatureModelWalkerService::class.java).to(FeatureModelWalker::class.java).`in`(Singleton::class.java)
        bind(CacheSourceFeatureGroupSqlApi::class.java).to(CacheSourceFeatureGroupApi::class.java).`in`(Singleton::class.java)
        bind(FoundationFeatureUpdateListenerImpl::class.java).to(FeatureUpdateListener::class.java).`in`(Singleton::class.java)
        bind(DbFeatureUpdatePublisher::class.java).to(FeatureUpdatePublisher::class.java).`in`(Singleton::class.java)
        bind(DummyPublisher::class.java).to(CacheRefresherApi::class.java).to(CacheSource::class.java).`in`(
          Singleton::class.java
        )
        bind(DummyFeatureMessagingPublisher::class.java).to(FeatureMessagingPublisher::class.java).`in`(
          Singleton::class.java
        )
      }
    })

    return true
  }
}
