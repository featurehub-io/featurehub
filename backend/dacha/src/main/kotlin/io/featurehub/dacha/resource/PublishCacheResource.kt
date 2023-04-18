package io.featurehub.dacha.resource

import io.featurehub.dacha.InternalCache
import io.featurehub.dacha.model.PublishServiceAccount
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import java.util.*

data class CacheSize(val serviceAccountSize: Int, val environmentSize: Int)
data class TypeCacheSize(val name: String, val size: Int)

@Path("/dacha1/cache")
class PublishCacheResource @Inject constructor(private val cache: InternalCache) {

  @GET
  @Path("/apiKeys")
  @Produces("application/json")
  fun apiKeys(): List<String> {
    val l = mutableListOf<String>()
    var serviceAccounts = mutableMapOf<UUID, PublishServiceAccount>()
    cache.serviceAccounts()?.forEach { serviceAccounts.put(it.serviceAccount!!.id, it) }
    cache.environments()?.forEach { e -> e.serviceAccounts.forEach { sa -> l.add("${e.environment.id}/${serviceAccounts[sa]?.serviceAccount?.apiKeyServerSide}") } }
    return l;
  }

  @GET
  @Produces("application/json")
  fun consistencyCheck(): List<TypeCacheSize> {
    val list = mutableListOf<TypeCacheSize>()
    var lastSize: Int = 0
    list.add(TypeCacheSize("env-count", cache.getEnvironmentSize()))

    cache.environments()?.forEach { pe ->
      if (pe.count != lastSize) {
        list.add(TypeCacheSize("env", pe.count))
        lastSize  = pe.count
      }
    }

    list.add(TypeCacheSize("sa-count", cache.getServiceAccountSize()))
    cache.serviceAccounts()?.forEach { sa ->
      if (sa.count != lastSize) {
        list.add(TypeCacheSize("sa", sa.count))
      }

      lastSize = sa.count
    }

    return list
  }
}
