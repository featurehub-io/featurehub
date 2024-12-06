package io.featurehub.db.services

import io.ebean.annotation.Transactional
import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.ApplicationRolloutStrategyApi
import io.featurehub.db.model.DbApplicationRolloutStrategy
import io.featurehub.db.model.query.QDbApplicationRolloutStrategy
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.*
import jakarta.inject.Inject
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class ApplicationRolloutStrategySqlApi @Inject constructor(
  private val conversions: Conversions, private val cacheSource: CacheSource
) : ApplicationRolloutStrategyApi {

  override fun createStrategy(
    appId: UUID,
    rolloutStrategy: CreateApplicationRolloutStrategy,
    person: UUID,
    opts: Opts
  ): ApplicationRolloutStrategy?  {
    val app = conversions.byApplication(appId) ?: return null
    val p = conversions.byPerson(person) ?: return null

    val existing = QDbApplicationRolloutStrategy().application.eq(app)
      .whenArchived.isNull()
      .name.ieq(rolloutStrategy.name)
      .exists()

    if (existing) {
      throw ApplicationRolloutStrategyApi.DuplicateNameException()
    }

    // make a random code and keep flipping until we find a new unique one. Standard rollout strategies are all alphanumeric,
    // feature group strategies start with a !
    var code = randomId()
    while (QDbApplicationRolloutStrategy().application.id.eq(appId).shortUniqueCode.eq(code).exists()) {
      code = randomId()
    }

    rolloutStrategy.attributes?.let { rationaliseAttributeIds(it) }
    val strategy = ApplicationRolloutStrategy()
      .id(UUID.randomUUID())
      .name(rolloutStrategy.name)
      .percentage(rolloutStrategy.percentage)
      .percentageAttributes(rolloutStrategy.percentageAttributes)
      .attributes(rolloutStrategy.attributes)
      .colouring(rolloutStrategy.colouring)
      .avatar(rolloutStrategy.avatar)
      .disabled(rolloutStrategy.disabled)

    val rs = with(DbApplicationRolloutStrategy(app, code, strategy)) {
      whoChanged = p
      name = rolloutStrategy.name
      id = strategy.id
      this
    }

    return try {
      save(rs)
      conversions.toApplicationRolloutStrategy(rs, opts)!!
    } catch (e: Exception) {
      throw ApplicationRolloutStrategyApi.DuplicateNameException()
    }
  }

  private fun randomId(): String {
    return "@${RandomStringUtils.randomAlphanumeric(FeatureSqlApi.strategyIdLength - 1)}"
  }


  private fun rationaliseAttributeIds(attributes: List<RolloutStrategyAttribute>): Boolean {
    var changes = false

    attributes.forEach { attribute ->
      if (attribute.id == null || attribute.id!!.length > FeatureSqlApi.strategyIdLength) {
        var id = randomId()
        // make sure it is unique
        while (attributes.any { id == attribute.id }) {
          id = randomId()
        }

        changes = true
        attribute.id = id
      }
    }

    return changes
  }


  @Transactional
  private fun save(rs: DbApplicationRolloutStrategy) {
    rs.save()
  }


  override fun updateStrategy(
    appId: UUID,
    strategyId: UUID,
    update: UpdateApplicationRolloutStrategy,
    person: UUID,
    opts: Opts
  ): ApplicationRolloutStrategy? {
    val app = conversions.byApplication(appId) ?: return null
    val p = conversions.byPerson(person) ?: return null

    val strategy = byStrategy(appId, strategyId, Opts.empty()).findOne()  ?: return null
    if (strategy.application.id == app.id) {
      // check if we are renaming it and if so, are we using a duplicate name
      update.name?.let { newName ->
        if (!strategy.name.equals(newName, ignoreCase = true)) {
          // is there something using the existing name?
          val existing = QDbApplicationRolloutStrategy().application
            .eq(app).name
            .ieq(update.name).whenArchived
            .isNull()
            .exists()

          if (existing) {
            throw ApplicationRolloutStrategyApi.DuplicateNameException()
          }
        }

        strategy.name = newName
        strategy.strategy.name = newName
      }

      update.percentage?.let { percent ->
        strategy.strategy.percentage = percent
      }

      update.percentageAttributes?.let { percentAttrs ->
        strategy.strategy.percentageAttributes = percentAttrs
      }

      update.attributes?.let { attr ->
        rationaliseAttributeIds(attr)
        strategy.strategy.attributes = attr
      }

      update.avatar?.let { strategy.strategy.avatar = it }
      update.colouring?.let { strategy.strategy.colouring = it }
      update.disabled?.let { strategy.strategy.disabled = it }

      strategy.whoChanged = p

      return try {
        save(strategy)
        cacheSource.publishApplicationRolloutStrategyChange(PublishAction.UPDATE, strategy)
        conversions.toApplicationRolloutStrategy(strategy, opts)!!
      } catch (e: Exception) {
        throw ApplicationRolloutStrategyApi.DuplicateNameException()
      }
    } else {
      log.warn("Attempted violation of strategy update by {}", person)
      return null
    }
  }

  @Transactional(readOnly = true)
  override fun listStrategies(appId: UUID, page: Int, max: Int, filter: String?, includeArchived: Boolean, opts: Opts): ApplicationRolloutStrategyList {
    var qRS = QDbApplicationRolloutStrategy().application.id.eq(appId).orderBy().name.asc()

    filter?.let {
      qRS = qRS.name.ilike("%${it.lowercase()}%")
    }

    if (!includeArchived) {
      qRS = qRS.whenArchived.isNull()
    }

    if (opts.contains(FillOpts.SimplePeople)) {
      qRS.whoChanged.fetch()
    }

    val strategies = qRS.setFirstRow(page * max).setMaxRows(max).findList()
    val count = qRS.findCount()

    return ApplicationRolloutStrategyList().max(count).page(page)
      .items(strategies.mapNotNull {
        ListApplicationRolloutStrategyItem()
          .strategy(conversions.toApplicationRolloutStrategy(it, opts)!!)
          .whenUpdated(it.whenUpdated.atOffset(ZoneOffset.UTC))
          .whenCreated(it.whenCreated.atOffset(ZoneOffset.UTC))
          .updatedBy(ListApplicationRolloutStrategyItemUser().name(it.whoChanged.name).email(it.whoChanged.email))
      })
  }

  @Transactional(readOnly = true)
  override fun getStrategy(appId: UUID, strategyId: UUID, opts: Opts): ApplicationRolloutStrategy? {
    return conversions.toApplicationRolloutStrategy(byStrategy(appId, strategyId, opts).findOne(), opts)
  }

  private fun byStrategy(appId: UUID, strategyId: UUID, opts: Opts): QDbApplicationRolloutStrategy {
    val qRS = QDbApplicationRolloutStrategy().application.id.eq(appId).whenArchived.isNull().id.eq(strategyId)

    if (opts.contains(FillOpts.SimplePeople)) {
      return qRS.whoChanged.fetch()
    }

    return qRS
  }

  override fun archiveStrategy(appId: UUID, strategyId: UUID, person: UUID): Boolean {
    val p = conversions.byPerson(person) ?: return  false
    val strategy = byStrategy(appId, strategyId, Opts.empty()).findOne() ?: return false

    // only update and publish if it _actually_ changed
    if (strategy.whenArchived == null) {
      strategy.whoChanged = p
      strategy.whenArchived = LocalDateTime.now()
      save(strategy)
      cacheSource.publishApplicationRolloutStrategyChange(PublishAction.DELETE, strategy)
    }

    return true
  }

  companion object {
    private val log = LoggerFactory.getLogger(ApplicationRolloutStrategySqlApi::class.java)
  }
}
