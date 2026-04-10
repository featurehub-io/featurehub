package io.featurehub.db.services;

import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbFeatureFilter;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.db.model.query.QDbApplicationFeature;
import io.featurehub.db.model.query.QDbFeatureFilter;
import io.featurehub.db.model.query.QDbOrganization;
import io.featurehub.db.model.query.QDbPerson;
import io.featurehub.db.model.query.QDbPortfolio;
import io.featurehub.db.model.query.QDbServiceAccount;

import java.util.UUID;

public class Finder {
  public static DbPerson findByEmail(String email) {
    return new QDbPerson().email.eq(email).findOne();
  }

  public static DbPortfolio findPortfolioById(UUID id) {
    return new QDbPortfolio().id.eq(id).findOne();
  }

  public static DbOrganization findDbOrganization() {
    return new QDbOrganization().findOne();
  }

  public static DbFeatureFilter findFeatureFilterById(UUID id) {
    return new QDbFeatureFilter().id.eq(id).findOne();
  }

  /** Returns the feature with its filters association eagerly fetched. */
  public static DbApplicationFeature findApplicationFeatureWithFilters(UUID id) {
    return new QDbApplicationFeature().id.eq(id).filters.fetch().findOne();
  }

  /** Returns the service account with its featureFilters association eagerly fetched. */
  public static DbServiceAccount findServiceAccountWithFilters(UUID id) {
    return new QDbServiceAccount().id.eq(id).featureFilters.fetch().findOne();
  }
}
