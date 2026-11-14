package io.featurehub.responder

import io.featurehub.rest.MaintenanceNotificationFilter
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.annotation.Priority
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Provider
@PreMatching
@Priority(2000)
class AbortForMaintenanceFilter : ContainerRequestFilter {
  companion object {
    val log: Logger = LoggerFactory.getLogger(AbortForMaintenanceFilter::class.java)

    val interceptPrefixes = FallbackPropertyConfig.getConfig("interceptPrefixes", "mr-api,saml,oauth,dacha2,info")
      .split(",")
      .map { it.trim() }
      .filter { it.isNotBlank() }

    // the serving of web assets means we need to let the web container know what services are jersey resources and which ones to pass
    // through unaltered. This is never "/features" so wildcard mappings are OK here.
    fun setJerseyPrefixes() {
      System.setProperty("jersey.prefixes", interceptPrefixes.map { "/$it/*" }.joinToString(","))
    }
  }

  init {
    log.info("intercept prefixes are listed as {}", interceptPrefixes)
  }

  override fun filter(requestEvent: ContainerRequestContext) {
    log.trace("intercept path: {}", requestEvent.uriInfo.path)
    if (requestEvent.uriInfo.path == null) return
    if (interceptPrefixes.any({ requestEvent.uriInfo.path.startsWith(it) })) {
      log.trace("aborting request as matches")
      requestEvent.abortWith(
        Response.status(503)
          .entity(MaintenanceNotificationFilter.maintenanceMessage)
          .header("content-type", "text/plain")
          .header("retry-after",
          MaintenanceNotificationFilter.endAsRfc1123).build())
    }
  }
}
