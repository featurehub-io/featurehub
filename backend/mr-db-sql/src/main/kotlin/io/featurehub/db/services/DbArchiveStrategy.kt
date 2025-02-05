package io.featurehub.db.services

import io.ebean.annotation.Transactional
import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.model.DbApplication
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbApplicationRolloutStrategy
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbGroup
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbPortfolio
import io.featurehub.db.model.DbServiceAccount
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.services.ArchiveStrategy.Companion.isoDate
import io.featurehub.mr.events.common.CacheSource
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class DbArchiveStrategy @Inject constructor(private val cacheSource: CacheSource) :
  ArchiveStrategy {

  @Transactional
  override fun archivePortfolio(portfolio: DbPortfolio) {
    portfolio.whenArchived = LocalDateTime.now()
    portfolio.name = (portfolio.name + Conversions.archivePrefix + isoDate.format(portfolio.whenArchived)).take(250)
    portfolio.save()
    portfolio.applications.forEach { application: DbApplication -> archiveApplication(application) }
    portfolio.groups.forEach { group: DbGroup -> archiveGroup(group) }
    portfolio.serviceAccounts.forEach { serviceAccount: DbServiceAccount ->
      archiveServiceAccount(
        serviceAccount
      )
    }
  }

  @Transactional
  override fun archiveApplication(application: DbApplication) {
    application.whenArchived = LocalDateTime.now()
    application.save()
    application.environments.forEach { environment: DbEnvironment -> archiveEnvironment(environment) }
    application.features.forEach { feature: DbApplicationFeature -> archiveApplicationFeature(feature) }
  }

  @Transactional
  override fun archiveEnvironment(environment: DbEnvironment) {
    environment.whenArchived = LocalDateTime.now()
    environment.name = (environment.name + Conversions.archivePrefix + isoDate.format(environment.whenArchived)).take(250)
    environment.save()
    cacheSource.deleteEnvironment(environment.id)
    QDbEnvironment().priorEnvironment.eq(environment).findList().forEach { e: DbEnvironment ->
      if (environment.priorEnvironment != null) {
        if (e.id == environment.priorEnvironment.id) {
          e.priorEnvironment = null
        } else {
          e.priorEnvironment = environment.priorEnvironment
        }
        e.save()
      }
    }

    environmentListeners.forEach {
      it.invoke(environment)
    }
  }

  private val environmentListeners = mutableListOf<(DbEnvironment) -> Unit>()

  override fun environmentArchiveListener(listener: (DbEnvironment) -> Unit) {
    environmentListeners.add(listener)
  }

  @Transactional
  override fun archiveOrganization(organization: DbOrganization) {
    organization.whenArchived = LocalDateTime.now()
    organization.save()
    organization.portfolios.forEach { portfolio: DbPortfolio -> archivePortfolio(portfolio) }
  }

  @Transactional
  override fun archiveServiceAccount(serviceAccount: DbServiceAccount) {
    serviceAccount.whenArchived = LocalDateTime.now()
    serviceAccount.name =
      (serviceAccount.name + Conversions.archivePrefix + isoDate.format(serviceAccount.whenArchived)).take(250)
    serviceAccount.save()
    cacheSource.deleteServiceAccount(serviceAccount.id)
  }

  @Transactional
  override fun archiveGroup(group: DbGroup) {
    group.whenArchived = LocalDateTime.now()
    group.name =
      (group.name + Conversions.archivePrefix + isoDate.format(group.whenArchived)).take(250)
    group.save()
  }

  @Transactional
  override fun archiveApplicationFeature(feature: DbApplicationFeature) {
    feature.whenArchived = LocalDateTime.now()
    // key is unique
    val originalKey = feature.key
    feature.key =
      (feature.key + Conversions.archivePrefix + isoDate.format(feature.whenArchived)).take(250)
    feature.save()

    featureListeners.forEach {
      try {
        it(feature)
      } catch (e: Exception) {
        log.error("unable to update feature listener", e)
      }
    }

    cacheSource.publishFeatureChange(feature, PublishAction.DELETE, originalKey)
  }

  private val featureListeners = mutableListOf<(DbApplicationFeature) -> Unit>()

  override fun featureListener(listener: (DbApplicationFeature) -> Unit) {
    featureListeners.add(listener)
  }

  @Transactional
  override fun archivePerson(person: DbPerson) {
    person.whenArchived = LocalDateTime.now()
    person.save()
  }

  @Transactional
  override fun archiveApplicationStrategy(strategy: DbApplicationRolloutStrategy, personWhoArchived: DbPerson) {
  }

  companion object {
    private val log = LoggerFactory.getLogger(DbArchiveStrategy::class.java)
  }
}
