package io.featurehub.db.services

import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.db.api.FeatureGroupApi
import io.featurehub.db.model.*
import io.featurehub.db.model.query.*
import io.featurehub.db.publish.FeatureGroupHelper
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.*
import jakarta.inject.Inject
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * We never check the appId as we assume from the front end that it always does that as part of its
 * permissions check. We do not check permissions either.
 */
class FeatureGroupSqlApi @Inject constructor(
  private val conversions: Conversions,
  private val cacheSource: CacheSource,
  private val internalFeatureApi: InternalFeatureApi,
  archiveStrategy: ArchiveStrategy
) : FeatureGroupApi {
  private val log: Logger = LoggerFactory.getLogger(FeatureGroupSqlApi::class.java)

  init {
    archiveStrategy.environmentArchiveListener {
      archiveEnvironment(it)
    }
    archiveStrategy.featureListener {
      archiveFeature(it)
    }
  }

  // we are just removing this from any feature groups that may have it,
  // the feature itself is dealing with downstream publication.
  private fun archiveFeature(appFeature: DbApplicationFeature) {
    QDbFeatureGroupFeature().key.feature.eq(appFeature.id).delete()
  }

  fun archiveEnvironment(env: DbEnvironment) {
    // ok, now archive all those feature groups. We don't need to do anything else as the feature values will be deleted across the board
    // because we send a "delete environment" message
    QDbFeatureGroup().environment.id.eq(env.id).whenArchived.isNull.asUpdate()
      .set(QDbFeatureGroup.Alias.whenArchived, Instant.now()).update()
  }

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
        .environmentId.eq(env.id).findSingleAttribute() ?: 0) + 10

    // we have to create it without the features as we don't know the ID yet
    val fg = createFeatureGroup(env, person, mappedFeatures, highest, featureGroup)
    fg.refresh()

    if (mappedFeatures.isNotEmpty() && featureGroup.strategies?.isNotEmpty() == true) {
      republishFeatureValues(fg.environment, featureGroup.features.map { it.id })
    }

    return toFeatureGroup(fg)
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun republishFeatureValues(env: DbEnvironment, featureIds: List<UUID>) {
    internalFeatureApi.forceVersionBump(featureIds, env.id)

    QDbFeatureValue().feature.id.`in`(featureIds).environment.id.eq(env.id).findList().forEach { fv ->
      // we can't push the strategy as this feature may be in several strategies
      cacheSource.publishFeatureChange(fv)
    }
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
      featureGroup.strategies?.let { rationaliseStrategyIdsAndAttributeIds(it) }
      strategies = if (featureGroup.strategies?.isEmpty() == false) featureGroup.strategies else null
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

      ensureFeatureValueExists(feat, env, person)
    }

    return fg
  }

  private fun ensureFeatureValueExists(feat: DbApplicationFeature, env: DbEnvironment, person: DbPerson) {
    val valueExists = QDbFeatureValue().feature.id.eq(feat.id).environment.id.eq(env.id).exists()
    if (!valueExists) {
      internalFeatureApi.saveFeatureValue(DbFeatureValue(person, false, feat, env, null))
    }
  }

  private fun ensureFeatureValuesExist(features: List<DbApplicationFeature>, env: DbEnvironment, person: DbPerson) {
    val values = QDbFeatureValue().select(QDbFeatureValue.Alias.id, QDbFeatureValue.Alias.feature.id)
      .feature.id.`in`(features.map { it.id }).environment.id.eq(env.id).findList().map { it.feature.id }

    features.forEach { feat ->
      if (!values.contains(feat.id)) {
        internalFeatureApi.saveFeatureValue(DbFeatureValue(person, false, feat, env, null))
      }
    }
  }

  private fun toFeatureGroup(featureGroup: DbFeatureGroup): FeatureGroup {
    val featureIds = featureGroup.features.map { it.feature.id }

    val features = QDbApplicationFeature().id.`in`(featureIds).whenArchived.isNull.findList().map { feature ->
      FeatureGroupFeature().id(feature.id)
        .key(feature.key).type(feature.valueType)
        .name(feature.name)
        .locked(false)
    }.sortedBy { it.key }

    QDbFeatureValue()
      .environment.id.eq(featureGroup.environment.id)
      .feature.id.`in`(featureIds)
      .feature.fetch(
        QDbApplicationFeature.Alias.key,
        QDbApplicationFeature.Alias.valueType,
        QDbApplicationFeature.Alias.id
      )
      .retired.isFalse.findList()
      .forEach {
        features.find { f -> f.id == it.feature.id }?.let { fv ->
          fv.locked = it.isLocked
          fv.value = FeatureGroupHelper.cast(it.defaultValue, it.feature.valueType)
        }
      }

    QDbFeatureGroupFeature().key.feature.`in`(featureIds).key.group.eq(featureGroup.id).findList()
      .forEach {
        features.find { f -> f.id == it.key.feature }?.let { fv ->
          fv.value = FeatureGroupHelper.cast(it.value, it.feature.valueType)
        }
      }

    return FeatureGroup()
      .id(featureGroup.id)
      .order(featureGroup.order)
      .description(featureGroup.description ?: "")
      .environmentId(featureGroup.environment.id)
      .environmentName(featureGroup.environment.name)
      .strategies(featureGroup.strategies)
      .name(featureGroup.name)
      .version(featureGroup.version)
      .features(features)
  }

  override fun deleteGroup(appId: UUID, current: Person, fgId: UUID): Boolean {
    val group = deleteSelectedGroup(appId, current, fgId) ?: return false

    if (group.strategies?.isNotEmpty() == true) {
      // now republish these attached features
      republishFeatureValues(group.environment,
        QDbFeatureGroupFeature().select(QDbFeatureGroupFeature.Alias.key.feature).key.group.eq(fgId).findList().map { it.key.feature })
    }

    return true
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun deleteSelectedGroup(appId: UUID, current: Person, fgId: UUID): DbFeatureGroup? {
    val found =
      QDbFeatureGroup().select(QDbFeatureGroup.Alias.id, QDbFeatureGroup.Alias.whenArchived,
            QDbFeatureGroup.Alias.environment, QDbFeatureGroup.Alias.strategies)
        .id.eq(fgId).environment.parentApplication.id.eq(appId).whenArchived.isNull.forUpdate().findOne()
        ?: return null

    found.whenArchived = Instant.now()
    found.whoUpdated = conversions.byPerson(current)
    found.save()
    return found
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
            .hasStrategy(it.strategies?.isNotEmpty() ?: false)
            .description(it.description ?: "")
            .features((it.features.map { feat -> FeatureGroupListFeature().key(feat.feature.key) }).sortedBy { sb -> sb.key })
        }
      )
  }

  override fun updateGroup(
    appId: UUID,
    current: Person,
    update: FeatureGroupUpdate
  ): FeatureGroup? {
    val group = QDbFeatureGroup().id.eq(update.id).environment.parentApplication.id.eq(appId).findOne() ?: return null

    if (group.version != update.version) {
      log.trace("Group {} is version {} and update is version {}, lock error", update.id, group.version, update.version)
      throw FeatureGroupApi.OptimisticLockingException()
    }

    if (group.whenArchived != null) {
      log.trace("Attempt to update archived group")
      throw FeatureGroupApi.ArchivedGroup()
    }

    if (group.environment.whenArchived != null) {
      log.trace("Group {} has an expired environment", group.id)
      throw FeatureGroupApi.ArchivedEnvironment()
    }

    val person = conversions.byPerson(current.id?.id) ?: return null

    var orderChanged = false

    update.order?.let { newOrder ->
      if (newOrder != group.order) {
        if (QDbFeatureGroup().environment.id.eq(group.environment.id).order.eq(newOrder).exists()) {
          log.trace(
            "Attempting to change feature group {} from order {} to order {} and there is already one of that order",
            update.id,
            group.order,
            newOrder
          )
          throw FeatureGroupApi.DuplicateOrder()
        }

        group.order = newOrder
        orderChanged = true
      }
    }

    // we need to keep a track of this, so we can publish it out
    var nameChanged = false

    update.name?.let { newName ->
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

    var strategyChanged = false
    // are they changing the strategy - they can never change it back to "null"
    update.strategies?.let { newStrat ->
      if (group.strategies == null || newStrat != group.strategies) {

        rationaliseStrategyIdsAndAttributeIds(newStrat)
        strategyChanged = !Objects.deepEquals(newStrat, group.strategies)
        if (strategyChanged) {
          group.strategies = newStrat
        }
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
          val newVal =
            found.value?.toString() ?: (if (feat.feature.valueType == FeatureValueType.BOOLEAN) "false" else null)

          if (feat.value != newVal) {
            feat.value = newVal
            updates.updatedFeatures.add(feat)
          }

          newFeatures.remove(found)
        }
      }

      // we should now have just the new features
      if (newFeatures.isNotEmpty()) {
        val actualNewFeatures =
          QDbApplicationFeature().id.`in`(newFeatures.map { it.id }).parentApplication.id.eq(appId).findList()
            .toMutableList()
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

    if (nameChanged || orderChanged || strategyChanged || descChanged || updates.updatedFeatures.isNotEmpty() || updates.deletedFeatures.isNotEmpty() || updates.addedFeatures.isNotEmpty()) {
      log.trace("Updating group {} with updates {}", group, updates)
      updateGroup(group, updates, person)

      group.refresh()
      log.trace("Group refreshed is now {}", group)
      if (strategyChanged) {
        log.trace("Strategy changed so publishing all features {}", group.features)
        republishFeatureValues(group.environment, group.features.map { it.key.feature })
      } else {
        if (updates.updatedFeatures.isNotEmpty()) {
          log.trace("Publishing updated features {}", updates.updatedFeatures)
          republishFeatureValues(group.environment, updates.updatedFeatures.map { it.key.feature })
        }
        if (updates.deletedFeatures.isNotEmpty()) {
          log.trace("Publishing deleted features {}", updates.deletedFeatures)
          republishFeatureValues(group.environment, updates.deletedFeatures.map { it.key.feature })
        }
        if (updates.addedFeatures.isNotEmpty()) {
          log.trace("Publishing added features {}", updates.addedFeatures)
          republishFeatureValues(group.environment, updates.addedFeatures.map { it.key.feature })
        }
      }
    }
    return toFeatureGroup(group)
  }

  private fun randomId(): String {
    return "!${RandomStringUtils.randomAlphanumeric(FeatureSqlApi.strategyIdLength - 1)}"
  }

  private fun rationaliseStrategyIdsAndAttributeIds(strategies: List<GroupRolloutStrategy>): Boolean {
    var changes = false

    strategies.forEach { strategy ->
      if (strategy.id == null || strategy.id!!.length > FeatureSqlApi.strategyIdLength) {
        var id = randomId()
        // make sure it is unique
        while (strategies.any { id == strategy.id }) {
          id = randomId()
        }
        changes = true
        strategy.id = id
      }

      strategy.attributes?.forEach { attribute ->
        if (attribute.id == null || attribute.id!!.length > FeatureSqlApi.strategyIdLength) {
          var id = randomId()
          // make sure it is unique
          while (strategy.attributes!!.any { id == attribute.id }) {
            id = randomId()
          }

          changes = true
          attribute.id = id
        }
      }
    }

    return changes
  }


  override fun getFeaturesForEnvironment(appId: UUID, envId: UUID): List<FeatureGroupFeature> {
    val features =
      QDbApplicationFeature().parentApplication.id.eq(appId).whenArchived.isNull.findList().map { feature ->
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
          fv.value = null // because Irina wants it like this
        }
      }

    return features
  }

  @Transactional
  private fun updateGroup(group: DbFeatureGroup, features: FeatureUpdates, person: DbPerson) {
    group.save()

    features.deletedFeatures.forEach { it.delete() }
    features.updatedFeatures.forEach { it.update() }
    if (features.addedFeatures.isNotEmpty()) {
      features.addedFeatures.forEach { it.save() }

      log.trace("update added features ")
      val parentApp =
        QDbApplication().select(QDbApplication.Alias.id).environments.id.eq(group.environment.id).findOne()
      val feats = features.addedFeatures.mapNotNull {
        QDbApplicationFeature().select(QDbApplicationFeature.Alias.id).id.eq(it.key.feature).parentApplication.eq(
          parentApp
        ).findOne()
      }
      ensureFeatureValuesExist(feats, group.environment, person)
    }
  }
}

private data class FeatureUpdates(
  val deletedFeatures: MutableList<DbFeatureGroupFeature>,
  val addedFeatures: MutableList<DbFeatureGroupFeature>,
  val updatedFeatures: MutableList<DbFeatureGroupFeature>
)
