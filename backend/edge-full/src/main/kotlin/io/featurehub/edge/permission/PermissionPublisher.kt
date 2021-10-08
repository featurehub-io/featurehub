package io.featurehub.edge.permission

import io.featurehub.edge.KeyParts
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.mr.model.DachaPermissionResponse

interface PermissionPublisher {
  fun publishFeatureChangeRequest(featureUpdate: StreamedFeatureUpdate, namedCache: String)
  fun requestPermission(key: KeyParts, featureKey: String?): DachaPermissionResponse?
}
