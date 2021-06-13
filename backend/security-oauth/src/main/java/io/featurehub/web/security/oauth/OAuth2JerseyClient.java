package io.featurehub.web.security.oauth;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.web.security.oauth.providers.OAuth2Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class OAuth2JerseyClient implements OAuth2Client {
  private static final Logger log = LoggerFactory.getLogger(OAuth2JerseyClient.class);
  protected final Client client;

  // the url we pass to the POST to confirm we are who we say we are
  @ConfigKey("oauth2.redirectUrl")
  protected String redirectUrl;

  @Inject
  public OAuth2JerseyClient(Client client) {
    this.client = client;

    DeclaredConfigResolver.resolve(this);
  }

  public AuthClientResult requestAccess(String code, OAuth2Provider provider) {
    Form form = new Form();
    form.param("grant_type", "authorization_code");
    form.param("client_id", provider.getClientId());
    form.param("client_secret", provider.getClientSecret());
    form.param("redirect_uri", redirectUrl);
    form.param("code", code);

    Entity<Form> entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE);

    final Response response = client.target(provider.requestTokenUrl()).request()
      .accept(MediaType.APPLICATION_JSON)
      .post(entity);
    if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
      return response.readEntity(AuthClientResult.class);
    } else {
      log.warn("OAuth2 Login attempt failed! {}", response.getStatus());
      return null;
    }
  }
}
