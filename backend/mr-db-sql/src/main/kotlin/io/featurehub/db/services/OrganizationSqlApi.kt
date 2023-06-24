package io.featurehub.db.services

import io.ebean.Database
import io.ebean.annotation.Transactional
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.OrganizationApi
import io.featurehub.db.model.DbNamedCache
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.query.QDbNamedCache
import io.featurehub.db.model.query.QDbOrganization
import io.featurehub.mr.model.Organization
import io.featurehub.publish.ChannelConstants
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
    var cache = QDbNamedCache().cacheName.eq(ChannelConstants.DEFAULT_CACHE_NAME).findOne()
    if (cache == null) {
      cache = DbNamedCache.Builder().cacheName(ChannelConstants.DEFAULT_CACHE_NAME).build()
      cache.save()
    }
    val newOrg = DbOrganization.Builder().name(organization.name).namedCache(cache).build()
    newOrg.save()
    return convertUtils.toOrganization(newOrg, Opts.opts(FillOpts.Groups))!!
  }
}
