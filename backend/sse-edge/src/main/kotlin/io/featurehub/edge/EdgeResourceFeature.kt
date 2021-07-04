package io.featurehub.edge

import io.featurehub.edge.rest.EventStreamResource
import io.featurehub.edge.rest.SSEHeaderFilter
import javax.ws.rs.core.Feature
import javax.ws.rs.core.FeatureContext

class EdgeResourceFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context
      .register(EventStreamResource::class.java)
      .register(SSEHeaderFilter::class.java)

    return true
  }
}
