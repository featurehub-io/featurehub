package io.featurehub.db.services

import io.ebean.Database
import io.ebean.annotation.Transactional
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.RolloutStrategyApi
import io.featurehub.db.model.DbRolloutStrategy
import io.featurehub.db.model.query.QDbRolloutStrategy
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyInfo
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors

class RolloutStrategySqlApi @Inject constructor(
  private val database: Database, private val conversions: Conversions, private val cacheSource: CacheSource
) : RolloutStrategyApi {
  @Throws(RolloutStrategyApi.DuplicateNameException::class)
  override fun createStrategy(
    appId: UUID, rolloutStrategy: RolloutStrategy, person: Person, opts: Opts
  ): RolloutStrategyInfo? {
    requireNotNull(rolloutStrategy) { "rolloutStrategy required" }
    val app = conversions.byApplication(appId) ?: return null
    val p = conversions.byPerson(person) ?: return null
    val existing = QDbRolloutStrategy().application
      .eq(app).whenArchived
      .isNull().name
      .ieq(rolloutStrategy.name)
      .exists()

    if (existing) {
      throw RolloutStrategyApi.DuplicateNameException()
    }

    val rs = DbRolloutStrategy.Builder()
      .application(app)
      .whoChanged(p)
      .strategy(rolloutStrategy)
      .name(rolloutStrategy.name)
      .build()

    return try {
      save(rs)
      conversions.toRolloutStrategy(rs, opts)!!
    } catch (e: Exception) {
      throw RolloutStrategyApi.DuplicateNameException()
    }
  }

  @Transactional
  private fun save(rs: DbRolloutStrategy) {
    database.save(rs)
  }

  @Throws(RolloutStrategyApi.DuplicateNameException::class)
  override fun updateStrategy(
    appId: UUID, rolloutStrategy: RolloutStrategy, person: Person, opts: Opts
  ): RolloutStrategyInfo? {
    require(rolloutStrategy.id != null) { "RolloutStrategy.id is required" }

    val app = conversions.byApplication(appId) ?: return null
    val p = conversions.byPerson(person) ?: return null

    val strategy = byStrategy(appId, rolloutStrategy.id.toString(), Opts.empty()).findOne()  ?: return null
    if (strategy.application.id == app.id) {
      // check if we are renaming it and if so, are we using a duplicate name
      if (!strategy.name.equals(rolloutStrategy.name, ignoreCase = true)) {
        // is there something using the existing name?
        val existing = QDbRolloutStrategy().application
          .eq(app).name
          .ieq(rolloutStrategy.name).whenArchived
          .isNull()
          .exists()

        if (existing) {
          throw RolloutStrategyApi.DuplicateNameException()
        }
      }

      strategy.strategy = rolloutStrategy
      strategy.name = rolloutStrategy.name
      strategy.whoChanged = p

      return try {
        save(strategy)
        cacheSource.publishRolloutStrategyChange(strategy)
        conversions.toRolloutStrategy(strategy, opts)!!
      } catch (e: Exception) {
        throw RolloutStrategyApi.DuplicateNameException()
      }
    } else {
      log.warn("Attempted violation of strategy update by {}", person.id)
      return null
    }
  }

  @Transactional(readOnly = true)
  override fun listStrategies(appId: UUID, includeArchived: Boolean, opts: Opts): List<RolloutStrategyInfo> {
    var qRS = QDbRolloutStrategy().application.id.eq(appId)

    if (!includeArchived) {
      qRS = qRS.whenArchived.isNull()
    }

    if (opts.contains(FillOpts.SimplePeople)) {
      qRS.whoChanged.fetch()
    }

    return qRS.findList()
      .map { rs -> conversions.toRolloutStrategy(rs, opts)!! }
  }

  @Transactional(readOnly = true)
  override fun getStrategy(appId: UUID, strategyIdOrName: String, opts: Opts): RolloutStrategyInfo? {
    requireNotNull(strategyIdOrName) { "strategy name/id required" }

    return conversions.toRolloutStrategy(byStrategy(appId, strategyIdOrName, opts).findOne(), opts)
  }

  private fun byStrategy(appId: UUID, strategyIdOrName: String, opts: Opts): QDbRolloutStrategy {
    val sId = Conversions.checkUuid(strategyIdOrName)
    var qRS = QDbRolloutStrategy().application.id.eq(appId).whenArchived.isNull()
    qRS = if (sId != null) {
      qRS.id.eq(sId)
    } else {
      qRS.name.eq(strategyIdOrName)
    }
    if (opts.contains(FillOpts.SimplePeople)) {
      qRS.whoChanged.fetch()
    }
    return qRS
  }

  override fun archiveStrategy(
    appId: UUID, strategyIdOrName: String, person: Person, opts: Opts
  ): RolloutStrategyInfo? {
    requireNotNull(strategyIdOrName) { "strategy name/id required" }
    val app = conversions.byApplication(appId) ?: return null
    val p = conversions.byPerson(person) ?: return null
    val strategy = byStrategy(appId, strategyIdOrName, Opts.empty()).findOne() ?: return null
    // only update and publish if it _actually_ changed
    if (strategy.whenArchived == null) {
      strategy.whoChanged = p
      strategy.whenArchived = LocalDateTime.now()
      save(strategy)
      cacheSource.publishRolloutStrategyChange(strategy)
    }
    return conversions.toRolloutStrategy(strategy, opts)!!
  }

  companion object {
    private val log = LoggerFactory.getLogger(RolloutStrategySqlApi::class.java)
  }
}
