package io.featurehub.edge.utils

import io.featurehub.edge.features.FeatureRequestResponse
import io.featurehub.edge.rest.EdgeGetResponseWrapper
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.ws.rs.core.Response


// this class is only bound if fastly is configured
class FastlyResponseWrapper : EdgeGetResponseWrapper {
  private val fastlyAuth: String

  init {
    fastlyAuth = fastlyKey()!!
  }

  override fun wrapResponse(
    environments: List<FeatureRequestResponse>,
    builder: Response.ResponseBuilder,
    status: Int
  ): Int {
    /**
     * We set the surrogate key to include the environments that have been returned. This means that Dacha can send a request to
     * break any of these caches so the next time client requests it will come to us instead of the cache. The cache cannot be used without
     * a streaming platform. This will work regardless of apikey, all environments using client evaluated or server evaluated keys will have
     * their cache broken on the next poll.
     *
     * Clients using server evaluation needs to be upgraded to ensure that the SHA of their context data is included as
     * a parameter. This will mean they can break their own cache if they change their context data.
     */
    if (environments.isNotEmpty()) {
      builder.header("Surrogate-Key", environments.map { it.environment.id.toString() }.joinToString(" "))
    }

    return status
  }

  companion object {
    fun fastlyKey(): String? = FallbackPropertyConfig.getConfig("edge.cache.fastly.key")
    fun fastlyConfigured(): Boolean =
      fastlyKey() != null
  }
}
