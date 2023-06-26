package io.featurehub.db.model;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
public class DbBase extends Model {
  @Id
  private UUID id;

  @WhenModified
  @Column(name = "when_updated")
  private LocalDateTime whenUpdated;

  @WhenCreated
  @Column(name = "when_created")
  private LocalDateTime whenCreated;

  public UUID getId() { return id; }

  public void setId(UUID id) {
    this.id = id;
  }

  public LocalDateTime getWhenUpdated() {
    return whenUpdated;
  }

  public LocalDateTime getWhenCreated() {
    return whenCreated;
  }
}
