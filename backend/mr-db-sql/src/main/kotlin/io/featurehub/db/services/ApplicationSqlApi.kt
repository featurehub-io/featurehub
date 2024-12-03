package io.featurehub.db.services

import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.model.*
//import io.featurehub.db.model.RoleType
import io.featurehub.db.model.query.*
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.*
import io.featurehub.mr.model.RoleType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*

@Singleton
class ApplicationSqlApi @Inject constructor(
  private val convertUtils: Conversions,
  private val cacheSource: CacheSource,
  private val archiveStrategy: ArchiveStrategy,
  private val internalFeatureSqlApi: InternalFeatureApi
) : ApplicationApi {
  override fun getApplicationSummary(appId: UUID): ApplicationSummary? {
    return if (!QDbApplication().id.eq(appId).exists()) {
      null
    } else ApplicationSummary()
      .groupsHavePermission(
        QDbAcl().environment.parentApplication.id.eq(appId).roles.isNotNull().roles.notEqualTo(
          ""
        ).exists()
      )
      .environmentCount(
        QDbEnvironment().parentApplication.id.eq(appId).whenArchived.isNull().findCount()
      )
      .featureCount(
        QDbApplicationFeature().parentApplication.id.eq(appId).whenArchived.isNull()
          .findCount()
      )
      .serviceAccountsHavePermission(
        QDbServiceAccountEnvironment().environment.parentApplication.id.eq(
          appId
        ).permissions.isNotNull().permissions.notEqualTo("").exists()
      )
  }

  @Throws(ApplicationApi.DuplicateApplicationException::class)
  override fun createApplication(portfolioId: UUID, application: CreateApplication, current: Person): Application? {
    val portfolio = convertUtils.byPortfolio(portfolioId) ?: return null
    val updater = convertUtils.byPerson(current) ?: return null

    return if (!QDbApplication()
        .and().name
        .iequalTo(application.name).whenArchived
        .isNull().portfolio
        .eq(portfolio)
        .endAnd()
        .exists()
    ) {
      val aApp = DbApplication.Builder()
        .name(application.name)
        .description(application.description)
        .portfolio(portfolio)
        .whoCreated(updater)
        .build()

      // update the portfolio group to ensure it has permissions to add features to this new
      // application
      val adminGroup = QDbGroup().owningPortfolio
        .eq(aApp.portfolio).whenArchived
        .isNull().adminGroup
        .isTrue()
        .findOne()
      adminGroup?.groupRolesAcl?.add(
        DbAcl.Builder()
          .application(aApp)
          .roles(
            GroupSqlApi.appRolesToString(
              listOf(
                ApplicationRoleType.FEATURE_CREATE,
                ApplicationRoleType.FEATURE_EDIT_AND_DELETE
              )
            )
          )
          .build()
      )
      addApplicationFeatureCreationRoleToPortfolioAdminGroup(aApp, adminGroup)
      convertUtils.toApplication(aApp, Opts.empty())
    } else {
      throw ApplicationApi.DuplicateApplicationException()
    }
  }

  @Transactional
  private fun addApplicationFeatureCreationRoleToPortfolioAdminGroup(
    aApp: DbApplication, adminGroup: DbGroup?
  ) {
    aApp.save()
    adminGroup?.save()
  }

  @Transactional
  private fun saveApp(app: DbApplication) {
    app.save()
  }

  override fun findApplications(
    portfolioId: UUID,
    filter: String?,
    order: SortOrder?,
    opts: Opts,
    current: Person,
    loadAll: Boolean
  ): List<Application> {
    Conversions.nonNullPortfolioId(portfolioId)
    var queryApplicationList = QDbApplication().portfolio.id.eq(portfolioId)
    if (filter != null) {
      queryApplicationList = queryApplicationList.name.ilike("%$filter%")
    }
    if (!opts.contains(FillOpts.Archived)) {
      queryApplicationList = queryApplicationList.whenArchived.isNull()
    }
    queryApplicationList = fetchApplicationOpts(opts, queryApplicationList)
    if (SortOrder.ASC == order) {
      queryApplicationList = queryApplicationList.order().name.asc()
    } else if (SortOrder.DESC == order) {
      queryApplicationList = queryApplicationList.order().name.desc()
    }
    if (!loadAll) {
      Conversions.nonNullPerson(current)
      // we need to ascertain which apps they can actually see based on environments
      queryApplicationList = queryApplicationList.environments.groupRolesAcl.group.groupMembers.person.id.eq(
        current.id!!.id
      )
    }
    return queryApplicationList.findList()
      .map { app -> convertUtils.toApplication(app, opts)!! }
  }

  private fun fetchApplicationOpts(opts: Opts, finder: QDbApplication): QDbApplication {
    var eq = finder
    if (opts.contains(FillOpts.Environments)) {
      eq = eq.environments.fetch()
    }
    if (opts.contains(FillOpts.Features)) {
      eq = eq.features.fetch()
    }
    if (!opts.contains(FillOpts.Archived)) {
      eq = eq.whenArchived.isNull()
    }
    return eq
  }

  override fun deleteApplication(portfolioId: UUID, applicationId: UUID): Boolean {
    val portfolio = convertUtils.byPortfolio(portfolioId) ?: return false
    val app = convertUtils.byApplication(applicationId) ?: return false
    if (app.portfolio.id == portfolio.id) {
      archiveStrategy.archiveApplication(app)
      return true
    }
    return false
  }

  fun getApplication(portfolioId: UUID, name: String): Application? {
    val app = QDbApplication().name.ieq(name).portfolio.id.eq(portfolioId).findOne()
    return if (app != null) {
      convertUtils.toApplication(app, Opts.empty())
    } else null
  }

  override fun getApplication(appId: UUID, opts: Opts): Application {
    return convertUtils.toApplication(
      fetchApplicationOpts(opts, QDbApplication().id.eq(appId)).findOne(), opts
    )!!
  }


  override fun updateApplicationOnPortfolio(portfolioId: UUID, application: Application, opts: Opts): Application? {
    val app = fetchApplicationOpts(opts, QDbApplication().id.eq(application.id).portfolio.id.eq(portfolioId)).findOne() ?: return null
    return updateApplication(application, app, opts)
  }

  @Throws(ApplicationApi.DuplicateApplicationException::class, OptimisticLockingException::class)
  override fun updateApplication(appId: UUID, application: Application, opts: Opts): Application? {
    val app = fetchApplicationOpts(opts, QDbApplication().id.eq(appId)).findOne() ?: return null

    return updateApplication(application, app, opts)
  }

  private fun updateApplication(application: Application, app: DbApplication, opts: Opts): Application? {
    if (application.version != app.version) {
      throw OptimisticLockingException()
    }

    if (app.name != application.name) {
      if (QDbApplication().portfolio
          .eq(app.portfolio).name
          .eq(application.name).whenArchived
          .isNull()
          .exists()
      ) {
        throw ApplicationApi.DuplicateApplicationException()
      }
    }

    app.name = application.name
    app.description = application.description

    saveApp(app)

    return convertUtils.toApplication(app, opts)
  }

  @Throws(ApplicationApi.DuplicateFeatureException::class)
  fun createApplicationLevelFeature(
    applicationId: UUID, feature: CreateFeature, person: Person,
    opts: Opts
  ): DbApplicationFeature? {
    val app = convertUtils.byApplication(applicationId)
    if (app != null) {
      if (QDbApplicationFeature().key.eq(feature.key).parentApplication.eq(app).exists()) {
        throw ApplicationApi.DuplicateFeatureException()
      }
      val appFeature = DbApplicationFeature.Builder()
        .name(feature.name)
        .key(feature.key)
        .parentApplication(app)
        .alias(feature.alias)
        .link(feature.link)
        .secret(feature.secret == true)
        .valueType(feature.valueType)
        .metaData(feature.metaData)
        .description(feature.description)
        .build()

      saveApplicationFeature(appFeature)

      bumpVersionOfAllEnvironmentsWithFeatureChanged(applicationId)

      if (appFeature.valueType != FeatureValueType.BOOLEAN) {
        cacheSource.publishFeatureChange(appFeature, PublishAction.CREATE)
      } else {
        // if this is a boolean feature, create this feature with a default value of false in all
        // environments we currently
        // have
        createDefaultBooleanFeatureValuesForAllEnvironments(appFeature, app, person)
      }
      return appFeature
    }
    return null
  }

  private fun bumpVersionOfAllEnvironmentsWithFeatureChanged(applicationId: UUID) {
    QDbEnvironment().parentApplication.id.eq(applicationId).asUpdate().setRaw("version = version + 1").update()
  }

  @Throws(ApplicationApi.DuplicateFeatureException::class)
  override fun createApplicationFeature(
    appId: UUID, createFeature: CreateFeature, person: Person,
    opts: Opts
  ): List<Feature> {
    val feat = createApplicationLevelFeature(appId, createFeature, person, opts)
    return if (feat != null) {
      getAppFeatures(feat.parentApplication, opts)
    } else ArrayList()
  }

  private fun createDefaultBooleanFeatureValuesForAllEnvironments(
    appFeature: DbApplicationFeature, app: DbApplication, person: Person
  ) {
    val appEnvironments = QDbEnvironment().whenArchived.isNull().parentApplication.eq(app).findList()
    val dbPerson = convertUtils.byPerson(person)
    val defaultValue = java.lang.Boolean.FALSE.toString()
    val newFeatures = appEnvironments
      .map { env: DbEnvironment? ->
        DbFeatureValue(
          dbPerson!!, true, appFeature, env!!, defaultValue
        )
      }
    saveAllFeatures(newFeatures)
    cacheSource.publishFeatureChange(appFeature, PublishAction.CREATE)
  }

  // this ensures we create the features and create initial historical records for them as well
  @Transactional(type = TxType.REQUIRES_NEW)
  private fun saveAllFeatures(newFeatures: List<DbFeatureValue>) {
    for (newFeature in newFeatures) {
      internalFeatureSqlApi.saveFeatureValue(newFeature)
    }
  }

  private fun getAppFeatures(app: DbApplication, opts: Opts): List<Feature> {
    return app.features
      .filter { af: DbApplicationFeature -> af.whenArchived == null }
      .map { af: DbApplicationFeature? -> convertUtils.toApplicationFeature(af, opts)!! }
  }

  override fun updateApplicationFeature(appId: UUID, feature: Feature, opts: Opts): List<Feature>? {
    return updateApplicationFeature(appId, feature.key, feature, opts)
  }

  @Throws(ApplicationApi.DuplicateFeatureException::class, OptimisticLockingException::class)
  override fun updateApplicationFeature(appId: UUID, key: String, feature: Feature, opts: Opts): List<Feature>? {
    Conversions.nonNullApplicationId(appId)
    val app = convertUtils.byApplication(appId)
    if (app != null) {
      val appFeature = QDbApplicationFeature()
        .and().key
        .eq(key).parentApplication
        .eq(app)
        .endAnd()
        .findOne() ?: return null
      if (feature.version == null || appFeature.version != feature.version) {
        throw OptimisticLockingException()
      }
      if (key != feature.key) { // we are changing the key?
        if (QDbApplicationFeature().key
            .eq(feature.key).parentApplication
            .eq(app)
            .endAnd()
            .exists()
        ) {
          throw ApplicationApi.DuplicateFeatureException()
        }
        bumpVersionOfAllEnvironmentsWithFeatureChanged(appId)
      }
      val changed = feature.key != appFeature.key || feature.valueType != appFeature.valueType
      appFeature.name = feature.name
      appFeature.alias = feature.alias
      appFeature.key = feature.key
      if (feature.link != null) {
        appFeature.link = feature.link
      }
      appFeature.valueType = feature.valueType
      appFeature.isSecret = feature.secret != null && feature.secret!!
      if (feature.metaData != null) {
        appFeature.metaData = feature.metaData
      }
      if (feature.description != null) {
        appFeature.description = feature.description
      }
      updateApplicationFeature(appFeature)
      if (appFeature.whenArchived == null && changed) {
        cacheSource.publishFeatureChange(appFeature, PublishAction.UPDATE)
      }
      return getAppFeatures(app, opts)
    }
    return ArrayList()
  }

  @Transactional
  private fun updateApplicationFeature(appFeature: DbApplicationFeature) {
    val changedVersion = appFeature.version
    appFeature.update()
    if (appFeature.version != changedVersion) {
      cacheSource.publishFeatureChange(appFeature, PublishAction.UPDATE)
    }
  }

  @Transactional
  private fun saveApplicationFeature(f: DbApplicationFeature) {
    f.save()
  }

  override fun getApplicationFeatures(appId: UUID, opts: Opts): List<Feature> {
    val app = convertUtils.byApplication(appId)
    return app?.let { getAppFeatures(it, opts) } ?: ArrayList()
  }

  internal class AppFeature(val app: DbApplication?, val appFeature: DbApplicationFeature?) {
    val isValid: Boolean
      get() = app != null && appFeature != null
  }

  private fun findAppFeature(appId: UUID, applicationFeatureKeyName: String): AppFeature? {
    val app = convertUtils.byApplication(appId)
    if (app != null) {
      val appFeature = QDbApplicationFeature()
        .and().key
        .eq(applicationFeatureKeyName).parentApplication
        .eq(app)
        .endAnd()
        .findOne()
      if (appFeature == null) {
        val id = Conversions.checkUuid(applicationFeatureKeyName)
        if (id != null) {
          return AppFeature(
            app, QDbApplicationFeature().id.eq(id).parentApplication.eq(app).findOne()
          )
        }
      } else {
        return AppFeature(app, appFeature)
      }
    }
    return null
  }

  override fun deleteApplicationFeature(appId: UUID, key: String): List<Feature>? {
    val appFeature = findAppFeature(appId, key) ?: return null
    if (!appFeature.isValid) {
      return null
    }

    bumpVersionOfAllEnvironmentsWithFeatureChanged(appId)

    // make sure it isn't already deleted
    if (appFeature.appFeature!!.whenArchived == null) {
      archiveStrategy.archiveApplicationFeature(appFeature.appFeature)
    }

    return getAppFeatures(appFeature.app!!, Opts.empty())
  }

  override fun getApplicationFeatureByKey(appId: UUID, key: String, opts: Opts): Feature? {
    val af = findAppFeature(appId, key) ?: return null
    return if (af.isValid) convertUtils.toApplicationFeature(af.appFeature, opts)!! else null
  }

  private val editorRoles = setOf(ApplicationRoleType.FEATURE_EDIT, ApplicationRoleType.FEATURE_EDIT_AND_DELETE)
  private val creatorRoles = setOf(
    ApplicationRoleType.FEATURE_EDIT, ApplicationRoleType.FEATURE_CREATE,
    ApplicationRoleType.FEATURE_EDIT_AND_DELETE
  )

  // finds all the groups attached to this application  that have application roles
  // and filters them by the feature edit role, and adds them to the outgoing set.
  override fun findFeatureEditors(appId: UUID): Set<UUID> {
    Conversions.nonNullApplicationId(appId)
    return findFeaturePermissionsByType(appId, editorRoles)
  }

  override fun findFeatureCreators(appId: UUID): Set<UUID> {
    Conversions.nonNullApplicationId(appId)
    return findFeaturePermissionsByType(appId, creatorRoles)
  }

  override fun personIsFeatureEditor(appId: UUID, personId: UUID): Boolean {
    return personHoldsOneOfApplicationRoles(appId, personId, editorRoles)
  }

  override fun findApplicationPermissions(appId: UUID, personId: UUID): ApplicationPermissions {
    // superusers get everything
    if (convertUtils.personIsSuperAdmin(personId) || convertUtils.isPersonApplicationAdmin(personId, appId) ) {
      val allRoles = RoleType.values().toList()

      return ApplicationPermissions()
        .applicationRoles(ApplicationRoleType.values().toList())
        .environments(QDbEnvironment()
          .select(QDbEnvironment.Alias.name, QDbEnvironment.Alias.id)
          .whenArchived.isNull
          .parentApplication.id.eq(appId).findList().map { env -> EnvironmentPermission().id(env.id).name(env.name).roles(
            allRoles) })
    }

    val appPerms = mutableSetOf<ApplicationRoleType>()
      QDbAcl()
      .select(QDbAcl.Alias.roles)
      .application.id.eq(appId)
      .group.groupMembers.person.id.eq(personId)
      .findList().map { acl -> convertUtils.splitApplicationRoles(acl.roles) }.forEach { appPerms.addAll(it) }

    val environments = mutableListOf<EnvironmentPermission>()

    environmentPermissions(appId, personId)
      .select(QDbAcl.Alias.environment.id, QDbAcl.Alias.environment.name, QDbAcl.Alias.roles)
      .findList().forEach { acl ->
      val roles = convertUtils.splitEnvironmentRoles(acl.roles.trim())
      if (roles.isNotEmpty()) {
        if (!roles.contains(RoleType.READ)) {
          roles.add(RoleType.READ)
        }
        environments.add(EnvironmentPermission().id(acl.environment.id).name(acl.environment.name).roles(roles))
      }
    }

    return ApplicationPermissions().applicationRoles(appPerms.toList()).environments(environments)
  }

  override fun personIsFeatureCreator(appId: UUID, personId: UUID): Boolean {
    return personHoldsOneOfApplicationRoles(appId, personId, creatorRoles)
  }

  private fun personHoldsOneOfApplicationRoles(
    appId: UUID,
    personId: UUID, roles: Set<ApplicationRoleType>
  ): Boolean {
    return QDbAcl()
      .select(QDbAcl.Alias.roles).application.id.eq(appId).group.groupMembers.person.id.eq(personId)
      .findList().any { acl ->
        convertUtils.splitApplicationRoles(acl.roles).any { o -> roles.contains(o) }
      }
  }

  private fun findFeaturePermissionsByType(appId: UUID, roles: Set<ApplicationRoleType>): Set<UUID> {
    log.trace("searching for permissions for app id {}, roles {}", appId, roles)
    // find which groups have those roles
    val groups = QDbAcl()
      .select(QDbAcl.Alias.group.id, QDbAcl.Alias.roles).application.id.eq(appId).group.whenArchived.isNull()
      .findList().filter { acl ->
        convertUtils.splitApplicationRoles(acl.roles).any { o -> roles.contains(o) }
      }
      .map { acl: DbAcl -> acl.group.id }

    // find which people are in those groups
    if (groups.isNotEmpty()) {
      val collect = QDbPerson()
        .select(QDbPerson.Alias.id).groupMembers.group.id.`in`(groups).findList()
        .map { obj: DbPerson -> obj.id }
        .toSet()
      log.trace("and out with {}", collect)
      return collect
    }
    return HashSet()
  }

  override fun findFeatureReaders(appId: UUID): Set<UUID> {
    Conversions.nonNullApplicationId(appId)
    val featureReaders: MutableSet<UUID> = HashSet()
    QDbAcl()
      .or().environment.parentApplication.id.eq(appId).application.id.eq(appId)
      .endOr().group.whenArchived.isNull().group.groupMembers.person.fetch()
      .findList()
      .forEach { acl ->
        if (acl.application != null || acl.roles.trim { it <= ' ' }.isNotEmpty()) {
          acl.group
            .groupMembers
            .forEach { p: DbGroupMember -> featureReaders.add(p.person.id) }
        }
      }

    // we don't need to add superusers because they are automatically added to each portfolio group
    return featureReaders
  }

  fun environmentPermissions(applicationId: UUID, personId: UUID): QDbAcl {
    return QDbAcl()
      .or()
        .environment.parentApplication.id.eq(applicationId)
        .application.id.eq(applicationId)
      .endOr()
      .group.whenArchived.isNull()
      .group.groupMembers.person.id.eq(personId)
  }


  override fun personIsFeatureReader(appId: UUID, personId: UUID): Boolean {
    val person = convertUtils.byPerson(personId)
    if (convertUtils.personIsSuperAdmin(person)) {
      return true
    }
    if (person != null) {
      for (acl in environmentPermissions(appId, personId).findList()) {
        if (acl.application != null) {
          return true
        }
        if (acl.roles.trim { it <= ' ' }.isNotEmpty()) {
          return true
        }
      }
    }
    return false
  }

  companion object {
    private val log = LoggerFactory.getLogger(ApplicationSqlApi::class.java)
  }
}
