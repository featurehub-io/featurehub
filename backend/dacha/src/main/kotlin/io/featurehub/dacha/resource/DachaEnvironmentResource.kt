package io.featurehub.dacha.resource

import io.featurehub.dacha.InternalCache
import io.featurehub.dacha.api.DachaEnvironmentService
import io.featurehub.dacha.model.DachaStructureResponse
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import org.glassfish.hk2.api.Immediate
import java.util.*


@Immediate
class DachaEnvironmentResource @Inject constructor(private val cache: InternalCache) : DachaEnvironmentService {
  override fun getEnvironmentStructure(eId: UUID): DachaStructureResponse {
    val env = cache.findEnvironment(eId) ?: throw NotFoundException()

    return DachaStructureResponse()
      .organizationId(env.organizationId)
      .portfolioId(env.portfolioId)
      .applicationId(env.applicationId)
  }
}
