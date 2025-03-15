package io.featurehub.db.services

import io.ebean.annotation.Transactional
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.OrganizationApi
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.mr.model.Organization
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class OrganizationSqlApi @Inject constructor(private val convertUtils: Conversions) :
  OrganizationApi {
  override fun get(): Organization? {
    return if (convertUtils.hasOrganisation()) {
      convertUtils.toOrganization(convertUtils.dbOrganization(), Opts.opts(FillOpts.Groups))!!
    } else null
  }

  override fun hasOrganisation(): Boolean {
    return QDbOrganization().exists()
  }

  @Transactional
  override fun save(organization: Organization): Organization? {
    val newOrg = DbOrganization.Builder().name(organization.name).build()
    newOrg.save()
    return convertUtils.toOrganization(newOrg, Opts.opts(FillOpts.Groups))!!
  }
}
