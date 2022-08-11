package io.featurehub.rest

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import jakarta.annotation.Priority
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider

@Provider
@Priority(100)
class CacheControlFilter : ContainerResponseFilter {
  @ConfigKey("cache-control.api")
  var cacheControlHeader: String? = "no-store, max-age=0"
  @ConfigKey("cache-control.api.enabled")
  var enableCacheControl: Boolean? = true

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
    if (enableCacheControl!! && requestContext.method == "GET" && responseContext.stringHeaders.keys.none { it.lowercase() == "cache-control" }) {
      responseContext.headers.add("cache-control", cacheControlHeader)
    }
  }
}
