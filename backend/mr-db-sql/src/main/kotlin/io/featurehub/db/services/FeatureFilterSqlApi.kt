package io.featurehub.db.services

import io.ebean.DuplicateKeyException
import io.ebean.annotation.Transactional
import io.featurehub.db.api.FeatureFilterApi
import io.featurehub.db.model.DbFeatureFilter
import io.featurehub.db.model.query.QDbApplication
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbFeatureFilter
import io.featurehub.db.model.query.QDbPerson
import io.featurehub.db.model.query.QDbPortfolio
import io.featurehub.db.model.query.QDbServiceAccount
import io.featurehub.mr.model.CreateFeatureFilter
import io.featurehub.mr.model.FeatureFilter
import io.featurehub.mr.model.MatchingFilterResult
import io.featurehub.mr.model.MatchingFilterResults
import io.featurehub.mr.model.OptionalAnemicPerson
import io.featurehub.mr.model.PaginationResult
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonType
import io.featurehub.mr.model.SearchFeatureFilterApplication
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
  private val convertUtils: Conversions,
  private val internalApplicationSqlApi: InternalApplicationApi,
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
  override fun delete(portfolioId: UUID, deleter: Person, filterId: UUID, version: Int): FeatureFilter {
    val dbFilter = QDbFeatureFilter()
              .id.eq(filterId)
              .portfolio.id.eq(portfolioId)
              .findOne()
      ?: throw FeatureFilterApi.FilterNotFoundException()

    if (dbFilter.version != version.toLong()) {
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
    includeDetails: Boolean,
    personId: UUID,
  ): SearchFeatureFilterResult {
    val pageSize = (max ?: 20).coerceIn(1, 100)
    val pageNum = (page ?: 0).coerceAtLeast(0)

    var query = QDbFeatureFilter().portfolio.id.eq(portfolioId)

    if (!filter.isNullOrBlank()) {
      query = query.name.ilike("%$filter%")
    }

    val simpleQuery = query

    query = if (sortOrder == SortOrder.DESC) {
      query.orderBy().name.desc()
    } else {
      query.orderBy().name.asc()
    }

    if (includeDetails) {
      query = query.whoCreated.fetch(
        QDbPerson.Alias.id, QDbPerson.Alias.name
      )
    }

    // we actually have to find applications they are allowed to access first
    // Lightweight path: select only id and name
    val dbFilters = query
      .select(QDbFeatureFilter.Alias.id, QDbFeatureFilter.Alias.name, QDbFeatureFilter.Alias.description)
      .setMaxRows(pageSize)
      .setFirstRow(pageNum * pageSize)
      .findList()

    // if they want a simple list, return that and be done with it
    if (!includeDetails || dbFilters.isEmpty()) {
      return SearchFeatureFilterResult().pagination(PaginationResult().page(pageNum).pageSize(pageSize).total(simpleQuery.findCount()))
        .filters(dbFilters.map { db ->
          SearchFeatureFilterItem()
            .id(db.id)
            .name(db.name)
            .description(db.description)
            .version(db.version.toInt())
        })
    }

    val filters = dbFilters.associateBy { it.id }

    // now lets find what apps they are allowed to access, which will inform which feature details we return back
    // superadmins can see all apps in the portfolio
    val appQuery =
      internalApplicationSqlApi.findApplicationsUserCanAccess(portfolioId, personId)

    val allowedApps = appQuery
      .select(QDbApplication.Alias.id, QDbApplication.Alias.name)
      .findList().map { app ->
        SearchFeatureFilterApplication().id(app.id).name(app.name)
      }.associateBy { it.id }.toMutableMap()

    val total = QDbFeatureFilter().portfolio.id.eq(portfolioId)
      .let { q -> if (!filter.isNullOrBlank()) q.name.ilike("%$filter%") else q }
      .findCount()

    // Build features-per-filter map via one query per filter using the inverse association
    val filterIds = filters.keys
    val applicationsByFilter = mutableMapOf<UUID, MutableList<SearchFeatureFilterApplication>>()
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

    // we need to find the application features
    QDbApplicationFeature()
      .select(QDbApplicationFeature.Alias.id, QDbApplicationFeature.Alias.key, QDbApplicationFeature.Alias.parentApplication.id, QDbApplicationFeature.Alias.filters.id)
      .filters.id.`in`(filterIds)
      .parentApplication.id.`in`(allowedApps.keys)
      .findList().forEach { af ->
        af.filters.forEach { filter ->
          // find the applications associated with this filter
          val apps = applicationsByFilter.getOrPut(filter.id) { mutableListOf() }

          // add this new app (if its new) to the list
          var app = apps.firstOrNull( { it.id == af.parentApplication.id })
          if (app == null) {
            app = SearchFeatureFilterApplication()
              .id(af.parentApplication.id)
              .name(af.parentApplication.name)
            apps.add(app)
          }

          // add the feature to the app
          app.addFeaturesItem(SearchFeatureFilterFeature().id(af.id).key(af.key))
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
            .type(PersonType.PERSON)
        })
        .applications(applicationsByFilter[db.id])
        .serviceAccounts(saByFilter[db.id])
    }

    return SearchFeatureFilterResult().pagination(PaginationResult().page(pageNum).pageSize(pageSize).total(simpleQuery.findCount())).filters(items)
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
          .type(PersonType.PERSON)
      })
  }

  override fun findApplicationsWithFeatureWithFilters(
    id: UUID,
    filters: List<UUID>
  ): MatchingFilterResults {
    return MatchingFilterResults().matchingResults(
      QDbApplication()
      .select(QDbApplication.Alias.id, QDbApplication.Alias.name)
      .features.filters.id.`in`(filters)
    .findList().map { app ->
          MatchingFilterResult().id(app.id).name(app.name)
        })
  }

  override fun findServiceAccountsWithFilters(
    id: UUID,
    filters: List<UUID>
  ): MatchingFilterResults {
    return MatchingFilterResults().matchingResults(
      QDbServiceAccount()
        .select(QDbServiceAccount.Alias.id, QDbServiceAccount.Alias.name)
        .featureFilters.id.`in`(filters)
        .findList().map { sa ->
          MatchingFilterResult().id(sa.id).name(sa.name)
        }
    )
  }

  override fun getFeatureFilter(portfolioId: UUID, filterId: UUID): FeatureFilter? {
    val dbFilter = QDbFeatureFilter()
      .id.eq(filterId)
      .portfolio.id.eq(portfolioId)
      .findOne()
      ?: return null

    return toFeatureFilter(dbFilter)
  }
}
