package org.glassfish.jersey.logging

import cd.connect.jersey.common.logging.JerseyFiltering
import jakarta.annotation.Priority
import jakarta.inject.Inject

@Priority(Int.Companion.MAX_VALUE-2)
class ReprioritisedFilteringServerLoggingFilter @Inject constructor(jerseyFiltering: JerseyFiltering) : FilteringServerLoggingFilter(jerseyFiltering) {
}

@Priority(Int.Companion.MAX_VALUE-2)
class ReprioritisedFilteringClientLoggingFilter @Inject constructor(jerseyFiltering: JerseyFiltering) : FilteringClientLoggingFilter(jerseyFiltering) {
}


