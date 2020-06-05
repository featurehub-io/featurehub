package io.featurehub.db.services;

import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.query.QDbPerson;

public class Finder {
  public static DbPerson findByEmail(String email) {
    return new QDbPerson().email.eq(email).findOne();
  }
}
