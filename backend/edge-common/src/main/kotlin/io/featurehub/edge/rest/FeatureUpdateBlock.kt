package io.featurehub.edge.rest

import io.featurehub.utils.FallbackPropertyConfig
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Properties
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
 * Updates are blocked while the file `edge.update-block.file` contains a `blocked` key. The file is read as a Java
 * properties file, so `blocked`, `blocked=` and `blocked: 120` all count. The file existing on its own does not block,
 * so it can be mounted permanently (e.g. a Kubernetes ConfigMap mounted as a directory, not with subPath) and the key
 * added or removed without restarting Edge. It is re-read every `edge.update-block.check-interval-seconds`
 * (default 30, minimum 1) for as long as Edge runs.
 *
 * If the `blocked` value is a number of seconds or an RFC 1123 date it is used as the Retry-After value, otherwise
 * `edge.update-block.retry-after` (default 60 seconds) is.
 *
 * A file that is missing or cannot be read does not block (an unreadable file is logged as an error).
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

  // so a file that stays unreadable is only logged when it first becomes so, not on every check
  private var unreadable = false

  private val executor: ScheduledExecutorService? = blockFile?.let {
    Executors.newSingleThreadScheduledExecutor { r ->
      Thread(r, "edge-update-block").apply { isDaemon = true }
    }
  }

  init {
    if (blockFile == null) {
      log.info("edge.update-block.file is not set, feature updates are never blocked")
    } else {
      log.info("feature updates are blocked while {} has a `{}` key, checked every {}s", blockFile, BLOCKED_KEY,
        this.checkIntervalSeconds)
      check()
      executor!!.scheduleWithFixedDelay({ check() }, this.checkIntervalSeconds, this.checkIntervalSeconds, TimeUnit.SECONDS)
    }
  }

  override fun blockedRetryAfter(): String? = retryAfter

  /**
   * reads the file - called on the schedule, public so tests can call it directly. It never throws, as an exception
   * would stop the scheduled checks.
   */
  @Synchronized
  fun check() {
    val file = blockFile?.let { File(it) } ?: return

    val newRetryAfter = try {
      val result = if (file.isFile) {
        val props = Properties()
        file.reader(Charsets.UTF_8).use { props.load(it) }
        props.getProperty(BLOCKED_KEY)?.let { retryAfterFrom(it) }
      } else null

      if (unreadable) {
        log.info("feature update block file {} can be read again", blockFile)
        unreadable = false
      }

      result
    } catch (e: Exception) {
      if (!unreadable) {
        log.error("feature update block file {} cannot be read, feature updates are not blocked", blockFile, e)
        unreadable = true
      }
      null
    }

    val wasBlocked = retryAfter != null
    retryAfter = newRetryAfter

    if (newRetryAfter != null && !wasBlocked) {
      log.warn("feature updates are now BLOCKED ({} has a `{}` key), responding 503 with retry-after {}", blockFile,
        BLOCKED_KEY, newRetryAfter)
    } else if (newRetryAfter == null && wasBlocked) {
      log.warn("feature updates are now allowed again ({} has no `{}` key)", blockFile, BLOCKED_KEY)
    }
  }

  private fun retryAfterFrom(value: String): String {
    val v = value.trim()

    if (v.isEmpty()) {
      return defaultRetryAfter
    }

    if (v.all { it.isDigit() }) {
      return v
    }

    return try {
      DateTimeFormatter.RFC_1123_DATE_TIME.parse(v)
      v
    } catch (e: DateTimeParseException) {
      defaultRetryAfter
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
    const val BLOCKED_KEY = "blocked"
  }
}
