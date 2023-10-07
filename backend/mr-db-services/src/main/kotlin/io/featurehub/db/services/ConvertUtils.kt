package io.featurehub.db.services

import io.featurehub.db.FilterOptType
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.model.*
import io.featurehub.db.model.query.*
import io.featurehub.mr.model.*
import io.featurehub.mr.model.RoleType
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.stream.Collectors

@Singleton
open class ConvertUtils : Conversions {
  override fun byPerson(id: UUID?): DbPerson? {
    return if (id == null) null else QDbPerson().id.eq(id).findOne()
  }

  override fun byPerson(id: UUID?, opts: Opts?): DbPerson? {
    if (id == null) {
      return null
    }
    var finder = QDbPerson().id.eq(id)
    if (opts!!.contains(FillOpts.Groups)) {
      finder = finder.groupMembers.fetch()
    }
    return finder.findOne()
  }

  override fun byPortfolio(portfolioId: UUID?): DbPortfolio? {
    return if (portfolioId == null) null else QDbPortfolio().id.eq(portfolioId).findOne()
  }

  override fun byEnvironment(id: UUID?): DbEnvironment? {
    return if (id == null) null else QDbEnvironment().id.eq(id).findOne()
  }

  override fun byEnvironment(id: UUID?, opts: Opts?): DbEnvironment? {
    if (id == null) {
      return null
    }
    val eq = QDbEnvironment().id.eq(id)
    if (opts!!.contains(FillOpts.Applications)) {
      eq.parentApplication.fetch()
    }
    if (opts.contains(FillOpts.Portfolios)) {
      eq.parentApplication.portfolio.fetch()
    }
    if (opts.contains(FillOpts.ApplicationIds)) {
      eq.parentApplication.fetch(QDbApplication.Alias.id)
    }
    if (opts.contains(FillOpts.PortfolioIds)) {
      eq.parentApplication.portfolio.fetch(QDbPortfolio.Alias.id)
    }
    return eq.findOne()
  }

  override fun byApplication(id: UUID?): DbApplication? {
    return if (id == null) null else QDbApplication().id.eq(id).findOne()
  }

  override fun personIsNotSuperAdmin(person: DbPerson?): Boolean {
    return !QDbGroup().owningPortfolio
      .isNull.adminGroup
      .isTrue.groupMembers.person.id
      .eq(person!!.id)
      .exists()
  }

  override fun personIsSuperAdmin(person: DbPerson?): Boolean {
    val p = person ?: return false

    return personIsSuperAdmin(p.id)
  }

  override fun personIsSuperAdmin(person: UUID): Boolean {
    return QDbGroup().whenArchived
      .isNull.owningPortfolio
      .isNull.groupMembers.person
      .id.eq(person).adminGroup
      .isTrue
      .exists()
  }

  override fun safeConvert(bool: Boolean?): Boolean {
    return bool?.let { bool } ?: false
  }

  override fun isPersonMemberOfPortfolioGroup(portfolioId: UUID, personId: UUID): Boolean {
    return QDbGroup().owningPortfolio.id.eq(portfolioId).groupMembers.person.id.eq(personId).exists()
  }

  override fun isPersonMemberOfPortfolioAdminGroup(portfolioId: UUID, personId: UUID): Boolean {
    return QDbGroup()
      .owningPortfolio.id.eq(portfolioId)
      .adminGroup.isTrue
      .groupMembers.person.id.eq(personId)
      .exists()
  }

  override fun limitLength(s: String?, len: Int): String? {
    return if (s == null) null else if (s.length > len) s.substring(0, len) else s
  }

  override fun toEnvironment(
    env: DbEnvironment?, opts: Opts?, features: Set<DbApplicationFeature?>?
  ): Environment? {
    if (env == null) {
      return null
    }
    val environment = Environment()
      .id(env.id)
      .name(stripArchived(env.name, env.whenArchived))
      .version(env.version)
      .production(env.isProductionEnvironment)
      .priorEnvironmentId(
        if (env.priorEnvironment != null) env.priorEnvironment.id else null
      )
      .applicationId(env.parentApplication.id)
    if (opts!!.contains(FillOpts.Details)) {
      environment.environmentInfo(env.userEnvironmentInfo)
      environment.description(env.description)
    }
    if (opts.contains(FillOpts.People)) {
      environment.updatedBy(
          toPerson(
            env.whoCreated,
            env.parentApplication.portfolio.organization,
            Opts.empty()
          )
      )
      environment.createdBy(toPerson(env.whoCreated))
    }
    if (opts.contains(FillOpts.Features)) {
      if (features != null) {
        environment.features = features
          .map { ef: DbApplicationFeature? -> toApplicationFeature(ef, Opts.empty()) }
      } else {
        environment.features = env.environmentFeatures
          .filter { f: DbFeatureValue ->
            (opts.contains(FillOpts.Archived)
              || f.feature.whenArchived == null)
          }
          .map { ef: DbFeatureValue -> toApplicationFeature(ef.feature, Opts.empty()) }
      }
    }
    if (opts.contains(FillOpts.ServiceAccounts) || opts.contains(FillOpts.SdkURL)) {
      environment.serviceAccountPermission = env.serviceAccountEnvironments
        .filter { sae: DbServiceAccountEnvironment ->
          (opts.contains(FillOpts.Archived)
            || sae.serviceAccount.whenArchived == null)
        }
        .map { sae: DbServiceAccountEnvironment? -> toServiceAccountPermission(sae, null, false, opts) }
    }

    // collect all the ACls for all the groups for this environment?
    if (opts.contains(FillOpts.Acls)) {
      QDbAcl().environment
        .eq(env)
        .findEach { acl: DbAcl? -> environment.addGroupRolesItem(environmentGroupRoleFromAcl(acl)) }
    }
    return environment
  }

  override fun toEnvironment(env: DbEnvironment?, opts: Opts?): Environment? {
    return toEnvironment(env, opts, null)
  }

  override fun getCacheNameByEnvironment(env: DbEnvironment?): String? {
    return QDbNamedCache().organizations.portfolios.applications.environments
      .eq(env)
      .findOneOrEmpty()
      .map { obj: DbNamedCache -> obj.cacheName }
      .orElse(null)
  }

  override fun toServiceAccountPermission(
    sae: DbServiceAccountEnvironment?,
    rolePerms: Set<RoleType?>?,
    mustHaveRolePerms: Boolean,
    opt: Opts?
  ): ServiceAccountPermission? {
    val sap = ServiceAccountPermission()
      .id(sae!!.id)
      .permissions(splitServiceAccountPermissions(sae.permissions)!!)
      .environmentId(sae.environment.id)
    if (sap.permissions.isEmpty() && opt!!.contains(FillOpts.IgnoreEmptyPermissions)) {
      return null
    }

    // if they don't have read, but they really do have read, add read
    if (sap.permissions.isNotEmpty() && !sap.permissions.contains(RoleType.READ)) {
      sap.permissions.add(RoleType.READ)
    }
    if (opt!!.contains(FillOpts.ServiceAccounts) || opt.contains(FillOpts.SdkURL) && !opt.contains(FillOpts.ServiceAccountPermissionFilter)) {
      sap.serviceAccount(
        toServiceAccount(
          sae.serviceAccount, opt.minus(FillOpts.Permissions, FillOpts.SdkURL)
        )
      )
    }
    if (opt.contains(FillOpts.SdkURL)) {
      // if role perms is null (i.e. we don't care) or the roles that a person has is a super-set of
      // the roles of the service account
      if (!mustHaveRolePerms || rolePerms != null && rolePerms.containsAll(sap.permissions)) {
        sap.sdkUrlClientEval(
          String.format(
            "%s/%s",
            sap.environmentId, sae.serviceAccount.apiKeyClientEval
          )
        )
        sap.sdkUrlServerEval(
          String.format(
            "%s/%s",
            sap.environmentId, sae.serviceAccount.apiKeyServerEval
          )
        )
      }
    }
    return sap
  }

  override fun applicationGroupRoleFromAcl(acl: DbAcl?): ApplicationGroupRole {
    return ApplicationGroupRole()
      .groupId(acl!!.group.id)
      .roles(splitApplicationRoles(acl.roles))
      .applicationId(acl.application.id)
  }

  override fun environmentGroupRoleFromAcl(acl: DbAcl?): EnvironmentGroupRole {
    val environmentGroupRole = EnvironmentGroupRole()
      .groupId(acl!!.group.id)
      .roles(splitEnvironmentRoles(acl.roles))
      .environmentId(acl.environment.id)

    // READ should be implicit if we have any of the other roles
    if (!environmentGroupRole.roles.contains(RoleType.READ)
      && environmentGroupRole.roles.isNotEmpty()
    ) {
      environmentGroupRole.addRolesItem(RoleType.READ)
    }
    return environmentGroupRole
  }

  override fun splitEnvironmentRoles(roles: String?): MutableList<RoleType> {
    val roleTypes = mutableSetOf<RoleType>()

    if (roles.isNullOrEmpty()) {
      return ArrayList(roleTypes)
    }

    for (n in roles.split(",").map { it.trim() }.filter { it.isNotBlank() }) {
      try {
        roleTypes.add(RoleType.valueOf(n))
      } catch (ignored: Exception) {
      }
    }

    return roleTypes.toMutableList()
  }

  override fun splitApplicationRoles(roles: String?): MutableList<ApplicationRoleType> {
    val roleTypes = mutableSetOf<ApplicationRoleType>()
    if (roles != null) {
      for (n in roles.split(",").map { it.trim() }.filter { it.isNotBlank() }) {
        try {
          roleTypes.add(ApplicationRoleType.valueOf(n))
        } catch (ignored: Exception) {
        }
      }
    }
    return roleTypes.toMutableList()
  }

  override fun convertEnvironmentAcl(dbAcl: DbAcl?): EnvironmentGroupRole? {
    return EnvironmentGroupRole()
      .environmentId(dbAcl!!.environment.id)
      .groupId(dbAcl.group.id)
  }

  override fun toOff(ldt: LocalDateTime?): OffsetDateTime? {
    return ldt?.atOffset(ZoneOffset.UTC)
  }

  override fun personName(person: DbPerson): String {
    return if (person.name == null || person.name.isEmpty()) {
      "No name"
    } else person.name
  }

  override fun toPerson(person: DbPerson?): Person? {
    return if (person == null) {
      null
    } else Person()
      .id(PersonId().id(person.id))
      .version(person.version)
      .passwordRequiresReset(person.isPasswordRequiresReset)
      .email(person.email)
      .personType(person.personType)
      .name(personName(person))
      .groups(listOf())
  }

  override fun organizationId(): UUID = dbOrganization().id

  override fun dbOrganization(): DbOrganization =
    QDbOrganization().findOne()!!

  override fun hasOrganisation(): Boolean =
    QDbOrganization().exists()

  override fun toPerson(dbp: DbPerson?, opts: Opts): Person? {
    return toPerson(dbp, null, opts)
  }

  override fun toPerson(dbp: DbPerson?, org: DbOrganization?, opts: Opts): Person? {
    if (dbp == null) {
      return null
    }
    if (opts.contains(FillOpts.SimplePeople)) {
      return toPerson(dbp)
    }
    val p = Person()
      .email(dbp.email)
      .name(stripArchived(personName(dbp), dbp.whenArchived))
      .version(dbp.version)
      .passwordRequiresReset(dbp.isPasswordRequiresReset)
      .personType(dbp.personType)
      .whenArchived(toOff(dbp.whenArchived))
      .id(PersonId().id(dbp.id))
    if (opts.contains(FillOpts.PersonLastLoggedIn)) {
      if (dbp.whenLastAuthenticated != null) {
        p.whenLastAuthenticated(dbp.whenLastAuthenticated.atOffset(ZoneOffset.UTC))
      }
      QDbLogin().person.id.eq(dbp.id).orderBy().lastSeen.desc().setMaxRows(1).findList().forEach { login: DbLogin ->
        p.whenLastSeen(login.lastSeen.atOffset(ZoneOffset.UTC))
      }
    }
    if (opts.contains(FillOpts.Groups)) {
      val groupList = QDbGroup().whenArchived
        .isNull.groupMembers.person
        .eq(dbp).owningOrganization.id.eq(if (org == null) organizationId() else org.id)
        .findList()
      log.trace("groups for person {} are {}", p, groupList)
      groupList
        .forEach { dbg: DbGroup? -> p.addGroupsItem(toGroup(dbg, opts.minus(FillOpts.Groups))!!) }
    }
    return p
  }

  override fun toGroup(dbg: DbGroup?, opts: Opts?): Group? {
    if (dbg == null) {
      return null
    }
    val group = Group().version(dbg.version).whenArchived(toOff(dbg.whenArchived))
    group.id = dbg.id
    group.name = stripArchived(dbg.name, dbg.whenArchived)!!
    group.admin = dbg.isAdminGroup
    if (dbg.owningPortfolio != null) {
      group.portfolioId = dbg.owningPortfolio.id
    }
    group.organizationId = if (dbg.owningOrganization == null) null else dbg.owningOrganization.id
    if (opts!!.contains(FillOpts.Members)) {
      val org = if (dbg.owningOrganization == null) dbg.owningPortfolio.organization else dbg.owningOrganization
      group.members = QDbPerson()
        .order().name.asc().whenArchived.isNull.groupMembers.group.eq(dbg).findList()
        .map { p: DbPerson? ->
          this.toPerson(
            p, org, opts.minus(FillOpts.Members, FillOpts.Acls, FillOpts.Groups)
          )
        }
    }
    if (opts.contains(FillOpts.Acls)) {
      val appIdFilter = opts.id(FilterOptType.Application)
      var aclQuery = QDbAcl().group.eq(dbg)
      if (appIdFilter != null) {
        aclQuery = aclQuery
          .or()
          .environment.parentApplication.id.eq(appIdFilter)
          .application.id.eq(appIdFilter)
          .endOr()
      }
      aclQuery.findEach { acl: DbAcl ->
        if (acl.environment != null) {
          group.addEnvironmentRolesItem(environmentGroupRoleFromAcl(acl))
        } else if (acl.application != null) {
          group.addApplicationRolesItem(applicationGroupRoleFromAcl(acl))
        }
      }

      // if this is an admin group and we have no roles, add the create/edit feature roles
      if (group.admin == true) {
        appIdFilter?.let { appId ->
          val agr = group.applicationRoles?.find { appId == it.applicationId }

          if (agr != null) {
            if (agr.roles.isEmpty()) {
              agr.roles = mutableListOf(ApplicationRoleType.EDIT_AND_DELETE, ApplicationRoleType.CREATE)
            }
          } else {
            group.addApplicationRolesItem(ApplicationGroupRole().groupId(group.id!!).applicationId(appId).roles(
              mutableListOf(ApplicationRoleType.EDIT_AND_DELETE, ApplicationRoleType.CREATE)
            ))
          }

          null
        }
      }
    }
    return group
  }

  override fun toApplication(app: DbApplication?, opts: Opts?): Application? {
    if (app == null) {
      return null
    }
    val application = Application()
      .name(stripArchived(app.name, app.whenArchived)!!)
      .description(app.description)
      .id(app.id)
      .version(app.version)
      .whenArchived(toOff(app.whenArchived))
      .portfolioId(app.portfolio.id)
    if (opts!!.contains(FillOpts.Environments)) {
      application.environments =
        QDbEnvironment().whenArchived.isNull.parentApplication.eq(app).findList()
          .map { env: DbEnvironment? -> toEnvironment(env, opts) }

      val envIds = application.environments!!.associate { it.id to it.id }

      // TODO: Remove in 1.6.0
      application.environments!!
        .stream().forEach { e: Environment ->
          if (!envIds.containsKey(e.priorEnvironmentId)) {
            e.priorEnvironmentId = null
          }
        }
    }
    if (opts.contains(FillOpts.Features)) {
      application.features =
        QDbApplicationFeature().whenArchived.isNull.parentApplication.eq(app).findList()
          .map { af: DbApplicationFeature? -> toApplicationFeature(af, opts) }
    }
    return application
  }

  override fun toApplicationFeature(af: DbApplicationFeature?, opts: Opts?): Feature? {
    val feat = Feature()
      .key(stripArchived(af!!.key, af.whenArchived))
      .name(af.name)
      .alias(af.alias)
      .link(af.link)
      .version(af.version)
      .secret(af.isSecret)
      .whenArchived(toOff(af.whenArchived))
      .valueType(af.valueType)
      .description(af.description)
      .id(af.id)
    if (opts!!.contains(FillOpts.MetaData)) {
      feat.metaData(af.metaData)
    }
    return feat
  }

  override fun toFeature(fs: DbFeatureValue?): Feature? {
    if (fs == null) {
      return null
    }
    val f = fs.feature
    return Feature()
      .alias(f.alias)
      .id(f.id)
      .key(stripArchived(f.key, f.whenArchived))
      .link(f.link)
      .name(f.name)
      .secret(f.isSecret)
      .valueType(f.valueType)
      .version(f.version)
  }

  protected fun featureValue(
    actFeature: DbApplicationFeature?, fs: DbFeatureValue?, opts: Opts
  ): FeatureValue? {
    if (fs == null) {
      return null
    }
    val appFeature = actFeature ?: fs.feature
    val featureValue = FeatureValue()
      .key(stripArchived(appFeature.key, appFeature.whenArchived)!!)
      .locked(fs.isLocked)
      .id(fs.id)
      .retired(true == fs.retired)
      .version(fs.version)
    if (appFeature.valueType == FeatureValueType.BOOLEAN) {
      featureValue.valueBoolean(
        if (fs.defaultValue == null) java.lang.Boolean.FALSE else java.lang.Boolean.parseBoolean(fs.defaultValue)
      )
    }
    if (appFeature.valueType == FeatureValueType.JSON) {
      featureValue.valueJson(fs.defaultValue)
    }
    if (appFeature.valueType == FeatureValueType.STRING) {
      featureValue.valueString(fs.defaultValue)
    }
    if (appFeature.valueType == FeatureValueType.NUMBER) {
      featureValue.valueNumber(
        if (fs.defaultValue == null) null else BigDecimal(fs.defaultValue)
      )
    }
    featureValue.environmentId = fs.environment.id
    if (opts.contains(FillOpts.RolloutStrategies)) {
      featureValue.rolloutStrategies = fs.rolloutStrategies
      featureValue.rolloutStrategyInstances =
        fs.sharedRolloutStrategies?.map { srs: DbStrategyForFeatureValue ->
          val rolloutStrategy = srs.rolloutStrategy
          RolloutStrategyInstance()
            .value(
              sharedRolloutStrategyToObject(
                srs.value, appFeature.valueType
              )
            )
            .name(rolloutStrategy.name)
            .disabled(if (srs.isEnabled == true) null else true)
            .strategyId(rolloutStrategy.id)
        } ?: listOf()
    }

    // this is an indicator it is for the UI not for the cache.
    if (opts.contains(FillOpts.People)) {
      featureValue.whenUpdated = toOff(fs.whenUpdated)
      featureValue.whoUpdated = toPerson(fs.whoUpdated)
    }
    return featureValue
  }

  private fun sharedRolloutStrategyToObject(value: String, valueType: FeatureValueType): Any {
    return when (valueType) {
      FeatureValueType.BOOLEAN -> java.lang.Boolean.parseBoolean(value)
      FeatureValueType.STRING, FeatureValueType.JSON -> value
      FeatureValueType.NUMBER -> BigDecimal(value)
    }
  }

  override fun toFeatureValue(fs: DbFeatureValue?, opts: Opts?): FeatureValue? {
    return featureValue(null, fs, opts!!)
  }

  override fun toFeatureValue(fs: DbFeatureValue?): FeatureValue? {
    return featureValue(null, fs, Opts.opts(FillOpts.People, FillOpts.RolloutStrategies))
  }

  override fun toFeatureValue(feature: DbApplicationFeature?, value: DbFeatureValue?): FeatureValue? {
    return featureValue(feature, value, Opts.opts(FillOpts.People))
  }

  override fun toFeatureValue(
    feature: DbApplicationFeature?, value: DbFeatureValue?, opts: Opts?
  ): FeatureValue? {
    return if (value == null) {
      FeatureValue()
        .id(feature!!.id)
        .key(stripArchived(feature.key, feature.whenArchived)!!)
        .version(0L)
        .locked(false)
    } else featureValue(feature, value, opts!!)
  }

  override fun toApplicationRolloutStrategy(rs: DbApplicationRolloutStrategy?, opts: Opts?): ApplicationRolloutStrategy? {
    if (rs == null) {
      return null
    }

    val info = ApplicationRolloutStrategy()
      .id(rs.id)
      .name(rs.name)
      .disabled(rs.isDisabled)
      .attributes(rs.attributes)
      .colouring(rs.colouring)
      .avatar(rs.avatar)

//    if (opts!!.contains(FillOpts.SimplePeople)) {
//      info.changedBy(toPerson(rs.whoChanged)!!)
//    }
    return info
  }

  override fun byStrategy(id: UUID?): DbApplicationRolloutStrategy? {
    return if (id == null) null else QDbApplicationRolloutStrategy().id.eq(id).findOne()
  }

  override fun toPortfolio(p: DbPortfolio?, opts: Opts?): Portfolio? {
    val pid: UUID? = null

    return toPortfolio(p, opts, pid, true)
  }

  override fun toPortfolio(p: DbPortfolio?, opts: Opts?, person: Person?, personNotSuperAdmin: Boolean): Portfolio? {
    return toPortfolio(p, opts, person?.id?.id, personNotSuperAdmin)
  }

  override fun toPortfolio(p: DbPortfolio?, opts: Opts?, personId: UUID?, personNotSuperAdmin: Boolean): Portfolio? {
    if (p == null) {
      return null
    }
    val portfolio = Portfolio()
      .name(stripArchived(p.name, p.whenArchived))
      .description(p.description)
      .version(p.version)
      .organizationId(p.organization.id)
      .id(p.id)
    if (opts!!.contains(FillOpts.Portfolios)) {
      portfolio
        .whenCreated(toOff(p.whenCreated))
        .whenUpdated(toOff(p.whenUpdated))
        .createdBy(toPerson(p.whoCreated, p.organization, Opts.empty()))
    }
    if (opts.contains(FillOpts.Groups)) {
      portfolio.groups =
        QDbGroup().whenArchived.isNull.owningPortfolio.eq(p).order().name.asc().findList()
          .map { g: DbGroup? -> toGroup(g, opts) }
    }
    if (opts.contains(FillOpts.Applications)) {
      var appFinder = QDbApplication()
        .whenArchived.isNull
        .portfolio.eq(p)
        .order().name.asc()

      personId?.let {
        val portAdmin = isPersonMemberOfPortfolioAdminGroup(portfolio.id, personId)
        if (personNotSuperAdmin && !portAdmin) {
          appFinder = appFinder.or()
            .environments.groupRolesAcl.group.groupMembers.person.id.eq(personId)
            .groupRolesAcl.group.groupMembers.person.id.eq(personId).endOr()
        }
      }

      portfolio.applications = appFinder.findList()
          .map { a: DbApplication? -> toApplication(a, opts) }
    }
    return portfolio
  }

  override fun toOrganization(org: DbOrganization?, opts: Opts?): Organization? {
    if (org == null) {
      return null
    }
    val organisation = Organization()
      .name(stripArchived(org.name, org.whenArchived)!!)
      .id(org.id)
      .whenArchived(toOff(org.whenArchived))
      .admin(true)
    if (opts!!.contains(FillOpts.Groups)) {
      organisation.orgGroup(
        toGroup(
          QDbGroup().adminGroup
            .isTrue.owningPortfolio
            .isNull.owningOrganization
            .eq(org)
            .findOne(),
          Opts.empty()
        )
      )
    }
    return organisation
  }

  override fun byGroup(gid: UUID?, opts: Opts?): DbGroup? {
    if (gid == null) {
      return null
    }
    var eq = QDbGroup().id.eq(gid)
    if (opts!!.contains(FillOpts.Members)) {
      eq = eq.groupMembers.person.fetch()
    }
    return eq.findOne()
  }

  override fun byPerson(creator: Person?): DbPerson? {
    return if (creator?.id?.id == null) {
      null
    } else byPerson(creator.id!!.id)
  }

  override fun isPersonEnvironmentAdmin(current: Person, environmentId: UUID): Boolean {
    return QDbGroup()
      .adminGroup.isTrue
      .whenArchived.isNull
      .groupMembers.person.id.eq(current.id?.id)
      .owningPortfolio.applications.environments.id.eq(environmentId).exists()
  }

  /** is this person a superuser or portfolio admin for this application  */
  override fun isPersonApplicationAdmin(dbPerson: DbPerson?, app: DbApplication?): Boolean {
    if (dbPerson == null || app == null) return false

    return isPersonApplicationAdmin(dbPerson.id, app.id)
  }

  override fun isPersonApplicationAdmin(personId: UUID, appId: UUID): Boolean {
    // if a person is in a null portfolio group or portfolio group
    return QDbGroup()
      .groupMembers.person.id.eq(personId)
      .adminGroup.isTrue
      .or()
        .owningPortfolio.isNull
        .owningPortfolio.applications.id.eq(appId)
      .endOr()
      .exists()
  }

  override fun toServiceAccount(sa: DbServiceAccount?, opts: Opts?): ServiceAccount? {
    return toServiceAccount(sa, opts, null)
  }

  override fun toServiceAccount(
    sa: DbServiceAccount?, opts: Opts?, environmentsUserHasAccessTo: List<DbAcl?>?
  ): ServiceAccount? {
    if (sa == null) {
      return null
    }
    val account = ServiceAccount()
      .id(sa.id)
      .version(sa.version)
      .whenArchived(toOff(sa.whenArchived))
      .portfolioId(sa.portfolio.id)
      .name(sa.name)
      .description(sa.description)
    if (opts != null) {
      if (!opts.contains(FillOpts.ServiceAccountPermissionFilter)) {
        account.apiKeyServerSide(sa.apiKeyServerEval)
        account.apiKeyClientSide(sa.apiKeyClientEval)
      }
      if (opts.contains(FillOpts.Permissions) || opts.contains(FillOpts.SdkURL)) {
        // envId, acl
        val envs = mutableMapOf<UUID, MutableSet<RoleType?>>()

        // we need to figure out what kinds of roles this person has in each environment
        // so that they can't see an SDK URL that has more permissions than they do
        environmentsUserHasAccessTo?.forEach { acl: DbAcl? ->
          val e = envs.getOrPut(acl!!.environment.id) { mutableSetOf() }

          e.addAll(includeImplicitRead(splitEnvironmentRoles(acl.roles)))
        }
        val appIdFilter = opts.id(FilterOptType.Application)
        var permQuery =
          QDbServiceAccountEnvironment().serviceAccount.eq(sa).environment.whenArchived.isNull.environment.whenUnpublished.isNull
        if (appIdFilter != null) {
          permQuery = permQuery.environment.parentApplication.id.eq(appIdFilter)
        }
        account.permissions = permQuery.findList()
          .map { sae: DbServiceAccountEnvironment ->
            toServiceAccountPermission(
              sae,
              envs[sae.environment.id],
              envs.isNotEmpty(),
              opts.minus(FillOpts.ServiceAccounts)
            )
          }
          .filter { obj: ServiceAccountPermission? -> Objects.nonNull(obj) }
      }
    }
    return account
  }

  private fun includeImplicitRead(splitEnvironmentRoles: List<RoleType?>): List<RoleType?> {
    val roles: MutableList<RoleType?> = ArrayList(splitEnvironmentRoles)
    if (splitEnvironmentRoles.isNotEmpty() && !splitEnvironmentRoles.contains(RoleType.READ)) {
      roles.add(RoleType.READ)
    }
    return roles
  }

  override fun toFeatureEnvironment(
    featureValue: DbFeatureValue?, roles: List<RoleType?>, dbEnvironment: DbEnvironment, opts: Opts
  ): FeatureEnvironment {
    val featureEnvironment = FeatureEnvironment()
      .environment(toEnvironment(dbEnvironment, Opts.empty())!!)
      .roles(roles)
      .featureValue(toFeatureValue(featureValue))
    if (opts.contains(FillOpts.ServiceAccounts)) {
      featureEnvironment.serviceAccounts(
        dbEnvironment.serviceAccountEnvironments.stream()
          .filter { sae: DbServiceAccountEnvironment ->
            (opts.contains(FillOpts.Archived)
              || sae.serviceAccount.whenArchived == null)
          }
          .map { sae: DbServiceAccountEnvironment -> toServiceAccount(sae.serviceAccount, null, null) }
          .sorted(
            Comparator.comparing { obj: ServiceAccount? -> obj!!.id }) // this is really only because the test is finicky, it should be
          // removed
          .collect(Collectors.toList())
      )
    }
    return featureEnvironment
  }

  override fun getSuperuserGroup(opts: Opts?): Group? {
    val g = QDbGroup().owningOrganization.id.eq(
      organizationId()
    ).adminGroup
      .isTrue.owningPortfolio
      .isNull.groupMembers.person
      .fetch()
      .findOne()
    return g?.let { toGroup(it, opts) }
  }

  companion object {
    private val log = LoggerFactory.getLogger(ConvertUtils::class.java)
  }
}
