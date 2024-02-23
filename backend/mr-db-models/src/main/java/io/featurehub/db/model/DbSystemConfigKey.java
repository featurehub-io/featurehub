package io.featurehub.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Embeddable
public class DbSystemConfigKey {
  @Column(nullable = false, name = "key")
  @NotNull
  private final String key;

  @Column(nullable = false, name = "org_id")
  @NotNull
  private final UUID orgId;

  public DbSystemConfigKey(@NotNull String key, @NotNull UUID orgId) {
    this.key = key;
    this.orgId = orgId;
  }

  @NotNull
  public String getKey() {
    return key;
  }

  @NotNull
  public UUID getOrgId() {
    return orgId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DbSystemConfigKey that = (DbSystemConfigKey) o;

    if (!getKey().equals(that.getKey())) return false;
    return getOrgId().equals(that.getOrgId());
  }

  @Override
  public int hashCode() {
    int result = getKey().hashCode();
    result = 31 * result + getOrgId().hashCode();
    return result;
  }
}
