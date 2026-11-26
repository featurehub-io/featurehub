package io.featurehub.edge.features

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheRolloutStrategyAttribute
import io.featurehub.mr.model.RolloutStrategyAttributeConditional
import io.featurehub.mr.model.RolloutStrategyFieldType
import io.featurehub.strategies.matchers.WhenActiveMatcher
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * For server evaluated keys, Edge evaluates strategies and sends only the result. Some strategies depend on Edge's
 * clock rather than on the feature data or the client context - fhubWhenActive, and "now" DATE/DATETIME attributes -
 * so their result can change while the environment etag (a hash of the feature data) and the context tag stay
 * the same. Without help a client presenting its etag would get NO_CHANGE / 304 forever after the time had passed.
 *
 * This works out every moment at which such a strategy's result could flip, and counts how many of them the clock
 * has passed. Appending that count to the environment's etag makes the etag change exactly when a result could, while
 * leaving it stable (and 304s working) between those moments. Environments without any clock-based strategies get
 * no suffix, so their etags are unchanged.
 *
 * Client evaluated keys don't need this: they are sent the strategies and the SDK evaluates them against its own clock.
 */
object TimeBoundaryEtag {
  private const val NOW = "now"
  private val WHEN_ACTIVE_CONDITIONALS = setOf(
    RolloutStrategyAttributeConditional.EQUALS, RolloutStrategyAttributeConditional.NOT_EQUALS,
    RolloutStrategyAttributeConditional.GREATER, RolloutStrategyAttributeConditional.GREATER_EQUALS,
    RolloutStrategyAttributeConditional.LESS, RolloutStrategyAttributeConditional.LESS_EQUALS)

  /**
   * @return the etag to use for this environment for a server evaluated request
   */
  fun etag(dachaEtag: String, features: List<CacheEnvironmentFeature>?, clock: Clock): String =
    etag(dachaEtag, features, clock.instant())

  /**
   * @param now the instant the request's features are evaluated at, so the count and the values agree
   */
  fun etag(dachaEtag: String, features: List<CacheEnvironmentFeature>?, now: Instant): String {
    val boundaries = boundaries(features)

    if (boundaries.isEmpty()) {
      return dachaEtag
    }

    return "${dachaEtag}_t${boundaries.count { !now.isBefore(it) }}"
  }

  /**
   * The distinct instants at which a clock based strategy in these features could change its result.
   */
  fun boundaries(features: List<CacheEnvironmentFeature>?): Set<Instant> {
    val boundaries = mutableSetOf<Instant>()

    features?.forEach { feature ->
      feature.value?.rolloutStrategies?.forEach { strategy ->
        strategy.attributes?.forEach { attr -> addBoundaries(attr, boundaries) }
      }
    }

    return boundaries
  }

  private fun addBoundaries(attr: CacheRolloutStrategyAttribute, boundaries: MutableSet<Instant>) {
    val values = attr.values ?: return

    if (WhenActiveMatcher.isWhenActive(attr.fieldName)) {
      // fhubWhenActive is a fixed point in time T from the attribute (never the context). T and the clock are compared
      // to the whole second, so the clock is before T, in T's second, or after it: the result can only change at T and
      // one second later. Conditionals other than the comparisons never match, whatever the time, so they add nothing.
      if (attr.conditional !in WHEN_ACTIVE_CONDITIONALS) return

      values.filterNotNull().mapNotNull { WhenActiveMatcher.parse(it) }.forEach {
        boundaries.add(it)
        boundaries.add(it.plusSeconds(1))
      }
    } else if (NOW.equals(attr.fieldName, ignoreCase = true)) {
      when (attr.type) {
        // the DATETIME matcher compares to the second
        RolloutStrategyFieldType.DATETIME -> values.filterNotNull().mapNotNull { WhenActiveMatcher.parse(it) }.forEach {
          val second = it.truncatedTo(ChronoUnit.SECONDS)
          boundaries.add(second)
          boundaries.add(second.plusSeconds(1))
        }

        // the DATE matcher compares the UTC date
        RolloutStrategyFieldType.DATE -> values.filterNotNull().mapNotNull { parseDate(it) }.forEach {
          boundaries.add(it.atStartOfDay(ZoneOffset.UTC).toInstant())
          boundaries.add(it.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())
        }

        else -> {}
      }
    }
  }

  private fun parseDate(value: Any): LocalDate? {
    return try {
      // as the DATE matcher reads its values
      LocalDate.from(DateTimeFormatter.ISO_DATE.parse(value.toString()))
    } catch (e: Exception) {
      null
    }
  }
}
