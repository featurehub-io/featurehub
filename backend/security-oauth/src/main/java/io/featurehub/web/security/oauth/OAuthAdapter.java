package io.featurehub.web.security.oauth;

import javax.ws.rs.core.Response;

// this is a call back mechanism that lets the core system
public interface OAuthAdapter {
  Response successfulCompletion(String email, String username, boolean userMustBeCreatedFirst, String failureUrl, String successUrl);

  // does the organisation need to be created and does it already exist
  boolean organisationCreationRequired();
}
