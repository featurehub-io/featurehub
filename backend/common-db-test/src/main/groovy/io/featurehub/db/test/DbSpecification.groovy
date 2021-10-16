package io.featurehub.db.test

import io.ebean.DB
import io.ebean.Database
import spock.lang.Specification

/**
 * This base class allows us to rollback transactional changes after every test so the database is clean,
 * and tests don't interfere with each other.
 */
class DbSpecification extends Specification {
  Database db

  def setup() {
    db = DB.getDefault()

    // put all changes in a transaction
    DB.beginTransaction()
  }

  def cleanup() {
    // roll all changes back again, so we can isolate our tests. if Exceptions are thrown, then there is no current transaction
    if (DB.currentTransaction() != null) {
      DB.rollbackTransaction()
    }
  }
}
