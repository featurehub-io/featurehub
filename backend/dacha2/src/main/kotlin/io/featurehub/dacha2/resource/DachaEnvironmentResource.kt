package io.featurehub.dacha2.resource

import io.featurehub.dacha.api.DachaEnvironmentService
import io.featurehub.dacha.model.DachaStructureResponse
import io.featurehub.dacha2.Dacha2Cache
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import java.util.*

class DachaEnvironmentResource @Inject constructor(private val cache: Dacha2Cache) : DachaEnvironmentService {
  override fun getEnvironmentStructure(eId: UUID): DachaStructureResponse {
    try {
      val env = cache.findEnvironment(eId).environment

      return DachaStructureResponse()
        .organizationId(env.organizationId)
        .portfolioId(env.portfolioId)
        .applicationId(env.applicationId)
    } catch (e: Exception) {
      throw NotFoundException()
    }
  }
}
