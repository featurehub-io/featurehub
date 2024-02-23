package io.featurehub.db.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.featurehub.db.api.SystemConfigApi
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbSystemConfig
import io.featurehub.db.model.query.QDbSystemConfig
import io.featurehub.encryption.WebhookEncryptionServiceImpl
import io.featurehub.mr.model.SystemConfig
import io.featurehub.mr.model.UpdatedSystemConfig
import io.featurehub.systemcfg.KnownSystemConfigSource
import io.featurehub.systemcfg.ValidSystemConfig
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider
import java.util.*


interface InternalSystemConfigApi {
  fun findConfig(key: String, orgId: UUID): DbSystemConfig?
  fun findConfigs(vararg keys: String, orgId: UUID): Map<String, SystemConfig>
  fun findConfigs(keys: Collection<String>, orgId: UUID): Map<String, SystemConfig>
}

data class SystemConfigChange(var original: SystemConfig?, val updated: SystemConfig)

class SystemConfigSqlApi @Inject constructor(private val conversions: Conversions, private val configProvider: IterableProvider<KnownSystemConfigSource>) : SystemConfigApi, InternalSystemConfigApi {
  private val mapper = ObjectMapper().apply { registerModule(KotlinModule.Builder().build())
    .registerModule(JavaTimeModule()) }

  val systemConfigMap: Map<String, ValidSystemConfig> = configProvider.map { it.knownConfig }.flatten().associateBy { it.key }

  override fun updateConfigs(configs: List<UpdatedSystemConfig>, whoUpdated: UUID): List<SystemConfig> {
    val updatedBy = conversions.byPerson(whoUpdated)!!
    val originalConfigs = findConfigs(configs.map { it.key })
    val updatedConfigs = configs.mapNotNull { updateConfig(it, updatedBy) }.map { Pair(it.key, SystemConfigChange(null, it)) }.toMap()

    originalConfigs.forEach { cfg ->
      updatedConfigs[cfg.key]?.let {
        it.original = cfg
      }
    }

    // not yet required
//    configProvider.forEach {  it.configUpdateCheck(updatedConfigs) }

    return updatedConfigs.values.map { it.updated }
  }

  fun updateConfig(config: UpdatedSystemConfig, updatedBy: DbPerson): SystemConfig? {
    if (!systemConfigMap.containsKey(config.key)) {
      return null
    }

    val org = conversions.organizationId()
    val cfg = QDbSystemConfig().id.key.eq(config.key).id.orgId.eq(org).version.eq(config.version).findOne()
    if (cfg == null) {
      val newCfg = DbSystemConfig(config.key, org, updatedBy).apply {
        config.value?.let {
          value = mapper.writeValueAsString(it)
        }

        save()
      }

      return mapVal(newCfg)
    }

    cfg.apply {
      value = if (config.value == null) null else mapper.writeValueAsString(config.value)
      whoUpdated = updatedBy
      save()
    }

    return mapVal(cfg)
  }

  override fun findConfigs(filters: List<String>): List<SystemConfig> {
    return filters.map{ filter ->
      QDbSystemConfig().id.key.startsWith(filter.lowercase()).findList().map(this::mapVal)
    }.flatten()
  }

  fun mapVal(config: DbSystemConfig): SystemConfig  {
    val encrypted = systemConfigMap[config.key]?.requiresEncryption ?: false
    return SystemConfig()
      .key(config.key)
      .encrypted(encrypted)
      .value(if (encrypted) WebhookEncryptionServiceImpl.ENCRYPTEDTEXT else mapVal(config.value) )
  }

  private fun mapVal(json: String): Any {
    return mapper.readValue(json, Object::class.java)
  }

  override fun findConfig(key: String, orgId: UUID): DbSystemConfig? {
    return QDbSystemConfig().id.key.eq(key).id.orgId.eq(orgId).findOne()
  }

  override fun findConfigs(vararg keys: String, orgId: UUID): Map<String, SystemConfig> {
    return QDbSystemConfig().id.key.`in`(keys.toList()).id.orgId.eq(orgId).findList().map { mapVal(it) }.associateBy { it.key }
  }

  override fun findConfigs(keys: Collection<String>, orgId: UUID): Map<String, SystemConfig> {
    return QDbSystemConfig().id.key.`in`(keys).id.orgId.eq(orgId).findList().map { mapVal(it) }.associateBy { it.key }
  }

  private fun findConfigs(keys: List<String>, orgId: UUID): List<SystemConfig> {
    return QDbSystemConfig().id.key.`in`(keys).id.orgId.eq(orgId).findList().map { mapVal(it) }
  }
}
