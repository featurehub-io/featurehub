package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.SetupApi;
import io.featurehub.db.model.DbPerson;
import io.featurehub.mr.model.SetupSiteAdmin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class SetupSqlApi implements SetupApi {
  private final Database database;
  private final Conversions convertUtils;

  @Inject
  public SetupSqlApi(Database database, Conversions convertUtils) {
    this.database = database;
    this.convertUtils = convertUtils;
  }

  @Override
  public boolean initialized() {
    return convertUtils.dbOrganization() != null;
  }

  @Override
  @Transactional
  public boolean setup(SetupSiteAdmin setupSiteAdmin) {
    assert setupSiteAdmin.getEmailAddress() != null && setupSiteAdmin.getName() != null;

    if (!initialized()) {
      database.save(new DbPerson.Builder().email(setupSiteAdmin.getEmailAddress()).name(setupSiteAdmin.getName()));
      return true;
    }
    return false;
  }
}
