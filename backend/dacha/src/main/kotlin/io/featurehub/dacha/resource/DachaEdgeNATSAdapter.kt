package io.featurehub.dacha.resource

import io.featurehub.mr.model.DachaNATSRequest
import io.featurehub.mr.model.DachaNATSResponse
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException

interface DachaEdgeNATSAdapter {
  fun edgeRequest(request: DachaNATSRequest): DachaNATSResponse
}

class DacheEdgeNATSAdapterService @Inject constructor(private val apiKeyResource: DachaApiKeyResource) : DachaEdgeNATSAdapter {
  override fun edgeRequest(request: DachaNATSRequest): DachaNATSResponse {
    if (request.featuresRequest != null) {
      try {
        val response = apiKeyResource.getApiKeyDetails(request.featuresRequest!!.geteId(), request.featuresRequest!!.serviceAccountKey)

        return DachaNATSResponse().status(200).featuresResponse(response)
      } catch (e: WebApplicationException) {
        return DachaNATSResponse().status(e.response.status)
      } catch (e: Exception) {
        return DachaNATSResponse().status(500)
      }
    }
  }

}
