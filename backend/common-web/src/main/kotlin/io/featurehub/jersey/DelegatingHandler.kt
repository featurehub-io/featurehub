package io.featurehub.jersey

import io.featurehub.metrics.MetricsCollector
import io.featurehub.utils.FallbackPropertyConfig
import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer

class DelegatingHandler(
  private val jerseyHandler: GrizzlyHttpContainer,
  private val staticHttpHandler: AdminAppStaticHttpHandler
) : HttpHandler() {
  private var jerseyPrefixes = listOf("/mr-api/", "/saml/", "/oauth/", "/features", "/health/", "/metrics", "/info")

  private val webRequestCounter = MetricsCollector.counter("web_request_counter", "Amount of requests from serving the front end Admin website")
  private val apiRequestCounter = MetricsCollector.counter("api_request_counter", "Number of API requests received")
  private val featureRequestCounter = MetricsCollector.counter("feature_request_counter", "Number of Feature requests we have received")

  init {
    var prefixes = FallbackPropertyConfig.getConfig("jersey.prefixes")

    if (prefixes != null) {
      jerseyPrefixes = prefixes.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }
  }

  override fun start() {
    super.start()

    staticHttpHandler.isFileCacheEnabled = false
    jerseyHandler.start()
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

    if (url.startsWith("/features")) {
      featureRequestCounter.inc()
      return jerseyHandler.service(request, response)
    }

    if (url.isEmpty() || url == "/" || url.endsWith(".ico") || url.endsWith(".html") || url.endsWith(".js") || url.startsWith("/assets") || url.startsWith("/canvaskit")) {
      webRequestCounter.inc()
      return staticHttpHandler.service(request, response)
    }

    // do the html5 thing and redirect everything else to index.html.
    if (jerseyPrefixes.none { url.startsWith(it)} ) {
      webRequestCounter.inc()
      // this MUST be index.html, as all those random html5 urls are not actual files, e.g. /setup and /dashboard. if we let the
      // actual request pass through, it will never find the file and so if a user refreshes their page they will get nothing.
      staticHttpHandler.handle("/index.html", request, response)

      return
    }

    apiRequestCounter.inc()

    return jerseyHandler.service(request, response)
  }
}
