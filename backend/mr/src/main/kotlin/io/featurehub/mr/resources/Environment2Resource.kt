package io.featurehub.mr.resources

import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.EnvironmentApi.DuplicateEnvironmentException
import io.featurehub.db.api.EnvironmentApi.InvalidEnvironmentChangeException
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.mr.api.Environment2ServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.UpdateEnvironment
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import java.util.*

class Environment2Resource @Inject constructor(
  private val authManager: AuthManagerService,
  private val environmentApi: EnvironmentApi
) : Environment2ServiceDelegate {

  @Deprecated("Deprecated in Java")
  override fun updateEnvironmentV2(
    eid: UUID,
    updateEnvironment: UpdateEnvironment,
    holder: Environment2ServiceDelegate.UpdateEnvironmentV2Holder,
    securityContext: SecurityContext
  ): Environment {
    val current: io.featurehub.mr.model.Person = authManager.from(securityContext)

    if (authManager.isOrgAdmin(current) ||
      authManager.isPortfolioAdminOfEnvironment(eid, current)
    ) {
      return try {
        environmentApi.updateEnvironment(
          eid, updateEnvironment, Opts().add(
            FillOpts.Acls,
            holder.includeAcls
          ).add(FillOpts.Features, holder.includeFeatures).add(FillOpts.Details, holder.includeDetails)
        ) ?: throw NotFoundException()
      } catch (e: OptimisticLockingException) {
        throw WebApplicationException(422)
      } catch (e: DuplicateEnvironmentException) {
        throw WebApplicationException(jakarta.ws.rs.core.Response.Status.CONFLICT)
      } catch (e: InvalidEnvironmentChangeException) {
        throw BadRequestException()
      }
    }

    throw ForbiddenException()
  }

}
