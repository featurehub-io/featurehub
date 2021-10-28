package io.featurehub.jersey

import io.featurehub.utils.FallbackPropertyConfig
import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer

class DelegatingHandler constructor(private val jerseyHandler: GrizzlyHttpContainer, private val staticHttpHandler: AdminAppStaticHttpHandler) : HttpHandler() {
  private var jerseyPrefixes = listOf("/mr-api/", "/oauth/", "/features")

  init {
    var prefixes = FallbackPropertyConfig.getConfig("jersey.prefixes")

    if (prefixes != null) {
      jerseyPrefixes = prefixes.split(",").map { it.trim() }.filter { it.isEmpty() }.toList()
    }
  }

  override fun start() {
    super.start()

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

    if (url.isEmpty() || url == "/" || url.endsWith(".html") || url.endsWith(".js") || url.startsWith("/assets")) {
      return staticHttpHandler.service(request, response)
    }

    // do the html5 think and redirect everything else to index.html
    if (jerseyPrefixes.none { url.startsWith(it)} ) {
      staticHttpHandler.handle("/index.html", request, response)

      return
    }

    return jerseyHandler.service(request, response)
  }
}
