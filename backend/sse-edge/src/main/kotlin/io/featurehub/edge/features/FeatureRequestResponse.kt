package io.featurehub.edge.features

import io.featurehub.edge.KeyParts
import io.featurehub.sse.model.Environment

/**
 * This holds the response per environment - it determines if we were successful in getting
 * this KeyPair or not. If Dacha does not know about it or it isn't up, success will be false.
 */
class FeatureRequestResponse(val environment: Environment, val success: Boolean, val key: KeyParts)
