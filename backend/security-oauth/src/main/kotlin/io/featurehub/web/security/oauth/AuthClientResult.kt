package io.featurehub.web.security.oauth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class AuthClientResult {
  @JsonProperty("access_token")
  var accessToken: String? = null

  @JsonProperty("id_token")
  var idToken: String? = null

  @JsonProperty("token_type")
  var tokenType: String? = null

  @JsonProperty("scope")
  var scope: String? = null
  override fun toString(): String {
    return "AuthClientResult{" +
      "accessToken='" + accessToken + '\'' +
      ", idToken='" + idToken + '\'' +
      ", tokenType='" + tokenType + '\'' +
      ", scope='" + scope + '\'' +
      '}'
  }
}
