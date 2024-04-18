package io.featurehub.mr.resources

import io.featurehub.db.api.TrackingEventApi
import io.featurehub.mr.api.TrackEventsServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.TrackEventsSummary
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.SecurityContext
import java.util.UUID

class TrackEventResource @Inject constructor(
  private val authManagerService: AuthManagerService,
  private val trackingEventApi: TrackingEventApi) : TrackEventsServiceDelegate {
  override fun getTrackedEvents(
    id: UUID,
    source: String,
    page: Int,
    pageSize: Int,
    cloudEventType: String,
    holder: TrackEventsServiceDelegate.GetTrackedEventsHolder,
    securityContext: SecurityContext?
  ): TrackEventsSummary {
    if (source != "env") {
      throw NotFoundException("Unknown source")
    }
    val person = authManagerService.from(securityContext)

    if (authManagerService.isOrgAdmin(person) || authManagerService.isPortfolioAdminOfEnvironment(id, person)) {
      return trackingEventApi.findEvents(source, id, cloudEventType, page, pageSize, holder.firstOnly != false)
    }

    throw ForbiddenException("You don't have sufficient access")
  }

}
