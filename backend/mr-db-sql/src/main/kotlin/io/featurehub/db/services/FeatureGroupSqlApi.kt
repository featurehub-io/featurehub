package io.featurehub.db.services

import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.db.api.FeatureGroupApi
import io.featurehub.db.model.*
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbFeatureGroup
import io.featurehub.db.model.query.QFeatureGroupOrderHighest
import io.featurehub.mr.model.*
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * We never check the appId as we assume from the front end that it always does that as part of its
 * permissions check. We do not check permissions either.
 */
class FeatureGroupSqlApi @Inject constructor(private val conversions: Conversions) : FeatureGroupApi {
  private val log: Logger = LoggerFactory.getLogger(FeatureGroupSqlApi::class.java)

  override fun createGroup(appId: UUID, current: Person, featureGroup: FeatureGroupCreate): FeatureGroup? {

    val env =
      QDbEnvironment().id.eq(featureGroup.environmentId).parentApplication.id.eq(appId).findOne() ?: return null;
    val person = conversions.byPerson(current.id?.id) ?: return null
    val mappedFeatures =
      QDbApplicationFeature().parentApplication.id.eq(appId).id.`in`(featureGroup.features.map { it.id }).findList()

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
      strategy = featureGroup.strategy

      whoCreated = person
      whoUpdated = person

      save()
      this
    }

    // because we are using a Emebdded object here, we need to use the id of the newly created group
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
    return FeatureGroup()
      .id(featureGroup.id)
      .environmentId(featureGroup.environment.id)
      .environmentName(featureGroup.environment.name)
      .strategy(featureGroup.strategy)
      .name(featureGroup.name)
      .version(featureGroup.version)
      .features(featureGroup.features?.map {
        FeatureGroupFeature().id(it.feature.id).key(it.feature.key).value(it.value)
      } ?: listOf())
  }

  @Transactional(type=TxType.REQUIRES_NEW)
  override fun deleteGroup(appId: UUID, current: Person, fgId: UUID): Boolean {
    val found =
      QDbFeatureGroup().id.eq(fgId).environment.parentApplication.id.eq(appId).whenArchived.isNull.forUpdate().findOne()
        ?: return false;

    found.whenArchived = Instant.now();
    found.save()

    return true
  }

  override fun getGroup(appId: UUID, current: Person, fgId: UUID): FeatureGroup? {
    return QDbFeatureGroup()
      .id.eq(fgId)
      .environment.parentApplication.id.eq(appId)
      .whenArchived.isNull
      .features.fetch() // grab the features
      .features.feature.fetch() // grab the app-features
      .findOne()?.let { toFeatureGroup(it) }
  }

  override fun listGroups(
    appId: UUID,
    maxPerPage: Int,
    filter: String?,
    pageNum: Int,
    sortOrder: SortOrder
  ): FeatureGroupList {
    val max = maxPerPage.coerceAtLeast(1).coerceAtMost(100)
    var finder = QDbFeatureGroup()
      .select(
        QDbFeatureGroup.Alias.id, QDbFeatureGroup.Alias.name, QDbFeatureGroup.Alias.order,
        QDbFeatureGroup.Alias.environment.id, QDbFeatureGroup.Alias.environment.name,
        QDbFeatureGroup.Alias.features.feature.key
      )
      .environment.parentApplication.id.eq(appId)
      .whenArchived.isNull
      .setMaxRows(max)
      .setFirstRow(max * pageNum)
      .features.fetch() // grab the features
      .features.feature.fetch() // grab the app-features

    if (filter != null) {
      finder = finder.name.ilike("%${filter}%")
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
            .features(it.features?.map { feat -> FeatureGroupListFeature().key(feat.feature.key) } ?: listOf())
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
    update.strategy?.let { newStrat ->
      if (group.strategy == null || newStrat != group.strategy) {
        strategyChanged = true
        group.strategy = newStrat
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
            found.value = newVal
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

    if (nameChanged || environmentChanged || strategyChanged || updates.updatedFeatures.isNotEmpty() || updates.deletedFeatures.isNotEmpty() || updates.addedFeatures.isNotEmpty()) {
      updateGroup(group, updates)
      group.refresh()
    }

    return toFeatureGroup(group)
  }

  @Transactional
  private fun updateGroup(group: DbFeatureGroup, features: FeatureUpdates) {
    group.save()

    features.deletedFeatures.forEach { it.delete() }
    features.updatedFeatures.forEach { it.update() }
    features.addedFeatures.forEach { it.update() }
  }
}

private data class FeatureUpdates(val deletedFeatures: MutableList<DbFeatureGroupFeature>, val addedFeatures: MutableList<DbFeatureGroupFeature>, val updatedFeatures: MutableList<DbFeatureGroupFeature>)
