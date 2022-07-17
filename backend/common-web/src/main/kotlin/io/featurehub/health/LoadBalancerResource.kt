package io.featurehub.health

import cd.connect.jersey.prometheus.Prometheus
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response

@Path("/")
class LoadBalancerResource {
  @GET
  @Prometheus(name = "load_balancer_root_api", help = "Root url designed for the load balancer")
  fun loadBalancerAck(): Response {
    return Response.ok().entity("featurehub is here").build()
  }
}
