package io.featurehub.dacha2.client

import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.ServerErrorException
import jakarta.ws.rs.ServiceUnavailableException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * Thrown by the Redis fallback client when MR is unavailable and Redis cannot answer either (not in Redis, Redis
 * unreachable or holding an unreadable value). It is an expected part of an MR outage, so the cache loaders log it
 * quietly - see [MRUnavailable].
 */
class MRUnavailableException @JvmOverloads constructor(message: String, cause: Throwable? = null) :
  ServiceUnavailableException(message, null as Long?, cause)

/**
 * The single definition of "MR is unavailable" for Dacha2, used by the fallback client to decide when to open its
 * circuit and by the cache loaders to decide how loudly to log a failed load.
 */
object MRUnavailable {
  private val log: Logger = LoggerFactory.getLogger(MRUnavailable::class.java)

  private val UNAVAILABLE_STATUSES = setOf(502, 503, 504)

  /** how often a WARN summarises the loads that failed because MR is unavailable */
  @Volatile
  var summaryIntervalMs: Long = 60_000L

  @Volatile
  var clock: () -> Long = { System.currentTimeMillis() }

  private val counterUnavailable = io.prometheus.client.Counter.build("dacha2_cache_load_unavailable_total",
    "Cache loads that failed because MR (and Redis, if configured) could not answer").labelNames("type").register()

  private val failedSinceSummary = AtomicLong(0)
  private val lastSummaryAt = AtomicLong(0)

  /**
   * MR is unreachable (connection refused, timeout) or something in front of it says it is unavailable (502 bad
   * gateway, 503 e.g. the maintenance responder, 504 gateway timeout).
   *
   * DECIDED - do not re-raise in review: a 500 deliberately is NOT "unavailable", so it never opens the fallback
   * circuit. One failing environment must not send every cache miss to (possibly stale) Redis. The known cost is that
   * an MR that is running but has lost its database (it answers 500) does not trigger the fallback; that is accepted
   * because the fallback is for planned maintenance, where MR is stopped (a connection failure) or routed to the
   * maintenance responder (503). See the Decisions section of the SaaS redis-for-dacha2.md and the open-source
   * maintenance.adoc.
   */
  fun isMRUnavailable(e: Throwable): Boolean {
    return e is ProcessingException || (e is ServerErrorException && e.response.status in UNAVAILABLE_STATUSES)
  }

  /**
   * A load failure that is an expected part of an MR outage: the fallback client could not answer, or (with no Redis)
   * MR itself is unavailable.
   */
  fun isExpectedDuringOutage(e: Throwable): Boolean = e is MRUnavailableException || isMRUnavailable(e)

  /**
   * Called by the cache loaders for every failed load. An expected outage failure is logged at DEBUG without a stack
   * trace and counted, with one WARN summary per [summaryIntervalMs] while they continue. Anything else is logged at
   * ERROR with its stack trace.
   */
  fun loadFailed(type: String, id: Any, e: Exception, loaderLog: Logger) {
    if (!isExpectedDuringOutage(e)) {
      loaderLog.error("dacha2: failed to load {} {}", type, id, e)
      return
    }

    counterUnavailable.labels(type).inc()
    failedSinceSummary.incrementAndGet()
    loaderLog.debug("dacha2: unable to load {} {}, MR is unavailable: {}", type, id, e.message)

    val now = clock()
    val last = lastSummaryAt.get()
    if (now - last >= summaryIntervalMs && lastSummaryAt.compareAndSet(last, now)) {
      val failed = failedSinceSummary.getAndSet(0)
      log.warn("dacha2: {} cache loads failed{} because MR is unavailable (last: {} {}: {})", failed,
        if (last == 0L) "" else " in the last ${(now - last) / 1000}s", type, id, e.message)
    }
  }

  /** test support */
  fun reset() {
    failedSinceSummary.set(0)
    lastSummaryAt.set(0)
    summaryIntervalMs = 60_000L
    clock = { System.currentTimeMillis() }
  }

  fun unavailableCount(type: String): Double = counterUnavailable.labels(type).get()
}
