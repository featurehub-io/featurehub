package org.glassfish.jersey.logging

import cd.connect.jersey.common.logging.JerseyFiltering
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.ConstrainedTo
import jakarta.ws.rs.RuntimeType

// @ConstrainedTo is not inherited, so it has to be restated here
@ConstrainedTo(RuntimeType.SERVER)
@Priority(Int.MAX_VALUE-2)
class ReprioritisedFilteringServerLoggingFilter @Inject constructor(jerseyFiltering: JerseyFiltering) : FilteringServerLoggingFilter(jerseyFiltering) {
}
