package io.featurehub.db.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.featurehub.db.api.SystemConfigApi
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbSystemConfig
import io.featurehub.db.model.query.QDbSystemConfig
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.mr.model.SystemConfig
import io.featurehub.mr.model.UpdatedSystemConfig
import io.featurehub.systemcfg.KnownSystemConfigSource
import io.featurehub.systemcfg.ValidSystemConfig
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


/**
 * this one will decrypt if requested
 */
interface InternalSystemConfigApi {
  fun findConfig(key: String, orgId: UUID, configDefinitions: List<ValidSystemConfig>): DbSystemConfig?
  fun findConfigs(vararg keys: String, orgId: UUID, configDefinitions: List<ValidSystemConfig>): Map<String, SystemConfig>
  fun findConfigs(keys: Collection<String>, orgId: UUID, configDefinitions: List<ValidSystemConfig>): Map<String, SystemConfig>
  fun mapVal(config: DbSystemConfig, configDefinitions: List<ValidSystemConfig>): SystemConfig?
}

class InternalSystemConfigSqlApi : InternalSystemConfigApi {
  private val mapper = ObjectMapper().apply {
    registerModule(KotlinModule.Builder().build())
      .registerModule(JavaTimeModule())
  }

  override fun findConfig(key: String, orgId: UUID, configDefinitions: List<ValidSystemConfig>): DbSystemConfig? {
    return QDbSystemConfig().id.key.eq(key).id.orgId.eq(orgId).findOne()
  }

  override fun findConfigs(vararg keys: String, orgId: UUID, configDefinitions: List<ValidSystemConfig>): Map<String, SystemConfig> {
    return QDbSystemConfig().id.key.`in`(keys.toList()).id.orgId.eq(orgId).findList().mapNotNull { mapVal(it, configDefinitions) }
      .associateBy { it.key }
  }

  override fun findConfigs(keys: Collection<String>, orgId: UUID, configDefinitions: List<ValidSystemConfig>): Map<String, SystemConfig> {
    return QDbSystemConfig().id.key.`in`(keys).id.orgId.eq(orgId).findList().mapNotNull { mapVal(it, configDefinitions) }.associateBy { it.key }
  }

  override fun mapVal(config: DbSystemConfig, configDefinitions: List<ValidSystemConfig>): SystemConfig? {
    val sysCfg = configDefinitions.find { it.key == config.key }
    if (sysCfg == null) return null // may not exist any more

    return SystemConfig()
      .key(config.key)
      .encrypted(sysCfg.requiresEncryption)
      .value(mapVal(config.value, sysCfg.dataType))
  }

  private fun mapVal(config: String?, ref: TypeReference<*>): Any? {
    if (config == null) return null

    return mapper.readValue(config, ref)
  }

}

data class SystemConfigChange(var original: SystemConfig?, val updated: SystemConfig)

class SystemConfigSqlApi @Inject constructor(
  private val conversions: Conversions,
  private val configProvider: IterableProvider<KnownSystemConfigSource>,
  private val encryptionService: WebhookEncryptionService
) : SystemConfigApi {
  private val mapper = ObjectMapper().apply {
    registerModule(KotlinModule.Builder().build())
      .registerModule(JavaTimeModule())
  }

  private val systemConfigMap =
    configProvider.map { it.knownConfig }.flatten().associateBy { it.key }
  private val knownConfigs = configProvider.map { it.name.lowercase() }.toSet()

  init {
    log.info("system-config sections enabled: {}", knownConfigs)
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(SystemConfigSqlApi::class.java)
  }

  override fun updateConfigs(configs: List<UpdatedSystemConfig>, whoUpdated: UUID): List<SystemConfig> {

    // check for invalid entries
    configProvider.forEach { provider ->
      provider.presaveUpdateCheck(configs, whoUpdated)?.let { failure ->
        throw SystemConfigApi.UpdateSystemConfigFailedException(failure)
      }
    }

    val updatedBy = conversions.byPerson(whoUpdated)!!
    val originalConfigs = findConfigs(configs.map { it.key })
    val updatedConfigs =
      configs.mapNotNull {
        val systemConfig = systemConfigMap[it.key]
        val updateResult = if (systemConfig == null) null else mapToStoredValue(it, systemConfig)
        if (updateResult == null || updateResult.skip) null else updateConfig(it, updatedBy, updateResult)
      }.map { Pair(it.key, SystemConfigChange(null, it)) }.toMap()

    originalConfigs.forEach { cfg ->
      updatedConfigs[cfg.key]?.let {
        it.original = cfg
      }
    }

    // not yet required
//    configProvider.forEach {  it.configUpdateCheck(updatedConfigs) }

    return updatedConfigs.values.map { it.updated }
  }

  inner class UpdateResult(val value: String?, val skip: Boolean)

  fun updateConfig(config: UpdatedSystemConfig, updatedBy: DbPerson, updateResult: UpdateResult): SystemConfig? {
    if (!systemConfigMap.containsKey(config.key)) {
      return null
    }

    val org = conversions.organizationId()
    val cfg = QDbSystemConfig().id.key.eq(config.key).id.orgId.eq(org).findOne()
    if (cfg == null) {
      // in  this case, it has never been seen before by the database (i.e. we never stored it), so
      // we now need to store it.

      val newCfg = DbSystemConfig(config.key, org, updatedBy).apply {

        updateResult.value?.let {
          value = it
        }

        save()
      }

      return mapVal(newCfg)
    }

    if (cfg.version != config.version) {
      return null
    }

    cfg.apply {
      value = updateResult.value
      whoUpdated = updatedBy
      save()
    }

    return mapVal(cfg)
  }

  fun mapToStoredValue(config: UpdatedSystemConfig, definition: ValidSystemConfig): UpdateResult {
    var updatedVal = config.value ?: return UpdateResult(null, false)

    if (definition.requiresEncryption && updatedVal == WebhookEncryptionService.ENCRYPTEDTEXT) {
      return UpdateResult(null, true) // don't bother using this, it hasn't actually changed
    }

    if (definition.requiresEncryption) {
      if (definition.dataType == KnownSystemConfigSource.encryptableHeaderRef) {
        if (updatedVal is Map<*,*>) {
          val value = updatedVal as Map<String,String>
          updatedVal = encryptionService.encrypt(value)
        }
      } else if (definition.dataType == KnownSystemConfigSource.stringRef) {
        updatedVal = encryptionService.encryptSingle(updatedVal.toString())!!
      }
    }

    return UpdateResult(mapper.writeValueAsString(updatedVal), false)
  }

  override fun findConfigs(filters: List<String>): List<SystemConfig> {
    if (filters.isEmpty()) {
      return systemConfigMap.values.map { SystemConfig().key(it.key).version(-1).encrypted(it.requiresEncryption) }
    }

    val foundKeys = filters.map { filter ->
      QDbSystemConfig().id.key.startsWith(filter.lowercase()).findList().map(this::mapVal)
    }.flatten().associateBy { it.key }

    // find all those keys that could/should have values but don't
    val valReturn = filters.map { filter ->
      systemConfigMap.values.filter { it.createIfMissing && foundKeys[it.key] == null && it.key.startsWith(filter) }.map {
        SystemConfig().key(it.key).encrypted(it.requiresEncryption).value(it.defaultValue).version(-1)
      }
    }.flatten()

    // now yoink in any read only fields
    val readOnlys = filters.map { filter ->
      configProvider.map { it.readOnlyConfg }.flatten().filter { it.key.startsWith(filter) }.map { SystemConfig().value(it.defaultValue).version(-1).key(it.key).encrypted(false) }
    }.flatten()

    return foundKeys.values + valReturn + readOnlys
  }

  override fun isEnabled(configSection: String): Boolean {
    return knownConfigs.contains(configSection.lowercase())
  }

  override fun decryptSystemConfig(key: String, mapKey: String?): String? {
    val sysConfig = systemConfigMap[key] ?: throw SystemConfigApi.NoSuchKeyException()
    if (!sysConfig.requiresEncryption) return null
    val config = QDbSystemConfig().id.key.eq(key).id.orgId.eq(conversions.organizationId()).findOne() ?: return null
    if (config.value == null) return null
    val value = mapper.readValue(config.value, sysConfig.dataType)

    if (sysConfig.dataType == KnownSystemConfigSource.encryptableHeaderRef) {
      val indexKey = mapKey ?: return null
      val decrypted = encryptionService.decrypt(value as Map<String,String>)
      return decrypted[indexKey]
    } else if (sysConfig.dataType == KnownSystemConfigSource.stringRef) {
      return encryptionService.decryptSingle(value.toString())
    }

    return null
  }

  /**
   * These are for public use, so once the key is encrypted, it won't decrypt
   */
  fun mapVal(config: DbSystemConfig): SystemConfig {
    val sysConfig = systemConfigMap[config.key]
    val encrypted = sysConfig?.requiresEncryption ?: false
    val value = if (config.value == null) null else (if (encrypted)
      mapEncryptedVal(config.value, sysConfig?.dataType ?: KnownSystemConfigSource.stringRef)
      else mapVal(config.value, sysConfig?.dataType ?: KnownSystemConfigSource.stringRef))
    return SystemConfig()
      .key(config.key)
      .encrypted(encrypted)
      .version(config.version)
      .value(value)
  }

  private fun mapEncryptedVal(json: String, ref: TypeReference<*>): Any? {
    if (ref == KnownSystemConfigSource.stringRef) {
      return WebhookEncryptionService.ENCRYPTEDTEXT
    } else if (ref == KnownSystemConfigSource.encryptableHeaderRef) {
      val map = mapper.readValue(json, KnownSystemConfigSource.encryptableHeaderRef)
      val data = encryptionService.filterAndReplaceWithPlaceholder(map)
      if (!data.containsKey("slack.encrypted")) {
        data["slack.encrypted"] = ""
      }
      return data
    }

    return null
  }

  private fun mapVal(json: String, ref: TypeReference<*>): Any {
    return mapper.readValue(json, ref)
  }
}
