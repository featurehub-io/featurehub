package io.featurehub.systemcfg

import com.fasterxml.jackson.core.type.TypeReference
import io.featurehub.db.services.SystemConfigChange
import io.featurehub.mr.model.UpdatedSystemConfig
import java.util.UUID

/**
 * These represent key values a user _can_ change
 */
data class ValidSystemConfig(
  val key: String,
  val description: String,
  val requiresEncryption: Boolean,
  val dataType: TypeReference<*>,
  val createIfMissing: Boolean,
  val defaultValue: Any?, // has to be of the format specified in dataType,
  val internalOnly: Boolean = false
)

/**
 * These represent data fields that the user cannot change but are passed back when they are asked for and match
 * the key pattern that have requested
 */
data class ReadOnlySystemConfig(
  val key: String,
  val description: String,
  val defaultValue: Any?
)

interface KnownSystemConfigSource {
  /**
   * if there is anything wrong, it returns a text string of the problem
   */
  fun presaveUpdateCheck(changes: List<UpdatedSystemConfig>, orgId: UUID): String?

  /**
   * not currently used, but could be? if a subsystem needs to know if something has happened.
   */
  fun configUpdateCheck(changes: Map<String, SystemConfigChange>, orgId: UUID)

  /**
   * This is the name which other areas of the code can use to determine if this configuration source is even available. e.g. for Slack,
   * if there is no Encryption enabled, then Slack support won't be wired in.
   */
  val name: String

  /**
   * A collection of known config
   */
  val knownConfig: List<ValidSystemConfig>

  /**
   * This is usually determined later on once all appropriate injections have happened so it is called rather than provided.
   */
  val readOnlyConfg: List<ReadOnlySystemConfig>

  /**
   * These are shared so knowing how values are stored & retrieved is easy
   */
  companion object {
    val boolRef = object : TypeReference<Boolean>() {}
    val stringRef = object : TypeReference<String>() {}
    val encryptableHeaderRef =
      object : TypeReference<Map<String, String>>() {} // encrypted assume key is clear, value is encrypted
  }
}
