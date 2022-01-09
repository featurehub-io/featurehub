package io.featurehub.dacha.resource

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.mr.model.DachaNATSRequest
import io.featurehub.mr.model.DachaNATSResponse
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException

interface DachaEdgeNATSAdapter {
  fun edgeRequest(request: DachaNATSRequest): DachaNATSResponse
}

class DacheEdgeNATSAdapterService @Inject constructor(private val apiKeyResource: DachaApiKeyService) : DachaEdgeNATSAdapter {
  override fun edgeRequest(request: DachaNATSRequest): DachaNATSResponse {
    if (request.featuresRequest != null) {
      try {
        val response = apiKeyResource.getApiKeyDetails(request.featuresRequest!!.geteId(), request.featuresRequest!!.serviceAccountKey)

        return DachaNATSResponse().status(200).featuresResponse(response)
      } catch (e: WebApplicationException) {
        return DachaNATSResponse().status(e.response.status)
      } catch (e: Exception) {
        return DachaNATSResponse().status(412)
      }
    } else if (request.permissionRequest != null) {
      try {
        val permissionRequest = request.permissionRequest!!
        val response = apiKeyResource.getApiKeyPermissions(permissionRequest.geteId(), permissionRequest.serviceAccountKey, permissionRequest.featureKey)

        return DachaNATSResponse().status(200).permissionResponse(response)
      } catch (e: WebApplicationException) {
        return DachaNATSResponse().status(e.response.status)
      } catch (e: Exception) {
        return DachaNATSResponse().status(412)
      }
    }

    return DachaNATSResponse().status(400) // no idea what was asked for
  }

}
