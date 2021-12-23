package io.featurehub.jersey

import io.featurehub.utils.FallbackPropertyConfig
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.grizzly.http.server.StaticHttpHandler
import org.glassfish.grizzly.http.util.MimeType

class AdminAppStaticHttpHandler: StaticHttpHandler(setOf(FallbackPropertyConfig.getConfig("web.asset.location", "/var/www/html"))) {
  init {
      MimeType.add("wasm", "application/wasm")
  }

  public override fun handle(uri: String?, request: Request?, response: Response?): Boolean {
    return super.handle(uri, request, response)
  }
}
