package io.featurehub.edge.utils

import io.featurehub.edge.features.FeatureRequestResponse
import io.featurehub.edge.rest.EdgeGetResponseWrapper
import io.featurehub.utils.FeatureHubConfig
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response

class CacheControlResponseWrapper @Inject constructor(@FeatureHubConfig("edge.cache-control.header")
                                                        private val cacheControlHeader: String?,
) : EdgeGetResponseWrapper {
  override fun wrapResponse(
    environments: List<FeatureRequestResponse>,
    builder: Response.ResponseBuilder,
    status: Int
  ): Int {
    var bld = builder

    // check if the Ops team has set a header for this environment, then if they have set a generic one, and then
    // if the team themselves have set one in the database
    val managementCacheControl = environments.find { it.envInfo?.containsKey("mgmt.cacheControl") == true }

    if (managementCacheControl != null ) {
      bld = bld.header("Cache-Control", managementCacheControl.envInfo!!["mgmt.cacheControl"])
    } else if (cacheControlHeader?.isNotEmpty() == true) {
      bld = bld.header("Cache-Control", cacheControlHeader)
    } else {
      val environmentCacheControlHeader = environments.find { it.envInfo?.containsKey("cacheControl") == true }
      if (environmentCacheControlHeader != null) {
        bld = bld.header("Cache-Control", environmentCacheControlHeader.envInfo!!["cacheControl"])
      }
    }

    return status
  }
}
