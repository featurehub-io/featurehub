package io.featurehub.edge.db.sql

import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.messaging.FeatureMessagingPublisher
import java.util.UUID

/**
 * EdgeRest does not publish webhooks
 */
class DummyFeatureMessagingPublisher : FeatureMessagingPublisher {
  override fun publish(
    featureMessagingParameter: FeatureMessagingParameter,
    orgId: UUID
  ) {
  }
}
