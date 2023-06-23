package io.featurehub.db.services

import io.ebean.Database
import io.ebean.DuplicateKeyException
import io.ebean.annotation.Transactional
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PortfolioApi
import io.featurehub.db.model.DbGroup
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.query.QDbGroup
import io.featurehub.db.model.query.QDbPortfolio
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.model.SortOrder
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors

@Singleton
class PortfolioSqlApi @Inject constructor(
  private val database: Database, private val convertUtils: Conversions, private val archiveStrategy: ArchiveStrategy
) : PortfolioApi {
  fun getPortfolio(name: String): Portfolio? {
    return QDbPortfolio().name.ieq(name).findOne()?.let { p -> convertUtils.toPortfolio(p, Opts.empty()) }
  }

  @Transactional(readOnly = true)
  override fun findPortfolios(
    filter: String?, ordering: SortOrder?, opts: Opts, currentPerson: Person
  ): List<Portfolio> {
    val personDoingFind = convertUtils.byPerson(currentPerson) ?: return ArrayList()

    var pFinder = QDbPortfolio().organization.eq(
      convertUtils.dbOrganization()
    )

    if (filter != null && filter.trim { it <= ' ' }.isNotEmpty()) {
      pFinder = pFinder.name.ilike('%'.toString() + filter.trim { it <= ' ' } + '%')
    }

    if (ordering != null) {
      if (ordering == SortOrder.ASC) {
        pFinder = pFinder.order().name.asc()
      } else if (ordering == SortOrder.DESC) {
        pFinder = pFinder.order().name.desc()
      }
    }
    val personIsNotSuperAdmin = convertUtils.personIsNotSuperAdmin(personDoingFind)
    if (personIsNotSuperAdmin) {
      pFinder = pFinder.groups.groupMembers.person.id.eq(personDoingFind.id)
    }
    pFinder = finder(pFinder, opts)

    return pFinder.findList()
      .map { p -> convertUtils.toPortfolio(p, opts, currentPerson, personIsNotSuperAdmin)!! }
  }

  private fun finder(pFinder: QDbPortfolio, opts: Opts): QDbPortfolio {
    var pFinder = pFinder
    if (opts.contains(FillOpts.Groups)) {
      pFinder = pFinder.groups.fetch()
    }
    if (opts.contains(FillOpts.Portfolios)) {
      pFinder = pFinder.whoCreated.fetch()
    }
    if (opts.contains(FillOpts.Applications)) {
      pFinder = pFinder.applications.fetch()
    }
    if (!opts.contains(FillOpts.Archived)) {
      pFinder = pFinder.whenArchived.isNull
    }
    return pFinder
  }

  @Throws(PortfolioApi.DuplicatePortfolioException::class)
  override fun createPortfolio(portfolio: Portfolio, opts: Opts, createdBy: Person): Portfolio {
    val org = convertUtils.dbOrganization()
    val person = convertUtils.byPerson(createdBy)
    require(person != null) { "Created by person is an invalid argument (does not exist)" }
    duplicateCheck(portfolio, null, org)
    val dbPortfolio = DbPortfolio.Builder()
      .name(convertUtils.limitLength(portfolio.name, 200))
      .description(convertUtils.limitLength(portfolio.description, 400))
      .organization(org)
      .whoCreated(person)
      .build()
    updatePortfolio(dbPortfolio)
    return convertUtils.toPortfolio(dbPortfolio, opts)!!
  }

  @Transactional
  @Throws(PortfolioApi.DuplicatePortfolioException::class)
  private fun updatePortfolio(portfolio: DbPortfolio) {
    try {
      database.save(portfolio)
    } catch (dke: DuplicateKeyException) {
      throw PortfolioApi.DuplicatePortfolioException()
    }
  }

  @Transactional(readOnly = true)
  override fun getPortfolio(id: UUID, opts: Opts, currentPerson: Person): Portfolio? {
    val personDoingFind = convertUtils.byPerson(currentPerson) ?: return null
    var finder = finder(QDbPortfolio().id.eq(id), opts)
    val personIsNotSuperAdmin = convertUtils.personIsNotSuperAdmin(personDoingFind)
    if (personIsNotSuperAdmin) {
      finder = finder.groups.groupMembers.person.id.eq(personDoingFind.id)
    }

    return finder.findOne()?.let { portf ->
      convertUtils.toPortfolio(portf, opts, currentPerson, personIsNotSuperAdmin)
    }
  }

  @Throws(PortfolioApi.DuplicatePortfolioException::class, OptimisticLockingException::class)
  override fun updatePortfolio(portfolio: Portfolio, opts: Opts): Portfolio? {
    val portf = convertUtils.byPortfolio(portfolio.id)

    if (portf != null) {
      if (portfolio.version == null || portfolio.version != portf.version) {
        throw OptimisticLockingException()
      }
      duplicateCheck(portfolio, portf, portf.organization)
      portf.name = portfolio.name
      portf.description = portfolio.description

      // this is actually a possible leak for duplicate portfolio names, we will get an exception
      // here.
      updatePortfolio(portf)

      // rename the group
      QDbGroup().adminGroup
        .eq(true)
        .and().owningPortfolio
        .eq(portf)
        .endAnd()
        .findOne()?.let { group ->
          group.name = portfolio.name
          updateGroup(group)
        }

      return convertUtils.toPortfolio(portf, opts)!!
    }

    return null
  }

  @Throws(PortfolioApi.DuplicatePortfolioException::class)
  private fun duplicateCheck(portfolio: Portfolio, portf: DbPortfolio?, organization: DbOrganization) {
    // if you are changing your name to its existing name, thats fine.  optimisation step
    if (portf != null && portf.name.equals(portfolio.name, ignoreCase = true)) {
      return
    }
    // check for name duplicates
    val nameCheck = QDbPortfolio().whenArchived
      .isNull.organization
      .eq(organization).name
      .ieq(portfolio.name)
      .findOne()
    if (nameCheck != null && (portf == null || nameCheck.id != portf.id)) {
      throw PortfolioApi.DuplicatePortfolioException()
    }
  }

  @Transactional
  private fun updateGroup(group: DbGroup) {
    database.save(group)
  }

  @Transactional
  override fun deletePortfolio(id: UUID) {
    QDbPortfolio().id.eq(id).findOne()?.let {
      archiveStrategy.archivePortfolio(it)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(PortfolioSqlApi::class.java)
  }
}
