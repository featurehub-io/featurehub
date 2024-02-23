package io.featurehub.db.model;

import io.ebean.Model;
import io.ebean.annotation.ChangeLog;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Entity
@Table(name = "fh_sysconfig")
@ChangeLog
public class DbSystemConfig extends Model {
  @EmbeddedId
  private final DbSystemConfigKey id;

  @ManyToOne
  @Column(name = "fk_who_updated", nullable = false)
  @JoinColumn(name = "fk_who_updated")
  private DbPerson whoUpdated;

  @Version
  private long version;

  @Lob
  private String value;

  public DbSystemConfig(@NotNull String key, @NotNull UUID organisationId, @NotNull DbPerson whoUpdated) {
    id = new DbSystemConfigKey(key, organisationId);
    this.whoUpdated = whoUpdated;
  }

  @ManyToOne(optional = false)
  @Column(name = "org_id")
  @JoinColumn(name="org_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
  @MapsId("org_id")
  private DbOrganization organization;

  @NotNull public DbSystemConfigKey getId() {
    return id;
  }

  @NotNull public String getKey() {
    return id.getKey();
  }

  @NotNull public DbOrganization getOrganization() {
    return organization;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public DbPerson getWhoUpdated() {
    return whoUpdated;
  }

  public void setWhoUpdated(DbPerson whoUpdated) {
    this.whoUpdated = whoUpdated;
  }
}
