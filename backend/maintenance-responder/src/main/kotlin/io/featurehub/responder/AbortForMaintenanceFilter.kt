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
    val interceptPrefixes = FallbackPropertyConfig.getConfig("interceptPrefixes", "mr-api")
      .split(",")
      .map { it.trim() }
      .filter { it.isNotBlank() }
  }

  override fun filter(requestEvent: ContainerRequestContext) {
    if (requestEvent.uriInfo.path != null && interceptPrefixes.any({ requestEvent.uriInfo.path.startsWith(it) })) {
      log.info("Intercepting request ${requestEvent.uriInfo.path}")
      requestEvent.abortWith(
        Response.status(503, MaintenanceNotificationFilter.maintenanceMessage).header("retry-after",
          MaintenanceNotificationFilter.end).build())
    }
  }
}
