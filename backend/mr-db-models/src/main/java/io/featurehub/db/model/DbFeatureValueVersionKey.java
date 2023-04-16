package io.featurehub.db.model;

import io.ebean.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class DbFeatureValueVersionKey {
  @Column(nullable = false)
  protected UUID id;
  @Column(nullable = false)
  protected long version;

  public DbFeatureValueVersionKey(UUID id, long version) {
    this.id = id;
    this.version = version;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbFeatureValueVersionKey that = (DbFeatureValueVersionKey) o;
    return version == that.version && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, version);
  }
}
