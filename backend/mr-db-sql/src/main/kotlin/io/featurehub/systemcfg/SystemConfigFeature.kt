package io.featurehub.systemcfg

import io.featurehub.db.api.SystemConfigApi
import io.featurehub.db.services.InternalSystemConfigApi
import io.featurehub.db.services.SystemConfigSqlApi
import io.featurehub.lifecycle.LifecycleListeners
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class SystemConfigFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(SlackConfig::class.java).to(KnownSystemConfigSource::class.java).to(SlackConfigApi::class.java).`in`(Singleton::class.java)
        bind(SystemConfigSqlApi::class.java).to(SystemConfigApi::class.java).to(InternalSystemConfigApi::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.starter(SlackLifecycleListener::class.java, context)

    return true
  }
}
