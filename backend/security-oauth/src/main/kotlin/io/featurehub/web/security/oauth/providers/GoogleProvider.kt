package io.featurehub.web.security.oauth.providers;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.web.security.oauth.AuthClientResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GoogleProvider implements OAuth2Provider {
  public static final String PROVIDER_NAME = "oauth2-google";
  @ConfigKey("oauth2.providers.google.secret")
  protected String oauthClientSecret;
  @ConfigKey("oauth2.providers.google.id")
  protected String oauthClientID;
  @ConfigKey("oauth2.redirectUrl")
  protected String redirectUrl;

  private String actualAuthUrl;
  private String tokenUrl;

  public GoogleProvider() {
    DeclaredConfigResolver.resolve(this);

    actualAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?&scope=profile%20email&access_type=online" +
      "&include_granted_scopes=true&response_type=code&client_id=" + oauthClientID + "&redirect_uri=" +
      URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);

    tokenUrl = "https://oauth2.googleapis.com/token";
  }

  @Override
  public ProviderUser discoverProviderUser(AuthClientResult authed) {
    Map<String, String> idInfo = Jwt.decodeJwt(authed.getIdToken());

    if (idInfo == null) {
      return null;
    }

    return new ProviderUser.Builder().email(idInfo.get("email")).name(idInfo.get("given_name") + " " + idInfo.get(
      "family_name")).build();
  }

  @Override
  public String providerName() {
    return PROVIDER_NAME;
  }

  @Override
  public String requestTokenUrl() {
    return tokenUrl;
  }

  @Override
  public String requestAuthorizationUrl() {
    return actualAuthUrl;
  }

  @Override
  public String getClientId() {
    return oauthClientID;
  }

  @Override
  public String getClientSecret() {
    return oauthClientSecret;
  }
}
