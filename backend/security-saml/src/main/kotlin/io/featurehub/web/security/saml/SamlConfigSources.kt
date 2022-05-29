package io.featurehub.web.security.saml

interface SamlConfigSources {
  fun getSourceFromRegistrationId(registrationId: String): SamlServiceProviderConfig?
}
