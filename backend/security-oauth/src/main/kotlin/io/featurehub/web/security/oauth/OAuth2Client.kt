package io.featurehub.web.security.oauth

import io.featurehub.web.security.oauth.providers.OAuth2Provider

interface OAuth2Client {
    fun requestAccess(code: String?, provider: OAuth2Provider): AuthClientResult?
}
