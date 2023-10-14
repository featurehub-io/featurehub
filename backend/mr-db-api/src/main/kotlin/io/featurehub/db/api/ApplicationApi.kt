package io.featurehub.db.api

import io.featurehub.mr.model.*
import java.util.*

interface ApplicationApi {
  fun getApplicationSummary(appId: UUID): ApplicationSummary?
  class DuplicateApplicationException : Exception()
  class DuplicateFeatureException : Exception()

  @Throws(DuplicateApplicationException::class)
  fun createApplication(portfolioId: UUID, application: CreateApplication, current: Person): Application?
  fun findApplications(
    portfolioId: UUID, filter: String?, order: SortOrder?,
    opts: Opts, current: Person, loadAll: Boolean
  ): List<Application>

  fun deleteApplication(portfolioId: UUID, applicationId: UUID): Boolean
  fun getApplication(appId: UUID, opts: Opts): Application?

  @Throws(DuplicateApplicationException::class, OptimisticLockingException::class)
  fun updateApplication(appId: UUID, application: Application, opts: Opts): Application?
  fun updateApplicationOnPortfolio(portfolioId: UUID, application: Application, opts: Opts): Application?

  @Throws(DuplicateFeatureException::class)
  fun createApplicationFeature(appId: UUID, createFeature: CreateFeature, person: Person, opts: Opts): List<Feature>

  @Throws(DuplicateFeatureException::class, OptimisticLockingException::class)
  fun updateApplicationFeature(appId: UUID, key: String, feature: Feature, opts: Opts): List<Feature>?

  @Throws(DuplicateFeatureException::class, OptimisticLockingException::class)
  fun updateApplicationFeature(appId: UUID, feature: Feature, opts: Opts): List<Feature>?

    fun getApplicationFeatures(appId: UUID, opts: Opts): List<Feature>
  fun deleteApplicationFeature(appId: UUID, key: String): List<Feature>?
  fun getApplicationFeatureByKey(appId: UUID, key: String, opts: Opts): Feature?
  fun findFeatureEditors(appId: UUID): Set<UUID>
  fun personIsFeatureEditor(appId: UUID, personId: UUID): Boolean
  fun findApplicationPermissions(appId: UUID, personId: UUID): ApplicationPermissions

  /**
   * Those who can create features
   * @param appId
   * @return
   */
  fun findFeatureCreators(appId: UUID): Set<UUID>
  fun personIsFeatureCreator(appId: UUID, personId: UUID): Boolean
  fun findFeatureReaders(appId: UUID): Set<UUID>
  fun personIsFeatureReader(appId: UUID, personId: UUID): Boolean
}
