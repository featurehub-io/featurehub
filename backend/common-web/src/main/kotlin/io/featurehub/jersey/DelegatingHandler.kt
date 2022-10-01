package io.featurehub.jersey

import io.featurehub.metrics.MetricsCollector
import io.featurehub.utils.FallbackPropertyConfig
import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer

class DelegatingHandler(
  private val staticHttpHandler: AdminAppStaticHttpHandler
) : HttpHandler() {
  private val webRequestCounter = MetricsCollector.counter("web_request_counter", "Amount of requests from serving the front end Admin website")
  private val apiRequestCounter = MetricsCollector.counter("api_request_counter", "Number of API requests received")
  private val featureRequestCounter = MetricsCollector.counter("feature_request_counter", "Number of Feature requests we have received")

  override fun start() {
    super.start()

    staticHttpHandler.isFileCacheEnabled = false
    staticHttpHandler.start()
  }

  @Throws(Exception::class)
  fun getRelativeURI(request: Request): String? {
    var uri = request.decodedRequestURI
    return if (uri.contains("..")) {
      null
    } else {
      val resourcesContextPath = request.contextPath
      if (resourcesContextPath != null && !resourcesContextPath.isEmpty()) {
        if (!uri.startsWith(resourcesContextPath)) {
          return null
        }
        uri = uri.substring(resourcesContextPath.length)
      }
      uri
    }
  }

  override fun service(request: Request, response: Response) {
    val uriRef = request.request.requestURIRef
    uriRef.defaultURIEncoding = requestURIEncoding

    val url = getRelativeURI(request)

    if (url == null) {   // root url request
      webRequestCounter.inc()
      staticHttpHandler.handle("/index.html", request, response)
      return
    }

    staticHttpHandler.service(request, response)
  }
}
