package io.featurehub.db.api;

import io.featurehub.mr.model.SetupSiteAdmin;

public interface SetupApi {
  boolean initialized();

  boolean setup(SetupSiteAdmin site);
}
