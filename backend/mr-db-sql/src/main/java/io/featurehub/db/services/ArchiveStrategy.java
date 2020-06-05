package io.featurehub.db.services;

import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbGroup;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.DbServiceAccount;

public interface ArchiveStrategy {
  void archivePortfolio(DbPortfolio portfolio);
  void archiveApplication(DbApplication application);
  void archiveEnvironment(DbEnvironment environment);
  void archiveOrganization(DbOrganization organization);
  void archiveServiceAccount(DbServiceAccount serviceAccount);
  void archiveGroup(DbGroup group);
  void archiveApplicationFeature(DbApplicationFeature feature);
  void archivePerson(DbPerson person);
}
