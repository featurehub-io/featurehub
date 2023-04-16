package io.featurehub.db.model;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public class DbVersionedBase extends DbBase {
  @Version
  private long version;

  public long getVersion() {
    return version;
  }
}
