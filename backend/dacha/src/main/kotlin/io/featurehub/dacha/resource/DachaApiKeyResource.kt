package io.featurehub.dacha.resource

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.dacha.InternalCache
import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.model.DachaKeyDetailsResponse
import io.featurehub.dacha.model.DachaPermissionResponse
import jakarta.inject.Inject
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.glassfish.hk2.api.Immediate
import java.util.*

@Immediate
class DachaApiKeyResource @Inject constructor(private val cache: InternalCache) : DachaApiKeyService {
  @ConfigKey("dacha.busy-timeout")
  var retryAfter = "5"

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun getApiKeyDetails(eId: UUID, serviceAccountKey: String): DachaKeyDetailsResponse {
    // in a proper load balance solution, this won't happen, it can happen in Party Server so we need to show the correct error

    if (!cache.cacheComplete()) {
      throw WebApplicationException(Response.status(503).header("Retry-After", retryAfter).entity("The server is not ready yet").build())
    }

    val collection = cache.getFeaturesByEnvironmentAndServiceAccount(eId, serviceAccountKey) ?: throw NotFoundException()

    val environment = collection.features.environment
    return DachaKeyDetailsResponse()
      .organizationId(environment.organizationId)
      .portfolioId(environment.portfolioId)
      .applicationId(environment.applicationId)
      .serviceKeyId(collection.serviceAccountId)
      .etag(collection.features.etag)
      .features(collection.features.features.toMutableList())
  }

  override fun getApiKeyPermissions(eId: UUID, serviceAccountKey: String, featureKey: String): DachaPermissionResponse {
    val collection = cache.getFeaturesByEnvironmentAndServiceAccount(eId, serviceAccountKey) ?: throw NotFoundException()

    val feature = collection.features.features.find { fv -> fv.feature.key == featureKey } ?: throw NotFoundException()

    val environment = collection.features.environment
    return DachaPermissionResponse()
      .organizationId(environment.organizationId)
      .portfolioId(environment.portfolioId)
      .applicationId(environment.applicationId)
      .serviceKeyId(collection.serviceAccountId)
      .roles(collection.perms.permissions)
      .feature(feature)
  }
}
