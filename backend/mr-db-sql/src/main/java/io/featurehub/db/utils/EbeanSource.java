package io.featurehub.db.utils;

import io.ebean.Database;

import javax.sql.DataSource;

public interface EbeanSource {
  Database getEbeanServer();
  DataSource getDatasource();
}
