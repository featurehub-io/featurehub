package io.featurehub.systemcfg

import com.fasterxml.jackson.core.type.TypeReference
import io.featurehub.db.services.SystemConfigChange
import java.util.UUID

data class ValidSystemConfig(val key: String, val description: String, val requiresEncryption: Boolean, val dataType: TypeReference<*>)

interface KnownSystemConfigSource {
  fun configUpdateCheck(changes: Map<String, SystemConfigChange>, orgId: UUID)

  val knownConfig: List<ValidSystemConfig>

  companion object {
    val boolRef = object: TypeReference<Boolean>() {}
    val stringRef = object: TypeReference<String>() {}
    val mapStringStringRef = object: TypeReference<Map<String,String>>() {} // encrypted assume key is clear, value is encrypted
  }
}
