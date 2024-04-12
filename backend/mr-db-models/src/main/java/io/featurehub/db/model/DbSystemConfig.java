package io.featurehub.db.model;

import io.ebean.Model;
import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fh_sysconfig")
@ChangeLog
public class DbSystemConfig extends Model {
  @EmbeddedId
  private final DbSystemConfigKey id;

  @ManyToOne
  @Column(name = "fk_who_updated", nullable = true)
  @JoinColumn(name = "fk_who_updated")
  private DbPerson whoUpdated;

  @NotNull
  @WhenModified
  @Column(name = "w_upd")
  private Instant whenLastUpdated;

  @NotNull
  @WhenCreated
  @Column(name = "w_cre")
  private final Instant whenCreated;

  @Version
  private long version;

  @Lob
  @Column(name = "vl")
  private String value;

  public DbSystemConfig(@NotNull String key, @NotNull UUID organisationId, @Nullable DbPerson whoUpdated) {
    id = new DbSystemConfigKey(key, organisationId);
    this.whoUpdated = whoUpdated;
    whenCreated = Instant.now();
    whenLastUpdated = whenCreated;
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

  @Nullable
  public String getValue() {
    return value;
  }

  public void setValue(@Nullable String value) {
    this.value = value;
  }

  @Nullable
  public DbPerson getWhoUpdated() {
    return whoUpdated;
  }

  public void setWhoUpdated(@Nullable DbPerson whoUpdated) {
    this.whoUpdated = whoUpdated;
  }

  public Instant getWhenLastUpdated() {
    return whenLastUpdated;
  }

  public Instant getWhenCreated() {
    return whenCreated;
  }

  public long getVersion() {
    return version;
  }
}
