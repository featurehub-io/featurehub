package io.featurehub.edge.utils

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.core.MediaType.SERVER_SENT_EVENTS_TYPE

class FastlySSEContainerResponseFilter : ContainerResponseFilter {
  @ConfigKey("edge.sse.drop-after-seconds")
  var sseTimeout: Int? = 59
//  @ConfigKey("edge.cache.prefix")
//  var prefix: String? = "features"

  val cacheControlHeader: String

  init {
    DeclaredConfigResolver.resolve(this)

    cacheControlHeader = "public, max-age=${sseTimeout!!-1}"
  }

  override fun filter(requestContext: ContainerRequestContext, response: ContainerResponseContext) {
    if ((response.status == 200 || response.status == 236) && requestContext.method == "GET" &&
      response.mediaType == SERVER_SENT_EVENTS_TYPE) {
      response.headers["cache-control"] = listOf(cacheControlHeader)
    }
  }
}
