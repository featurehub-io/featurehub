package io.featurehub.dacha.caching

import io.featurehub.dacha.CacheUpdateListener
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.utils.FallbackPropertyConfig
import io.featurehub.utils.FeatureHubConfig
import jakarta.inject.Inject
import jakarta.ws.rs.client.Client
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class FastlyPublisher @Inject constructor(@FeatureHubConfig("edge.cache.fastly.key")
                                    private val fastlyKey: String?,
                                          @FeatureHubConfig("edge.cache.fastly.service-id")
                                    private val fastlyServiceId: String?,
                                          private val client: Client

) : CacheUpdateListener {
  private val log: Logger = LoggerFactory.getLogger(FastlyPublisher::class.java)


  override fun updateServiceAccount(sa: PublishServiceAccount) {
  }

  override fun updateEnvironment(e: PublishEnvironment) {
    // new environments have nothing cached in Fastly yet, so they don't need a purge. Updated environments are purged
    // because their metadata (or other environment-level data) may have changed what is served, and deleted
    // environments are purged so they stop being served. Feature value changes are purged separately below.
    if (e.action == PublishAction.DELETE || e.action == PublishAction.UPDATE) {
      purge(e.environment.id)
    }
  }

  private fun purge(environmentId: UUID) {
    if (fastlyServiceId != null && fastlyKey != null) {
      try {
        client.target("https://api.fastly.com/service/${fastlyServiceId}/purge/${environmentId}")
          .request()
          .header("Fastly-Key", fastlyKey)
          .post(null)
          .use { response ->
            if (response.status !in 200..299) {
              log.warn("Fastly refused cache purge for environment {}: HTTP {}", environmentId, response.status)
            }
          }
      } catch (ex: Exception) {
        log.error("Unable to break cache for Fastly for environment {}", environmentId, ex)
      }
    }
  }

  override fun updateFeatureValue(fv: PublishFeatureValue) {
    purge(fv.environmentId)
  }

  companion object {
    fun fastlyEnabled(): Boolean {
      return FallbackPropertyConfig.getConfig("edge.cache.fastly.key") != null && FallbackPropertyConfig.getConfig("edge.cache.fastly.service-id") != null
    }
  }
}
