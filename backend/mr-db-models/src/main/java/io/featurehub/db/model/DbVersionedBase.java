package io.featurehub.db.model;

import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

@MappedSuperclass
public class DbVersionedBase extends DbBase {
  @Version
  private long version;

  public long getVersion() {
    return version;
  }
}
