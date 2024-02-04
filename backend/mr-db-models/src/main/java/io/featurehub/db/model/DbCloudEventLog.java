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
@Index(name = "idx_cloudevents", columnNames = {"type", "link_type", "link"})
@ChangeLog
public class DbCloudEventLog extends Model {
  @Id
  private UUID id;

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

  public DbCloudEventLog(@NotNull UUID id, @NotNull String type, @NotNull String linkType, @NotNull UUID link, @NotNull String data, @Nullable String metadata) {
    this.id = id;
    this.type = type;
    this.linkType = linkType;
    this.link = link;
    this.data = data;
    this.metadata = metadata;
    this.whenUpdated = Instant.now();
    this.whenCreated = this.whenUpdated;
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

  public void setId(UUID id) {
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

  public Instant getWhenUpdated() {
    return whenUpdated;
  }

  public void setWhenUpdated(Instant whenUpdated) {
    this.whenUpdated = whenUpdated;
  }
}
