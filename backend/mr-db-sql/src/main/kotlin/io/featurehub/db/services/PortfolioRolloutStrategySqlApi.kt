package io.featurehub.db.services

import io.ebean.annotation.Transactional
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PortfolioRolloutStrategyApi
import io.featurehub.db.model.DbPortfolioRolloutStrategy
import io.featurehub.db.model.query.QDbPortfolioRolloutStrategy
import io.featurehub.db.model.query.QDbPortfolioStrategyForFeatureValue
import io.featurehub.db.services.ArchiveStrategy.Companion.isoDate
import io.featurehub.mr.model.*
import jakarta.inject.Inject
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class PortfolioRolloutStrategySqlApi @Inject constructor(
  private val conversions: Conversions,
  private val internalFeatureApi: InternalFeatureApi
) : PortfolioRolloutStrategyApi {

  override fun createStrategy(
    appId: UUID,
    rolloutStrategy: CreatePortfolioRolloutStrategy,
    person: UUID,
    opts: Opts
  ): PortfolioRolloutStrategy? {
    val portfolio = conversions.byPortfolio(appId) ?: return null
    val p = conversions.byPerson(person) ?: return null

    val existing = QDbPortfolioRolloutStrategy().portfolio.eq(portfolio)
      .whenArchived.isNull()
      .name.ieq(rolloutStrategy.name)
      .exists()

    if (existing) {
      throw PortfolioRolloutStrategyApi.DuplicateNameException()
    }

    var code = Conversions.strategyCodeGenerator(portfolioStrategyCodePrefix)
    while (QDbPortfolioRolloutStrategy().portfolio.id.eq(appId).shortUniqueCode.eq(code).exists()) {
      code = Conversions.strategyCodeGenerator(portfolioStrategyCodePrefix)
    }

    rolloutStrategy.attributes?.let { rationaliseAttributeIds(it) }
    val strategy = PortfolioRolloutStrategy()
      .id(UUID.randomUUID())
      .name(rolloutStrategy.name)
      .percentage(rolloutStrategy.percentage)
      .percentageAttributes(rolloutStrategy.percentageAttributes)
      .attributes(rolloutStrategy.attributes)
      .colouring(rolloutStrategy.colouring)
      .avatar(rolloutStrategy.avatar)
      .disabled(rolloutStrategy.disabled)

    val rs = with(DbPortfolioRolloutStrategy(portfolio, code, strategy)) {
      whoChanged = p
      name = rolloutStrategy.name
      id = strategy.id
      this
    }

    return try {
      save(rs)
      rs.strategy
    } catch (e: Exception) {
      throw PortfolioRolloutStrategyApi.DuplicateNameException()
    }
  }

  private fun rationaliseAttributeIds(attributes: List<RolloutStrategyAttribute>): Boolean {
    var changes = false

    attributes.forEach { attribute ->
      if (attribute.id == null || attribute.id!!.length > Conversions.strategyIdLength) {
        var id = Conversions.strategyCodeGenerator(portfolioStrategyCodePrefix)
        while (attributes.any { id == attribute.id }) {
          id = Conversions.strategyCodeGenerator(portfolioStrategyCodePrefix)
        }

        changes = true
        attribute.id = id
      }
    }

    return changes
  }

  @Transactional
  private fun save(rs: DbPortfolioRolloutStrategy) {
    rs.save()
  }

  override fun updateStrategy(
    appId: UUID,
    strategyId: UUID,
    update: UpdatePortfolioRolloutStrategy,
    person: UUID,
    opts: Opts
  ): PortfolioRolloutStrategy? {
    val portfolio = conversions.byPortfolio(appId) ?: return null
    val p = conversions.byPerson(person) ?: return null

    val strategy = byStrategy(appId, strategyId, Opts.empty()).findOne() ?: return null
    if (strategy.portfolio.id == portfolio.id) {
      var notifyAttachedFeatures = false

      val originalRolloutStrategy = InternalFeatureApi.toRolloutStrategy(strategy)
      val originalStrategiesAssociatedWithFeatureValues = strategy.sharedRolloutStrategies?.associate { it.featureValue.id to internalFeatureApi.collectFeatureValueStrategies(it.featureValue)  } ?: mapOf()

      update.name?.let { newName ->
        if (!strategy.name.equals(newName, ignoreCase = true)) {
          val existing = QDbPortfolioRolloutStrategy().portfolio
            .eq(portfolio).name
            .ieq(update.name).whenArchived
            .isNull()
            .exists()

          if (existing) {
            throw PortfolioRolloutStrategyApi.DuplicateNameException()
          }
        }

        strategy.name = newName
        strategy.strategy.name = newName
      }

      update.percentage?.let {
        strategy.strategy.percentage = it
        notifyAttachedFeatures = true
      }
      update.percentageAttributes?.let {
        strategy.strategy.percentageAttributes = it
        notifyAttachedFeatures = true
      }
      update.attributes?.let { attr ->
        rationaliseAttributeIds(attr)
        strategy.strategy.attributes = attr
        notifyAttachedFeatures = true
      }
      update.avatar?.let { strategy.strategy.avatar = it }
      update.colouring?.let { strategy.strategy.colouring = it }
      update.disabled?.let { strategy.strategy.disabled = it }

      strategy.whoChanged = p

      return try {
        save(strategy)

        if (notifyAttachedFeatures) {
          // now we have to update all of the tagged features, add an additional history element, force republishing of everything associated
          strategy.sharedRolloutStrategies?.let { strategies ->
            strategies.forEach { strategyForFeatureValue ->
              internalFeatureApi.updatedPortfolioStrategy(strategyForFeatureValue, originalRolloutStrategy, p,
                originalStrategiesAssociatedWithFeatureValues[strategyForFeatureValue.featureValue.id] ?: emptyList()
              )
            }
          }
        }

        strategy.strategy
      } catch (e: Exception) {
        throw PortfolioRolloutStrategyApi.DuplicateNameException()
      }
    } else {
      log.warn("Attempted violation of strategy update by {}", person)
      return null
    }
  }

  @Transactional(readOnly = true)
  override fun listStrategies(
    appId: UUID,
    page: Int,
    max: Int,
    filter: String?,
    includeArchived: Boolean,
    sortOrder: SortOrder?,
    opts: Opts
  ): PortfolioRolloutStrategyList {
    var qRS = QDbPortfolioRolloutStrategy().portfolio.id.eq(appId)

    filter?.let {
      qRS = qRS.name.ilike("%${it.lowercase()}%")
    }

    if (!includeArchived) {
      qRS = qRS.whenArchived.isNull()
    }

    if (opts.contains(FillOpts.SimplePeople)) {
      qRS = qRS.whoChanged.fetch()
    }

    val strategiesRS = if (sortOrder == SortOrder.DESC) {
      qRS.orderBy().name.desc()
    } else {
      qRS.orderBy().name.asc()
    }

    val strategies = strategiesRS.setFirstRow(page * max).setMaxRows(max).findList()
    val count = qRS.findCount()

    return PortfolioRolloutStrategyList().max(count).page(page)
      .items(strategies.map { toListPortfolioRolloutStrategyItem(it, opts) })
  }

  fun toListPortfolioRolloutStrategyItem(rs: DbPortfolioRolloutStrategy, opts: Opts): ListPortfolioRolloutStrategyItem {
    val info = ListPortfolioRolloutStrategyItem()
      .strategy(rs.strategy)
      .uniqueCode(rs.shortUniqueCode)
      .whenUpdated(rs.whenUpdated.atOffset(ZoneOffset.UTC))
      .whenCreated(rs.whenCreated.atOffset(ZoneOffset.UTC))
      .updatedBy(ListPortfolioRolloutStrategyItemUser().name(rs.whoChanged.name).email(rs.whoChanged.email))

    opts.contains(FillOpts.Usage).let {
      val envs = mutableMapOf<UUID, PortfolioRolloutStrategyEnvironment>()

      QDbPortfolioStrategyForFeatureValue()
        .rolloutStrategy.id.eq(rs.id)
        .findList().forEach { sfv ->
          val env = sfv.featureValue.environment
          val app = env.parentApplication
          envs.getOrPut(env.id) {
            PortfolioRolloutStrategyEnvironment()
              .id(env.id)
              .name(env.name)
              .appId(app.id)
              .appName(app.name)
              .featuresCount(0)
          }.let { e -> e.featuresCount = e.featuresCount + 1 }
        }

      info.usage = envs.values.toMutableList()
    }

    return info
  }

  @Transactional(readOnly = true)
  override fun getStrategy(appId: UUID, strategyId: UUID, opts: Opts): PortfolioRolloutStrategy? {
    return byStrategy(appId, strategyId, opts).findOne()?.strategy
  }

  private fun byStrategy(appId: UUID, strategyId: UUID, opts: Opts): QDbPortfolioRolloutStrategy {
    val qRS = QDbPortfolioRolloutStrategy().portfolio.id.eq(appId).whenArchived.isNull().id.eq(strategyId)

    if (opts.contains(FillOpts.SimplePeople)) {
      return qRS.whoChanged.fetch()
    }

    return qRS
  }

  override fun archiveStrategy(appId: UUID, strategyId: UUID, person: UUID): Boolean {
    val p = conversions.byPerson(person) ?: return false
    val strategy = byStrategy(appId, strategyId, Opts.empty()).findOne() ?: return false

    if (strategy.whenArchived == null) {
      val originalRolloutStrategy = InternalFeatureApi.toRolloutStrategy(strategy)
      val originalStrategiesAssociatedWithFeatureValues = strategy.sharedRolloutStrategies?.associate { it.featureValue.id to internalFeatureApi.collectFeatureValueStrategies(it.featureValue)  } ?: mapOf()

      strategy.whenArchived = LocalDateTime.now()
      strategy.name = (strategy.name + Conversions.archivePrefix + isoDate.format(strategy.whenArchived)).take(150)
      strategy.whoChanged = p
      strategy.sharedRolloutStrategies = mutableListOf()
      strategy.save()

      strategy.sharedRolloutStrategies?.let { attachedStrategies ->
        // we are going go and detach all of these, which will require us to create new audit records for this
        val copy = attachedStrategies.toList()

        copy.forEach { strategyForFeatureValue ->
          // this needs to remove the connection, create an audit trail, and publish a new record to Edge, and trigger webhooks
          internalFeatureApi.detachPortfolioStrategy(strategyForFeatureValue, originalRolloutStrategy, p,
            originalStrategiesAssociatedWithFeatureValues[strategyForFeatureValue.featureValue.id] ?: emptyList())
        }
      }
    }

    return true
  }

  companion object {
    private val log = LoggerFactory.getLogger(PortfolioRolloutStrategySqlApi::class.java)
    const val portfolioStrategyCodePrefix = "^"
  }
}
