package io.featurehub.db.model;

import io.ebean.Model;
import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.Index;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fh_celog")
@Index(name = "idx_cloudevents", columnNames = {"type", "link_type", "link", "when_upd"})
@Index(name = "idx_cloudevents_st", columnNames = {"type", "link_type", "link", "s"})
@Index(name = "idx_cloudevents_owner", columnNames = {"id", "fk_org", "type"})
@ChangeLog
public class DbCloudEventLog extends Model {

  @Id
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_org")
  @Column(name = "fk_org")
  @NotNull
  private DbOrganization owner;

  @Version
  private long version;

  @Column(nullable = false)
  private final String type;

  /**
   * Indicates what type the "link" key is
   * e.g. app, env, org, feature, etc
   */
  @Column(nullable = false, length = 6, name = "link_type")
  private final String linkType;

  /**
   * the link key, e.g. app id, org id, env id, portfolio id
   */
  @Column(nullable = false)
  private final UUID link;

  @Lob
  @Column(nullable = false)
  private final String data;

  @Lob
  private final String metadata;

  @Lob
  @Column(name = "te")
  private String trackedEvents;

  @WhenModified
  @NotNull
  @Column(name = "when_upd")
  private Instant whenUpdated;

  @WhenCreated
  @NotNull
  @Column(name = "when_cre")
  private Instant whenCreated;

  @Column(name = "s", nullable = true)
  private Integer status;

  public DbCloudEventLog(@NotNull UUID id, @NotNull DbOrganization owner, @NotNull String type,
                         @NotNull String linkType, @NotNull UUID link, @NotNull String data,
                         @NotNull Instant whenCreated,
                         @Nullable String metadata) {
    this.id = id;
    this.owner = owner;
    this.type = type;
    this.linkType = linkType;
    this.link = link;
    this.data = data;
    this.metadata = metadata;
    this.whenUpdated = whenCreated;
    this.whenCreated = whenCreated;
  }

  @Nullable
  public UUID getId() {
    return id;
  }

  @NotNull
  public String getType() {
    return type;
  }

  @NotNull
  public String getLinkType() {
    return linkType;
  }

  @NotNull
  public UUID getLink() {
    return link;
  }

  @NotNull
  public String getData() {
    return data;
  }

  @Nullable
  public String getMetadata() {
    return metadata;
  }

  public void setId(@NotNull UUID id) {
    this.id = id;
  }

  @Nullable
  public String getTrackedEvents() {
    return trackedEvents;
  }

  public void setTrackedEvents(@Nullable String trackedEvents) {
    this.trackedEvents = trackedEvents;
  }

  public long getVersion() {
    return version;
  }

  // used in testing
  public void setVersion(long version) {
    this.version = version;
  }

  @NotNull
  public Instant getWhenCreated() {
    return whenCreated;
  }

  @NotNull
  public Instant getWhenUpdated() {
    return whenUpdated;
  }

  public void setWhenUpdated(@NotNull Instant whenUpdated) {
    this.whenUpdated = whenUpdated;
  }

  @Nullable
  public Integer getStatus() {
    return status;
  }

  public void setStatus(@Nullable Integer status) {
    this.status = status;
  }

  @NotNull
  public DbOrganization getOwner() {
    return owner;
  }
}
