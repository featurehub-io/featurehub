package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.OrganizationApi;
import io.featurehub.db.model.DbNamedCache;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.query.QDbNamedCache;
import io.featurehub.mr.model.Organization;
import io.featurehub.publish.ChannelConstants;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class OrganizationSqlApi implements OrganizationApi {
  private final Database database;
  private final Conversions convertUtils;

  @Inject
  public OrganizationSqlApi(Database database, Conversions convertUtils) {
    this.database = database;
    this.convertUtils = convertUtils;
  }

  public Organization get() {
    return convertUtils.toOrganization(convertUtils.dbOrganization(), Opts.opts(FillOpts.Groups));
  }

  @Transactional
  @Override
  public Organization save(Organization organization) {
    DbNamedCache cache = new QDbNamedCache().cacheName.eq(ChannelConstants.DEFAULT_CACHE_NAME).findOne();

    if (cache == null) {
      cache = new DbNamedCache.Builder().cacheName(ChannelConstants.DEFAULT_CACHE_NAME).build();
      database.save(cache);
    }

    DbOrganization newOrg = new DbOrganization.Builder().name(organization.getName()).namedCache(cache).build();

    database.save(newOrg);

    return convertUtils.toOrganization(newOrg, Opts.opts(FillOpts.Groups));
  }
}
