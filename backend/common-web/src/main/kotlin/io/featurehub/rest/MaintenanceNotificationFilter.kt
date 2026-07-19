package io.featurehub.rest

import io.featurehub.utils.FallbackPropertyConfig
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import java.time.Instant

@Provider
class MaintenanceNotificationFilter : ContainerResponseFilter {
  override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
    if (start == null || end == null || maintenanceMessage == null) return

    responseContext.headers.putSingle("X-Maintenance-Start", start.toString())
    responseContext.headers.putSingle("X-Maintenance-End", end.toString())
    responseContext.headers.putSingle("X-Maintenance-Message", maintenanceMessage)
  }

  companion object {
    var start: Instant? = null
    var end: Instant? = null
    var maintenanceMessage: String? = null

    val corsHeaders = listOf("x-maintenance-start", "x-maintenance-end", "x-maintenance-message")

    fun wireFilterCheck(): Boolean {
      start = FallbackPropertyConfig.getConfig("maintenance.start")?.let { runCatching { Instant.parse(it) }.getOrNull() }
      end = FallbackPropertyConfig.getConfig("maintenance.end")?.let { runCatching { Instant.parse(it) }.getOrNull() }
      maintenanceMessage = FallbackPropertyConfig.getConfig("maintenance.message")

      return start != null && end != null && maintenanceMessage != null
    }
  }
}
