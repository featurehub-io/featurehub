package io.featurehub.db.services

import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.db.api.FeatureGroupApi
import io.featurehub.db.model.*
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbFeatureGroup
import io.featurehub.db.model.query.QDbFeatureGroupFeature
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.db.model.query.QFeatureGroupOrderHighest
import io.featurehub.mr.model.*
import io.featurehub.mr.model.RoleType
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * We never check the appId as we assume from the front end that it always does that as part of its
 * permissions check. We do not check permissions either.
 */
class FeatureGroupSqlApi @Inject constructor(
  private val conversions: Conversions,
) : FeatureGroupApi {
  private val log: Logger = LoggerFactory.getLogger(FeatureGroupSqlApi::class.java)

  override fun createGroup(appId: UUID, current: Person, featureGroup: FeatureGroupCreate): FeatureGroup? {

    val env =
      QDbEnvironment().id.eq(featureGroup.environmentId).parentApplication.id.eq(appId).findOne() ?: return null
    val person = conversions.byPerson(current.id?.id) ?: return null
    val mappedFeatures =
      QDbApplicationFeature().parentApplication.id.eq(appId).id.`in`(featureGroup.features.map { it.id }).whenArchived.isNull.findList()

    if (mappedFeatures.size != featureGroup.features.size) {
      log.trace("Features passed do not belong to the application")
      return null
    }

    val highest: Int =
      (QFeatureGroupOrderHighest()
          .select(QFeatureGroupOrderHighest.Alias.highest)
          .environmentId.eq(env.id).findSingleAttribute() ?: 0) + 1

    // we have to create it without the features as we don't know the ID yet
    val fg = createFeatureGroup(env, person, mappedFeatures, highest, featureGroup)
    fg.refresh()
    return toFeatureGroup(fg)
  }

  @Transactional
  private fun createFeatureGroup(
    env: DbEnvironment,
    person: DbPerson,
    mappedFeatures: List<DbApplicationFeature>,
    highest: Int,
    featureGroup: FeatureGroupCreate
  ): DbFeatureGroup {
    val fg = with(DbFeatureGroup(featureGroup.name, env)) {
      order = highest
      strategies = featureGroup.strategies ?: mutableListOf()
      description = featureGroup.description

      whoCreated = person
      whoUpdated = person

      save()
      this
    }

    // because we are using an Embedded object here, we need to use the id of the newly created group
    mappedFeatures.forEach { feat ->
      with(DbFeatureGroupFeature(DbFeatureGroupFeatureKey(feat.id, fg.id))) {
        value = featureGroup.features.find { it.id == feat.id }?.value?.toString()

        // boolean features must have a value
        if (feat.valueType == FeatureValueType.BOOLEAN && value == null) {
          value = "false"
        }

        save()
      }
    }

    return fg
  }

  private fun toFeatureGroup(featureGroup: DbFeatureGroup): FeatureGroup {
    val featureIds = featureGroup.features.map { it.feature.id }

    val features = QDbApplicationFeature().id.`in`(featureIds).whenArchived.isNull.findList().map {feature ->
      FeatureGroupFeature().id(feature.id)
        .key(feature.key).type(feature.valueType)
        .name(feature.name)
        .locked(false)
    }.sortedBy { it.key }

    QDbFeatureValue()
      .environment.id.eq(featureGroup.environment.id)
      .feature.id.`in`(featureIds)
      .feature.fetch(QDbApplicationFeature.Alias.key, QDbApplicationFeature.Alias.valueType, QDbApplicationFeature.Alias.id)
      .retired.isFalse.findList()
       .forEach {
        features.find { f -> f.id == it.feature.id }?.let { fv ->
          fv.locked = it.isLocked
          fv.value = cast(it.defaultValue, it.feature.valueType)
        }
      }

    QDbFeatureGroupFeature().key.feature.`in`(featureIds).key.group.eq(featureGroup.id).findList()
      .forEach {
        features.find { f -> f.id == it.key.feature }?.let { fv ->
          fv.value = cast(it.value, it.feature.valueType)
        }
      }

    return FeatureGroup()
      .id(featureGroup.id)
      .order(featureGroup.order)
      .description(featureGroup.description)
      .environmentId(featureGroup.environment.id)
      .environmentName(featureGroup.environment.name)
      .strategies(featureGroup.strategies)
      .name(featureGroup.name)
      .version(featureGroup.version)
      .features(features)
  }

  private fun cast(value: String?, valueType: FeatureValueType): Any? {
    if (value == null) return null
    return when(valueType) {
      FeatureValueType.BOOLEAN -> "true" == value
      FeatureValueType.STRING -> value.toString()
      FeatureValueType.NUMBER -> BigDecimal(value.toString())
      FeatureValueType.JSON -> value.toString()
    }
  }

  @Transactional(type=TxType.REQUIRES_NEW)
  override fun deleteGroup(appId: UUID, current: Person, fgId: UUID): Boolean {
    val found =
      QDbFeatureGroup().id.eq(fgId).environment.parentApplication.id.eq(appId).whenArchived.isNull.forUpdate().findOne()
        ?: return false

    found.whenArchived = Instant.now()
    found.save()

    return true
  }

  override fun getGroup(appId: UUID, fgId: UUID): FeatureGroup? {
    return QDbFeatureGroup()
      .id.eq(fgId)
      .environment.fetch(QDbEnvironment.Alias.id)
      .environment.parentApplication.id.eq(appId)
      .whenArchived.isNull
      .features.feature.fetch(QDbApplicationFeature.Alias.id)
      .findOne()?.let { toFeatureGroup(it) }
  }

  override fun listGroups(
    appId: UUID,
    maxPerPage: Int,
    filter: String?,
    pageNum: Int,
    sortOrder: SortOrder,
    environmentId: UUID?,
    appPerms: ApplicationPermissions
  ): FeatureGroupList {
    val max = maxPerPage.coerceAtLeast(1).coerceAtMost(100)
    var finder = QDbFeatureGroup()
      .select(
        QDbFeatureGroup.Alias.id, QDbFeatureGroup.Alias.name, QDbFeatureGroup.Alias.order,
        QDbFeatureGroup.Alias.strategies,
        QDbFeatureGroup.Alias.description,
        QDbFeatureGroup.Alias.version,
        QDbFeatureGroup.Alias.environment.id, QDbFeatureGroup.Alias.environment.name,
        QDbFeatureGroup.Alias.features.feature.key
      )
      .environment.id.`in`(appPerms.environments.map { it.id })
      .environment.parentApplication.id.eq(appId) // extra security, relates to SaaS
      .whenArchived.isNull
      .setMaxRows(max)
      .setFirstRow(max * pageNum)
      .features.fetch() // grab the features
      .features.feature.fetch() // grab the app-features

    if (filter != null) {
      finder = finder.name.ilike("%${filter}%")
    }

    environmentId?.let { envId ->
      finder = finder.environment.id.eq(envId)
    }

    finder = if (sortOrder == SortOrder.ASC) {
      finder.order.asc()
    } else {
      finder.order.desc()
    }

    val items = finder.findFutureList()
    val count = finder.findCount()

    return FeatureGroupList()
      .count(count)
      .featureGroups(
        items.get().map {
          FeatureGroupListGroup().id(it.id).name(it.name)
            .order(it.order).environmentId(it.environment.id).environmentName(it.environment.name)
            .version(it.version)
            .hasStrategy(it.strategies.isNotEmpty())
            .description(it.description)
            .features((it.features.map { feat -> FeatureGroupListFeature().key(feat.feature.key) }).sortedBy { sb -> sb.key })
        }
      )
  }

  override fun updateGroup(
    appId: UUID,
    current: Person,
    fgId: UUID,
    update: FeatureGroupUpdate
  ): FeatureGroup? {
    val group = QDbFeatureGroup().id.eq(fgId).environment.parentApplication.id.eq(appId).findOne() ?: return null

    if (group.version != update.version) throw FeatureGroupApi.OptimisticLockingException()

    // we need to keep a track of this so we can publish it out
    var nameChanged = false

    update.name?.let {newName ->
      if (newName != group.name) {
        group.name = newName
        nameChanged = true
      }
    }

    var descChanged = false

    update.description?.let { newDesc ->
      if (newDesc != group.description) {
        group.description = newDesc
        descChanged = true
      }
    }

    val originalEnvironment = group.environment
    var environmentChanged = false

    update.environmentId?.let { envId ->
      QDbEnvironment().parentApplication.id.eq(appId).id.eq(envId).findOne()?.let { env ->
        if (group.environment.id != envId) {
          group.environment = env
          environmentChanged = true
        }
      }
    }

    var strategyChanged = false
    // are they changing the strategy - they can never change it back to "null"
    update.strategies?.let { newStrat ->
      if (group.strategies == null || newStrat != group.strategies) {
        strategyChanged = true
        group.strategies = newStrat
      }
    }

    val updates = FeatureUpdates(mutableListOf(), mutableListOf(), mutableListOf())

    update.features?.let { newFeatList ->
      val newFeatures = newFeatList.toMutableList()
      // go through the existing list
      group.features.forEach { feat ->
        val found = newFeatures.find { it.id == feat.feature.id }
        if (found == null) { // existing feature not in the list
          updates.deletedFeatures.add(feat)
        } else { // it is there already
          val newVal = found.value?.toString() ?: (if (feat.feature.valueType == FeatureValueType.BOOLEAN) "false" else null)

          if (feat.value != newVal) {
            feat.value = newVal
            updates.updatedFeatures.add(feat)
          }

          newFeatures.remove(found)
        }
      }

      // we should now have just the new features
      if (newFeatures.isNotEmpty()) {
        val actualNewFeatures = QDbApplicationFeature().id.`in`(newFeatures.map { it.id }).parentApplication.id.eq(appId).findList().toMutableList()
        updates.addedFeatures.addAll(actualNewFeatures.map { feat ->
          val found = newFeatures.find { it.id == feat.id }
          val newVal = found?.value?.toString() ?: (if (feat.valueType == FeatureValueType.BOOLEAN) "false" else null)
          with(DbFeatureGroupFeature(DbFeatureGroupFeatureKey(feat.id, group.id))) {
            value = newVal
            this
          }
        })
      }
    }

    if (nameChanged || environmentChanged || strategyChanged || descChanged || updates.updatedFeatures.isNotEmpty() || updates.deletedFeatures.isNotEmpty() || updates.addedFeatures.isNotEmpty()) {
      updateGroup(group, updates)
    }

    group.refresh()
    return toFeatureGroup(group)
  }

  override fun getFeaturesForEnvironment(appId: UUID, envId: UUID): List<FeatureGroupFeature> {
    val features = QDbApplicationFeature().parentApplication.id.eq(appId).whenArchived.isNull.findList().map {feature ->
      FeatureGroupFeature().id(feature.id)
        .key(feature.key).type(feature.valueType)
        .name(feature.name)
        .locked(false)
    }.sortedBy { it.key }

    QDbFeatureValue()
      .environment.id.eq(envId)
      .feature.whenArchived.isNull
      .feature.fetch(QDbApplicationFeature.Alias.id)
      .retired.isFalse.findList().forEach {
        features.find { f -> f.id == it.feature.id }?.let { fv ->
          fv.locked = it.isLocked
          fv.value = cast(it.defaultValue, it.feature.valueType)
        }
      }

    return features
  }

  @Transactional
  private fun updateGroup(group: DbFeatureGroup, features: FeatureUpdates) {
    group.save()

    features.deletedFeatures.forEach { it.delete() }
    features.updatedFeatures.forEach { it.update() }
    features.addedFeatures.forEach { it.save() }
  }
}

private data class FeatureUpdates(val deletedFeatures: MutableList<DbFeatureGroupFeature>, val addedFeatures: MutableList<DbFeatureGroupFeature>, val updatedFeatures: MutableList<DbFeatureGroupFeature>)
