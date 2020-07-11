package io.featurehub.db.services;

import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.query.QDbOrganization;
import io.featurehub.db.model.query.QDbPerson;
import io.featurehub.db.model.query.QDbPortfolio;

import java.util.UUID;

public class Finder {
  public static DbPerson findByEmail(String email) {
    return new QDbPerson().email.eq(email).findOne();
  }

  public static DbPortfolio findPortfolioById(String id) {
    return new QDbPortfolio().id.eq(UUID.fromString(id)).findOne();
  }

  public static DbOrganization findDbOrganization() {
    return new QDbOrganization().findOne();
  }
}
