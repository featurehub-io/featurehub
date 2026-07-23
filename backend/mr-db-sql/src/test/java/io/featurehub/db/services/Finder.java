package io.featurehub.db.services;

import io.featurehub.db.model.*;
import io.featurehub.db.model.query.*;

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

  public static DbFeatureValue findFeatureValue(UUID envId, String key) {
    return new QDbFeatureValue().environment.id.eq(envId).feature.key.eq(key).findOne();
  }

  public static DbPortfolioRolloutStrategy findPortfolioRolloutStrategy(UUID id) {
    return new QDbPortfolioRolloutStrategy().id.eq(id).findOne();
  }
}
