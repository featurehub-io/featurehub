package io.featurehub.mr.resources

import io.featurehub.db.api.UserStateApi
import io.featurehub.mr.api.UserStateServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.HiddenEnvironments
import io.featurehub.mr.utils.ApplicationUtils
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import java.util.*

class UserStateResource @Inject constructor(
    private val authManager: AuthManagerService,
    private val applicationUtils: ApplicationUtils,
    private val userStateApi: UserStateApi
) : UserStateServiceDelegate {
    override fun getHiddenEnvironments(appId: UUID, securityContext: SecurityContext): HiddenEnvironments {
        applicationUtils.featureReadCheck(securityContext, appId)
        val hiddenEnvironments = userStateApi.getHiddenEnvironments(authManager.from(securityContext), appId)
        return hiddenEnvironments ?: HiddenEnvironments()
    }

    override fun saveHiddenEnvironments(
        appId: UUID, hiddenEnvironments: HiddenEnvironments,
        securityContext: SecurityContext
    ): HiddenEnvironments {
        applicationUtils.featureReadCheck(securityContext, appId)
        try {
            userStateApi.saveHiddenEnvironments(authManager.from(securityContext), hiddenEnvironments, appId)
        } catch (e: UserStateApi.InvalidUserStateException) {
            throw WebApplicationException(Response.status(422).entity(e.message).build())
        }
        return hiddenEnvironments
    }
}
