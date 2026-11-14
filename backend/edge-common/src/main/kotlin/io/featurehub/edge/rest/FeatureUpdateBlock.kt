package io.featurehub.edge.rest

import io.featurehub.utils.FallbackPropertyConfig
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Lets an operator stop Edge accepting feature updates (the PUT/POST test-sdk requests that FeatureUpdateProcessor
 * turns into StreamedFeatureUpdates for MR) without restarting it - typically while MR is in maintenance, so no
 * feature changes reach MR. While blocked, updates get a 503 with a Retry-After header.
 */
interface FeatureUpdateBlock {
  /**
   * @return null if updates are allowed, otherwise the value of the Retry-After header to send with the 503
   */
  fun blockedRetryAfter(): String?
}

/**
 * Updates are blocked while the file `edge.update-block.file` exists. It is checked every
 * `edge.update-block.check-interval-seconds` (default 30, minimum 1). If the file has any content, its first line is
 * used as the Retry-After value (seconds, or an RFC 1123 date), otherwise `edge.update-block.retry-after`
 * (default 60 seconds).
 *
 * The file is meant to be a key of a mounted (Kubernetes) ConfigMap: adding or removing the key adds or removes the
 * file in a running pod. It must not be mounted with subPath, as those mounts never update.
 *
 * If no file is configured, updates are never blocked and nothing is scheduled.
 */
class FileFeatureUpdateBlock(private val blockFile: String?,
                             checkIntervalSeconds: Long,
                             private val defaultRetryAfter: String) : FeatureUpdateBlock {
  @Inject
  constructor() : this(
    FallbackPropertyConfig.getConfig("edge.update-block.file")?.trim()?.ifBlank { null },
    FallbackPropertyConfig.getConfig("edge.update-block.check-interval-seconds", "30").trim().toLongOrNull()
      ?: DEFAULT_CHECK_INTERVAL_SECONDS,
    FallbackPropertyConfig.getConfig("edge.update-block.retry-after", DEFAULT_RETRY_AFTER).trim()
      .ifBlank { DEFAULT_RETRY_AFTER })

  val checkIntervalSeconds: Long = if (checkIntervalSeconds < MINIMUM_CHECK_INTERVAL_SECONDS) {
    log.error("edge.update-block.check-interval-seconds is {}, which is less than the minimum of {}s - using {}s",
      checkIntervalSeconds, MINIMUM_CHECK_INTERVAL_SECONDS, MINIMUM_CHECK_INTERVAL_SECONDS)
    MINIMUM_CHECK_INTERVAL_SECONDS
  } else checkIntervalSeconds

  @Volatile
  private var retryAfter: String? = null

  private val executor: ScheduledExecutorService? = blockFile?.let {
    Executors.newSingleThreadScheduledExecutor { r ->
      Thread(r, "edge-update-block").apply { isDaemon = true }
    }
  }

  init {
    if (blockFile == null) {
      log.info("edge.update-block.file is not set, feature updates are never blocked")
    } else {
      log.info("feature updates are blocked while {} exists, checked every {}s", blockFile, this.checkIntervalSeconds)
      check()
      executor!!.scheduleWithFixedDelay({ check() }, this.checkIntervalSeconds, this.checkIntervalSeconds, TimeUnit.SECONDS)
    }
  }

  override fun blockedRetryAfter(): String? = retryAfter

  /**
   * reads the file - called on the schedule, public so tests can call it directly
   */
  fun check() {
    val file = blockFile?.let { File(it) } ?: return

    val newRetryAfter = try {
      if (file.isFile) {
        file.readLines().firstOrNull { it.isNotBlank() }?.trim() ?: defaultRetryAfter
      } else null
    } catch (e: Exception) {
      // it exists but we can't read it - an operator put it there, so block rather than let updates through
      log.error("feature update block file {} exists but cannot be read, blocking updates", blockFile, e)
      defaultRetryAfter
    }

    val wasBlocked = retryAfter != null
    retryAfter = newRetryAfter

    if (newRetryAfter != null && !wasBlocked) {
      log.warn("feature updates are now BLOCKED ({} exists), responding 503 with retry-after {}", blockFile, newRetryAfter)
    } else if (newRetryAfter == null && wasBlocked) {
      log.warn("feature updates are now allowed again ({} has gone)", blockFile)
    }
  }

  @PreDestroy
  fun close() {
    executor?.shutdownNow()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(FileFeatureUpdateBlock::class.java)
    const val DEFAULT_CHECK_INTERVAL_SECONDS = 30L
    const val MINIMUM_CHECK_INTERVAL_SECONDS = 1L
    const val DEFAULT_RETRY_AFTER = "60"
  }
}
