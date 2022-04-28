package io.featurehub.health

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response

@Path("/")
class LoadBalancerResource {
  @GET
  fun loadBalancerAck(): Response {
    return Response.ok().entity("featurehub is here").build()
  }
}
