package io.featurehub.publish

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.health.HealthSource
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NATSFeature : Feature {
  private val log: Logger = LoggerFactory.getLogger(NATSFeature::class.java)
  var natsUrls: String? = null

  init {
    natsUrls = FallbackPropertyConfig.getConfig("nats.urls")

    if (natsUrls == null) {
      log.info("No NATS configuration detected, broadcast disabled")
    }
  }

  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(NATSConnectionSource::class.java).to(NATSSource::class.java).`in`(Singleton::class.java)

        if (natsUrls != null) {
          bind(NATSHealthSource::class.java).to(HealthSource::class.java).`in`(Singleton::class.java)
        }
      }

    })

    return true
  }
}
