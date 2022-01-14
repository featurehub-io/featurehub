package io.featurehub.jersey

import io.featurehub.utils.FallbackPropertyConfig
import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer

class DelegatingHandler constructor(private val jerseyHandler: GrizzlyHttpContainer, private val staticHttpHandler: AdminAppStaticHttpHandler) : HttpHandler() {
  private var jerseyPrefixes = listOf("/mr-api/", "/oauth/", "/features", "/health/", "/metrics", "/info")

  init {
    var prefixes = FallbackPropertyConfig.getConfig("jersey.prefixes")

    if (prefixes != null) {
      jerseyPrefixes = prefixes.split(",").map { it.trim() }.filter { it.isEmpty() }.toList()
    }
  }

  override fun start() {
    super.start()

    staticHttpHandler.isFileCacheEnabled = false
    jerseyHandler.start()
    staticHttpHandler.start()
  }

  override fun service(request: Request, response: Response) {
    val uriRef = request.request.requestURIRef
    uriRef.defaultURIEncoding = requestURIEncoding

    val decodedURI = uriRef.getDecodedRequestURIBC(isAllowEncodedSlash)
    val url = decodedURI.toString()

    //
    if (url.startsWith("/features")) {
      return jerseyHandler.service(request, response)
    }

    if (url.isEmpty() || url == "/" || url.endsWith(".html") || url.endsWith(".js") || url.startsWith("/assets") || url.startsWith("/canvaskit")) {
      return staticHttpHandler.service(request, response)
    }

    // do the html5 thing and redirect everything else to index.html.
    if (jerseyPrefixes.none { url.startsWith(it)} ) {
      // this MUST be index.html, as all those random html5 urls are not actual files, e.g. /setup and /dashboard. if we let the
      // actual request pass through, it will never find the file and so if a user refreshes their page they will get nothing.
      staticHttpHandler.handle("/index.html", request, response)

      return
    }

    return jerseyHandler.service(request, response)
  }
}
