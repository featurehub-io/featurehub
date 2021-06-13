package io.featurehub.web.security.oauth.providers;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.web.security.oauth.AuthClientResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

// register your app here: https://go.microsoft.com/fwlink/?linkid=2083908
// https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow
public class AzureProvider implements OAuth2Provider {
  public static final String PROVIDER_NAME = "oauth2-azure";
  @ConfigKey("oauth2.providers.azure.tenant")
  protected String tenant;
  @ConfigKey("oauth2.providers.azure.secret")
  protected String oauthClientSecret;
  @ConfigKey("oauth2.providers.azure.id")
  protected String oauthClientID;
  @ConfigKey("oauth2.redirectUrl")
  protected String redirectUrl;
  @ConfigKey("oauth2.providers.azure.scopes")
  protected String scopes = "openid email profile";

  private final String actualAuthUrl;
  private final String tokenUrl;

  public AzureProvider() {
    DeclaredConfigResolver.resolve(this);

    actualAuthUrl = String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?" +
      "response_mode=query&scope=%s&include_granted_scopes=true&response_type=code&client_id=%s&redirect_uri=%s",
      tenant,
      URLEncoder.encode(scopes, StandardCharsets.UTF_8),
      oauthClientID,
      URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8))
      ;

    tokenUrl = String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenant);
  }

  @Override
  public ProviderUser discoverProviderUser(AuthClientResult authed) {
    Map<String, String> idInfo = Jwt.decodeJwt(authed.getIdToken());

    if (idInfo == null) {
      return null;
    }

    return new ProviderUser.Builder().email(idInfo.get("email")).name(idInfo.get("name")).build();
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
