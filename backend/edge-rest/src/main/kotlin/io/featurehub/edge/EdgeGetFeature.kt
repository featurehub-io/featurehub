package io.featurehub.edge

import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.db.publish.CacheSourceFeatureGroupSqlApi
import io.featurehub.db.publish.FeatureModelWalker
import io.featurehub.db.publish.FeatureModelWalkerService
import io.featurehub.edge.db.sql.DbDachaCacheSqlApi
import io.featurehub.edge.db.sql.DbFeatureUpdateProcessor
import io.featurehub.edge.resources.EdgeResource
import io.featurehub.edge.rest.FeatureUpdatePublisher
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
        bind(DbDachaCacheSqlApi::class.java).to(DachaClientServiceRegistry::class.java).`in`(Singleton::class.java)
        bind(FeatureModelWalkerService::class.java).to(FeatureModelWalker::class.java).`in`(Singleton::class.java)
        bind(DbFeatureUpdateProcessor::class.java).to(FeatureUpdatePublisher::class.java).`in`(Singleton::class.java)
        bind(CacheSourceFeatureGroupSqlApi::class.java).to(CacheSourceFeatureGroupApi::class.java).`in`(Singleton::class.java)
      }
    })

    return true
  }
}
