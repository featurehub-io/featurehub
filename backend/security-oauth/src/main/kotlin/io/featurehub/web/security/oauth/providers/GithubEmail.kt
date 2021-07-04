package io.featurehub.web.security.oauth.providers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class GithubEmail {
  @JsonProperty("email")
  var email: String? = null

  @JsonProperty("primary")
  var primary: Boolean? = null

  @JsonProperty("verified")
  var verified: Boolean? = null

  @JsonProperty("visibility")
  var visibility: String? = null
}
