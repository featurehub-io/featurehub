package io.featurehub.db.model

import java.util.*

/**
 * This represents what is stored in the feature value when it changes. It is stored in the history table.
 * It is the strategy id, its version, its enabled status and its value
 */
data class SharedRolloutStrategyVersion(
    val strategyId : UUID, val version: Long,
    val enabled: Boolean, val value: Any
)
