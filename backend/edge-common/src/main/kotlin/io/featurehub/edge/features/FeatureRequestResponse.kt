package io.featurehub.edge.features

import io.featurehub.edge.KeyParts
import io.featurehub.mr.model.RoleType
import io.featurehub.sse.model.FeatureEnvironmentCollection

enum class FeatureRequestSuccess {
  NO_SUCH_KEY_IN_CACHE, SUCCESS, NO_CHANGE, DACHA_NOT_READY
}

/**
 * This holds the response per environment - it determines if we were successful in getting
 * this KeyPair or not. If Dacha does not know about it or it isn't up, success will be false.
 */
class FeatureRequestResponse(val environment: FeatureEnvironmentCollection, val success: FeatureRequestSuccess, val key: KeyParts, val etag: String, val envInfo: Map<String, String>?, val allowExtendedData: Boolean)
