package io.featurehub.dacha.caching

import io.featurehub.dacha.model.*
import io.featurehub.dacha2.Dacha2CacheListener
import io.featurehub.utils.FallbackPropertyConfig
import io.featurehub.utils.FeatureHubConfig
import jakarta.inject.Inject
import jakarta.ws.rs.client.Client
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class FastlyPublisher @Inject constructor(@FeatureHubConfig("edge.cache.fastly.key")
                                    private val fastlyKey: String?,
                                          @FeatureHubConfig("edge.cache.fastly.service-id")
                                    private val fastlyServiceId: String?,
                                          private val client: Client

) : Dacha2CacheListener {
  private val log: Logger = LoggerFactory.getLogger(FastlyPublisher::class.java)

  override fun updateServiceAccount(sa: PublishServiceAccount) {
  }

  override fun updateEnvironment(e: PublishEnvironment) {
    // for new or updated environments, we don't worry about, as they will take care of filling the Fastly cache
    // later. Environments that have features updated will have their features coming through updateFeatureValue
    // below. We only purge for a DELETE
    if (e.action == PublishAction.DELETE) {
      purge(e.environment.id)
    }
  }

  override fun updateFeature(feature: PublishFeatureValue) {
    purge(feature.environmentId)
  }

  private fun purge(e: UUID) {
    if (fastlyServiceId != null && fastlyKey != null) {
      try {
        client.target("https://api.fastly.com/service/${fastlyServiceId}/purge/${e}")
          .request()
          .header("Fastly-Key", fastlyKey)
          .post(null)
      } catch (e: Exception) {
        log.error("Unable to break cache for Fastly for environment ${e}")
      }
    }
  }

  companion object {
    fun fastlyEnabled(): Boolean {
      return FallbackPropertyConfig.getConfig("edge.cache.fastly.key") != null && FallbackPropertyConfig.getConfig("edge.cache.fastly.service-id") != null
    }
  }
}
