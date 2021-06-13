package io.featurehub.web.security.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthClientResult {
  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("id_token")
  private String idToken;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("scope")
  private String scope;

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getIdToken() {
    return idToken;
  }

  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  @Override
  public String toString() {
    return "AuthClientResult{" +
      "accessToken='" + accessToken + '\'' +
      ", idToken='" + idToken + '\'' +
      ", tokenType='" + tokenType + '\'' +
      ", scope='" + scope + '\'' +
      '}';
  }
}
