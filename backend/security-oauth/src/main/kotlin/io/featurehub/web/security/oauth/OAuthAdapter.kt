package io.featurehub.web.security.oauth

import javax.ws.rs.core.Response

// this is a call back mechanism that lets the core system
interface OAuthAdapter {
    fun successfulCompletion(
        email: String?,
        username: String?,
        userMustBeCreatedFirst: Boolean,
        failureUrl: String?,
        successUrl: String?
    ): Response?

    // does the organisation need to be created and does it already exist
    fun initialAppSetupComplete(): Boolean
}
