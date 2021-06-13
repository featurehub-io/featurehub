package io.featurehub.web.security.oauth;

import io.featurehub.web.security.oauth.providers.OAuth2Provider;

public interface OAuth2Client {
  AuthClientResult requestAccess(String code, OAuth2Provider provider);
}
