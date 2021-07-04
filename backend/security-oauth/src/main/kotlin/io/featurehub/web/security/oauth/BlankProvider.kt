package io.featurehub.web.security.oauth

import javax.ws.rs.NotFoundException

// ensures we have at least one of these as injection requires it
class BlankProvider : AuthProvider {
    override val providers: Collection<String>
        get() = emptyList<String>()

    override fun requestRedirectUrl(provider: String): String {
        // if we get picked there is something seriously wrong
        throw NotFoundException()
    }
}
