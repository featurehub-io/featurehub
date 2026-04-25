package io.featurehub.rest

import io.featurehub.metrics.MetricsCollector
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter

class ResponseTrackingResponseFilter : ContainerResponseFilter {
  private val response_5xx = MetricsCollector.counter("response_5xx", "5xx response count")
  private val response_4xx = MetricsCollector.counter("response_4xx", "4xx response count")
  private val response_3xx = MetricsCollector.counter("response_3xx", "3xx response count")
  private val response_2xx = MetricsCollector.counter("response_2xx", "2xx response count")
  private val response_total = MetricsCollector.counter("requests_total", "Total tracked requests")

  override fun filter(
    requestContext: ContainerRequestContext,
    responseContext: ContainerResponseContext
  ) {
    response_total.inc()

    if (responseContext.getStatus() >= 500) {
      response_5xx.inc()
    } else if (responseContext.getStatus() >= 400) {
      response_4xx.inc()
    } else if (responseContext.getStatus() >= 300) {
      response_3xx.inc()
    } else if (responseContext.getStatus() >= 200) {
      response_2xx.inc()
    }

  }
}
