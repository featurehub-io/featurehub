package io.featurehub.health

import cd.connect.jersey.common.LoggingConfiguration
import cd.connect.jersey.prometheus.PrometheusDynamicFeature
import cd.connect.openapi.support.ReturnStatusContainerResponseFilter
import io.featurehub.jersey.config.CommonConfiguration
import jakarta.inject.Inject
import org.glassfish.jersey.server.ServerProperties
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities

class CommonFeatureHubFeatures @Inject constructor(locator: ServiceLocator) : Feature {

  // ensure Immediate scope which is HK2 specific is enabled everywhere
  init {
    ServiceLocatorUtilities.enableImmediateScope(locator)
  }

  override fun configure(context: FeatureContext): Boolean {
    context.property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true)

    arrayOf(
      CommonConfiguration::class.java,
      LoggingConfiguration::class.java,
      ReturnStatusContainerResponseFilter::class.java,
      PrometheusDynamicFeature::class.java
    ).forEach { context.register(it) }

    return true
  }
}
