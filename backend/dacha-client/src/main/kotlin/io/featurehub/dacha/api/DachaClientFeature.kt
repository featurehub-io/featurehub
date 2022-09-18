package io.featurehub.dacha.api

import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.publish.NATSFeature
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class DachaClientFeature : Feature {
  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun configure(featureContext: FeatureContext): Boolean {
    featureContext.register(object : AbstractBinder() {
      override fun configure() {
        if (NATSFeature.isNatsConfigured()) {
          bind(NatsBackupDachaFactory::class.java).to(BackupDachaFactory::class.java).`in`(Singleton::class.java)
        } else {
          bind(NoNatsBackupDachaFactory::class.java).to(BackupDachaFactory::class.java).`in`(Singleton::class.java)
        }

        bind(DachaClientRegistry::class.java).to(DachaClientServiceRegistry::class.java).`in`(
          Singleton::class.java
        )
      }
    })
    return true
  }
}
