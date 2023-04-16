package io.featurehub.db.model;

import io.ebean.Model;
import org.jetbrains.annotations.NotNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import io.ebean.annotation.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="fh_webhook")
@Index(unique = false, name = "idx_webhook", columnNames = {"environment_id","when_sent"})
@Index(unique = false, name = "idx_webhook_type", columnNames = {"environment_id","when_sent", "whce_type"})
public class DbWebhookResult extends Model {
  @Id
  @NotNull
  private UUID id;

  @ManyToOne(optional = false)

  @NotNull
  private final DbEnvironment environment;

  @Column(nullable = false)
  @NotNull
  private final OffsetDateTime whenSent;

  @Column(length = 7, nullable = false)
  @NotNull
  private final String method;

  @Column(nullable = false)
  @NotNull
  private final Integer status;

  // this is the format of the data held by the webhook
  @Column(nullable = false, length = 100, name = "ce_type")
  @NotNull
  private final String cloudEventType;

  // this is the format of the json data
  @Column(nullable = false, length = 100, name="whce_type")
  @NotNull
  private final String webhookCloudEventType;

  @Column(nullable = false)
  @Lob
  @NotNull
  private final String json;

  public DbWebhookResult(@NotNull DbEnvironment environment, @NotNull OffsetDateTime whenSent,
                         @NotNull String method, @NotNull Integer status, @NotNull String cloudEventType,
                         @NotNull String webhookCloudEventType, @NotNull String json) {
    this.environment = environment;
    this.whenSent = whenSent;
    this.method = method;
    this.status = status;
    this.cloudEventType = cloudEventType;
    this.webhookCloudEventType = webhookCloudEventType;
    this.json = json;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public @NotNull DbEnvironment getEnvironment() {
    return environment;
  }

  public @NotNull OffsetDateTime getWhenSent() {
    return whenSent;
  }

  public String getWebhookCloudEventType() {
    return webhookCloudEventType;
  }

  public @NotNull String getMethod() {
    return method;
  }

  public @NotNull Integer getStatus() {
    return status;
  }

  public @NotNull String getCloudEventType() {
    return cloudEventType;
  }

  public @NotNull String getJson() {
    return json;
  }
}
