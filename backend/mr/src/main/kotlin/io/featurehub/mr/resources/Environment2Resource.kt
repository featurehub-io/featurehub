package io.featurehub.mr.resources

import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.EnvironmentApi.DuplicateEnvironmentException
import io.featurehub.db.api.EnvironmentApi.InvalidEnvironmentChangeException
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.exception.MissingEncryptionPasswordException
import io.featurehub.mr.api.Environment2ServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.UpdateEnvironment
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import java.util.*

class Environment2Resource @Inject constructor(
  private val authManager: AuthManagerService,
  private val environmentApi: EnvironmentApi
) : Environment2ServiceDelegate {

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
      val update: Environment?
      try {
        update = environmentApi.updateEnvironment(
          eid, updateEnvironment, Opts().add(
            FillOpts.Acls,
            holder.includeAcls
          ).add(FillOpts.Features, holder.includeFeatures).add(FillOpts.Details, holder.includeDetails)
        )
      } catch (e: OptimisticLockingException) {
        throw WebApplicationException(422)
      } catch (e: DuplicateEnvironmentException) {
        throw WebApplicationException(jakarta.ws.rs.core.Response.Status.CONFLICT)
      } catch (e: InvalidEnvironmentChangeException) {
        throw BadRequestException()
      } catch (e: MissingEncryptionPasswordException) {
        //TODO openapi generator currently does not return 412, always returns 400. So the generator needs to be updated to allow throwing 412
        // Or some other way front end can differentiate between 400 error above vs encryption password missing
        throw WebApplicationException(412)
      }
      if (update == null) {
        throw jakarta.ws.rs.NotFoundException()
      }
      return update
    }

    throw ForbiddenException()
  }

}
