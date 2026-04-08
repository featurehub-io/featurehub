package io.featurehub.db.services

import io.ebean.DuplicateKeyException
import io.ebean.annotation.Transactional
import io.featurehub.db.api.FeatureFilterApi
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureFilter
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbFeatureFilter
import io.featurehub.db.model.query.QDbPortfolio
import io.featurehub.db.model.query.QDbServiceAccount
import io.featurehub.mr.model.CreateFeatureFilter
import io.featurehub.mr.model.FeatureFilter
import io.featurehub.mr.model.OptionalAnemicPerson
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonType
import io.featurehub.mr.model.SearchFeatureFilterFeature
import io.featurehub.mr.model.SearchFeatureFilterItem
import io.featurehub.mr.model.SearchFeatureFilterResult
import io.featurehub.mr.model.SearchFeatureFilterServiceAccount
import io.featurehub.mr.model.SortOrder
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class FeatureFilterSqlApi @Inject constructor(
  private val convertUtils: Conversions
) : FeatureFilterApi {

  @Throws(FeatureFilterApi.DuplicateNameException::class)
  @Transactional
  override fun create(portfolioId: UUID, creator: Person, filter: CreateFeatureFilter): FeatureFilter {
    val portfolio = QDbPortfolio().id.eq(portfolioId).findOne()
      ?: throw jakarta.ws.rs.NotFoundException("Portfolio not found")
    val whoCreated = convertUtils.byPerson(creator)

    val dbFilter = DbFeatureFilter.Builder()
      .portfolio(portfolio)
      .whoCreated(whoCreated)
      .name(filter.name)
      .description(filter.description)
      .build()

    try {
      dbFilter.save()
    } catch (e: DuplicateKeyException) {
      throw FeatureFilterApi.DuplicateNameException()
    }

    return toFeatureFilter(dbFilter)
  }

  @Throws(FeatureFilterApi.OptimisticLockingException::class, FeatureFilterApi.FilterNotFoundException::class)
  @Transactional
  override fun update(portfolioId: UUID, updater: Person, filter: FeatureFilter): FeatureFilter {
    val dbFilter = QDbFeatureFilter().id.eq(filter.id).portfolio.id.eq(portfolioId).findOne()
      ?: throw FeatureFilterApi.FilterNotFoundException()

    if (dbFilter.version != filter.version.toLong()) {
      throw FeatureFilterApi.OptimisticLockingException()
    }

    dbFilter.name = filter.name
    dbFilter.description = filter.description

    try {
      dbFilter.update()
    } catch (e: DuplicateKeyException) {
      throw FeatureFilterApi.DuplicateNameException()
    }

    return toFeatureFilter(dbFilter)
  }

  @Throws(FeatureFilterApi.OptimisticLockingException::class, FeatureFilterApi.FilterNotFoundException::class)
  @Transactional
  override fun delete(portfolioId: UUID, deleter: Person, filter: FeatureFilter): FeatureFilter {
    val dbFilter = QDbFeatureFilter().id.eq(filter.id).portfolio.id.eq(portfolioId).findOne()
      ?: throw FeatureFilterApi.FilterNotFoundException()

    if (dbFilter.version != filter.version.toLong()) {
      throw FeatureFilterApi.OptimisticLockingException()
    }

    val result = toFeatureFilter(dbFilter)
    dbFilter.delete()
    return result
  }

  override fun find(
    portfolioId: UUID,
    filter: String?,
    max: Int?,
    page: Int?,
    sortOrder: SortOrder?,
    includeDetails: Boolean
  ): SearchFeatureFilterResult {
    val pageSize = max ?: 100
    val pageNum = page ?: 0

    var query = QDbFeatureFilter().portfolio.id.eq(portfolioId)

    if (!filter.isNullOrBlank()) {
      query = query.name.ilike("%$filter%")
    }

    query = if (sortOrder == SortOrder.DESC) {
      query.orderBy().name.desc()
    } else {
      query.orderBy().name.asc()
    }

    if (!includeDetails) {
      // Lightweight path: select only id and name
      val items = query
        .select(QDbFeatureFilter.Alias.id, QDbFeatureFilter.Alias.name)
        .setMaxRows(pageSize)
        .setFirstRow(pageNum * pageSize)
        .findList()
        .map { db ->
          SearchFeatureFilterItem()
            .id(db.id)
            .name(db.name)
            .version(db.version.toInt())
        }

      val total = QDbFeatureFilter().portfolio.id.eq(portfolioId)
        .let { q -> if (!filter.isNullOrBlank()) q.name.ilike("%$filter%") else q }
        .findCount()

      return SearchFeatureFilterResult().max(total).filters(items)
    }

    // Full detail path: fetch with whoCreated
    val dbFilters = query
      .whoCreated.fetch()
      .setMaxRows(pageSize)
      .setFirstRow(pageNum * pageSize)
      .findList()

    val total = QDbFeatureFilter().portfolio.id.eq(portfolioId)
      .let { q -> if (!filter.isNullOrBlank()) q.name.ilike("%$filter%") else q }
      .findCount()

    if (dbFilters.isEmpty()) {
      return SearchFeatureFilterResult().max(total).filters(emptyList())
    }

    // Build features-per-filter map via one query per filter using the inverse association
    val filterIds = dbFilters.map { it.id }
    val featuresByFilter = mutableMapOf<UUID, MutableList<SearchFeatureFilterFeature>>()
    val saByFilter = mutableMapOf<UUID, MutableList<SearchFeatureFilterServiceAccount>>()

    // we don't need to filter by portfolio ID as we control the unique IDs of the portfolios.

    // we should be able to pick up all the service accounts in a single query
    QDbServiceAccount()
      .select(QDbServiceAccount.Alias.id, QDbServiceAccount.Alias.name, QDbServiceAccount.Alias.featureFilters.id)
      .featureFilters.id.`in`(filterIds)
      .findList()
      .forEach({ sa ->
        sa.featureFilters.forEach { filter ->
          saByFilter.getOrPut(filter.id) { mutableListOf() }.add(
            SearchFeatureFilterServiceAccount()
              .id(sa.id)
              .name(sa.name)
          )
        }
    })

    QDbApplicationFeature()
      .select(QDbApplicationFeature.Alias.id, QDbApplicationFeature.Alias.key, QDbApplicationFeature.Alias.parentApplication.id,
        QDbApplicationFeature.Alias.parentApplication.name, QDbApplicationFeature.Alias.filters.id)
      .filters.id.`in`(filterIds)
      .findList().forEach { af ->
        af.filters.forEach { filter ->
          featuresByFilter.getOrPut(filter.id) { mutableListOf() }
            .add(
              SearchFeatureFilterFeature()
                .id(af.id)
                .key(af.key)
                .applicationId(af.parentApplication.id.toString())
                .applicationName(af.parentApplication.name)
            )
        }
      }

    val items = dbFilters.map { db ->
      SearchFeatureFilterItem()
        .id(db.id)
        .name(db.name)
        .description(db.description)
        .version(db.version.toInt())
        .whoCreated(db.whoCreated?.let { p ->
          OptionalAnemicPerson()
            .id(p.id)
            .name(if (p.name.isNullOrEmpty()) "No name" else p.name)
            .email(p.email)
            .type(PersonType.PERSON)
        })
        .features(featuresByFilter[db.id])
        .serviceAccounts(saByFilter[db.id])
    }

    return SearchFeatureFilterResult().max(total).filters(items)
  }

  fun toFeatureFilter(db: DbFeatureFilter): FeatureFilter {
    return FeatureFilter()
      .id(db.id)
      .name(db.name)
      .description(db.description)
      .version(db.version.toInt())
      .whoCreated(db.whoCreated?.let { p ->
        OptionalAnemicPerson()
          .id(p.id)
          .name(if (p.name.isNullOrEmpty()) "No name" else p.name)
          .email(p.email)
          .type(PersonType.PERSON)
      })
  }
}
