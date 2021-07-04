package io.featurehub.web.security.oauth.providers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class GithubUser {
    @JsonProperty("name")
    var name: String? = null

    @JsonProperty("email")
    var email: String? = null
}
