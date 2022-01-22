package io.featurehub.rest

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.context.ConnectContext
import jakarta.ws.rs.container.*
import jakarta.ws.rs.ext.Provider

/**
 * We should only be wired if there is a setting at all for tracking web config, otherwise it is a waste of
 * resources to intercept. Do NOT wire if the config-key doesn't exist
 */
@Provider
@PreMatching
class WebHeaderAuditLogger : ContainerRequestFilter, ContainerResponseFilter {
  @ConfigKey("audit.logging.web.header-fields")
  var auditHeaders: List<String>? = null

  companion object {
    val CONFIG_KEY = "audit.logging.web.header-fields"
    val WEBHEADERS = "http-headers"
  }

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun filter(requestContext: ContainerRequestContext) {
    val headers = mutableMapOf<String, String>()

    auditHeaders?.forEach {header ->
      requestContext.getHeaderString(header)?.let {
        headers.put(header, it)
      }
    }

    if (headers.isNotEmpty()) {
      ConnectContext.set(WEBHEADERS, headers)
    }
  }

  override fun filter(requestContext: ContainerRequestContext?, responseContext: ContainerResponseContext?) {
    ConnectContext.remove(WEBHEADERS)
  }
}
