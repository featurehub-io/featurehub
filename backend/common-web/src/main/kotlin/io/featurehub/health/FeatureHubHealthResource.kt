package io.featurehub.health

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response
import org.glassfish.hk2.api.ServiceLocator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Path("/health")
class FeatureHubHealthResource
  @Inject constructor(private val serviceLocator: ServiceLocator) {
  private val log: Logger = LoggerFactory.getLogger(FeatureHubHealthResource::class.java)

  private val healthSources: MutableList<HealthSource> = mutableListOf()

  private fun allHealthy(): Boolean {
    if (healthSources.isEmpty()) {
      healthSources.addAll(serviceLocator.getAllServices(HealthSource::class.java))
    }

    var healthy = true;

    for (hs in healthSources) {
      if (!hs.healthy) {
        log.error("Health source {} is failing", hs.sourceName)
        healthy = false;
      }
    }

    return healthy
  }

  @GET
  @Path("/liveness")
  fun liveness(): Response {
    return if (allHealthy()) Response.ok().build() else Response.serverError().build()
  }

  @GET
  @Path("/readiness")
  fun readyness(): Response {
    return if (allHealthy()) Response.ok().build() else Response.serverError().build()
  }

  @GET
  @Path("/readyness")
  fun readyness2(): Response {
    return readyness()
  }
}
