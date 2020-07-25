package io.featurehub.mr.resources.oauth2;

import io.featurehub.mr.resources.oauth2.providers.OAuth2Provider;

public interface OAuth2Client {
  AuthClientResult requestAccess(String code, OAuth2Provider provider);
}
