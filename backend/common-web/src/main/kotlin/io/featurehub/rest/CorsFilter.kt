package io.featurehub.rest

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import jakarta.ws.rs.container.*
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import java.io.IOException
import java.util.*

@Provider
@PreMatching
class CorsFilter : ContainerRequestFilter, ContainerResponseFilter {
  @ConfigKey("jersey.cors.headers")
  var allowedHeaders: List<String> = listOf()

  private var headers: String? = null

  init {
    DeclaredConfigResolver.resolve(this)
    val actualHeaders: Set<String> = HashSet(allowedHeaders.map { it.lowercase() })
      .plus("baggage").plus("if-none-match").plus("etag").plus("x-requested-with")
      .plus("authorization").plus("content-type").plus("accept-version").plus("content-md5")
      .plus("csrf-token").plus("x-ijt").plus("cache-control").plus("x-featurehub")

    headers = java.lang.String.join(",", actualHeaders)
  }

  /**
   * Method for ContainerRequestFilter.
   */
  @Throws(IOException::class)
  override fun filter(request: ContainerRequestContext) {

    // If it's a preflight request, we abort the request with
    // a 200 status, and the CORS headers are added in the
    // response filter method below.
    if (isPreflightRequest(request)) {
      request.abortWith(Response.ok().build())
      return
    }
  }

  /**
   * A preflight request is an OPTIONS request
   * with an Origin header.
   */
  private fun isPreflightRequest(request: ContainerRequestContext): Boolean {
    return (request.getHeaderString("Origin") != null
      && request.method.equals("OPTIONS", ignoreCase = true))
  }

  /**
   * Method for ContainerResponseFilter.
   */
  override fun filter(request: ContainerRequestContext, response: ContainerResponseContext) {

    // if there is no Origin header, then it is not a
    // cross origin request. We don't do anything.
    if (request.getHeaderString("Origin") == null) {
      return
    }

    // If it is a preflight request, then we add all
    // the CORS headers here.
    if (isPreflightRequest(request)) {
      response.headers.add("Access-Control-Allow-Credentials", "true")
      response.headers.add(
        "Access-Control-Allow-Methods",
        "GET, POST, PUT, DELETE, OPTIONS, HEAD"
      )
      response.headers.addAll("Access-Control-Allow-Headers", headers)
    }

    // Cross origin requests can be either simple requests
    // or preflight request. We need to add this header
    // to both type of requests. Only preflight requests
    // need the previously added headers.
    response.headers.add("Access-Control-Expose-Headers", "etag")
    response.headers.add("Access-Control-Allow-Origin", "*")
  }
}
