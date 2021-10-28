package org.glassfish.jersey.grizzly2.httpserver

import jakarta.ws.rs.core.Application

// its package private so we can't create one in the right place, so we create it here
class HttpGrizzlyContainer {
  companion object {
    fun makeHandler(app: Application) : GrizzlyHttpContainer {
      return GrizzlyHttpContainer(app)
    }
  }
}
