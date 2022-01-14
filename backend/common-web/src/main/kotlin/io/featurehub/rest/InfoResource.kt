package io.featurehub.rest

import com.fasterxml.jackson.annotation.JsonProperty
import io.featurehub.info.ApplicationVersion
import io.featurehub.utils.FeatureHubConfig
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces

data class RestInfoVersion (
  @JsonProperty("name")
  val name: String,
  @JsonProperty("version")
  val version: String)

class Info {
  companion object {
    const val APPLICATION_NAME_PROPERTY = "application.name"
  }
}

@Path("/info")
class InfoResource @Inject constructor(private val applicationVersion: ApplicationVersion, @FeatureHubConfig(Info.APPLICATION_NAME_PROPERTY) private val appName: String) {
  @Path("/version")
  @GET
  @Produces("application/json")
  fun version() : RestInfoVersion {
    return RestInfoVersion(appName, applicationVersion.appVersion())
  }
}
