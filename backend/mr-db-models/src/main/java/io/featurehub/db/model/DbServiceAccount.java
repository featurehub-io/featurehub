package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.Index;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Index(unique = true, name = "idx_service_name", columnNames = {"fk_portfolio_id", "name"})
@Entity
@Table(name = "fh_service_account")
@ChangeLog
public class DbServiceAccount extends DbVersionedBase {
  @Column(length = 100)
  @NotNull
  private String name;

  @Column(length = 400)
  @Nullable
  private String description;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_service_account_id")
  @NotNull
  private Set<DbServiceAccountEnvironment> serviceAccountEnvironments = new LinkedHashSet<>();

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_changed_by")
  @Column(name = "fk_changed_by")
  @NotNull
  private DbPerson whoChanged;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_person_who_created")
  @Column(name = "fk_person_who_created")
  @NotNull
  private DbPerson whoCreated;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_sdk_person")
  @Column(name = "fk_sdk_person", nullable = false)
  @NotNull
  private DbPerson sdkPerson;

  @Column(name = "api_key", unique = true, nullable = false, length = 100)
  @NotNull
  private String apiKeyServerEval;
  @Column(name = "api_key_client_eval", unique = true, length = 100, nullable = false)
  @NotNull
  private String apiKeyClientEval;

  @Column(name = "when_archived")
  @Nullable
  private LocalDateTime whenArchived;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_portfolio_id")
  @Column(name = "fk_portfolio_id", nullable = false)
  @NotNull
  private DbPortfolio portfolio;

  public DbServiceAccount(@NotNull DbPerson whoCreated, @NotNull DbPerson sdkPerson, @NotNull String name,
                          @Nullable String description, @NotNull String serverKey,
                          @NotNull String clientKey, @NotNull DbPortfolio portfolio ) {
    this.whoChanged = whoCreated;
    this.whoCreated = whoCreated;
    this.sdkPerson = sdkPerson;
    this.name = name;
    this.description = description;
    this.apiKeyServerEval = serverKey;
    this.apiKeyClientEval = clientKey;
    this.portfolio = portfolio;
  }

  /**
   * Indicates this environment is removed from the publishing list for some reason, usually because the organisation
   * expired. This is different from archiving because archiving is an action taken by the customer, whereas
   * unpublishing is a system action.
   */
  private Instant whenUnpublished;

  public Instant getWhenUnpublished() {
    return whenUnpublished;
  }

  public void setWhenUnpublished(Instant whenUnpublished) {
    this.whenUnpublished = whenUnpublished;
  }

  public @NotNull DbPerson getWhoCreated() {
    return whoCreated;
  }

  public void setWhoCreated(@NotNull DbPerson whoCreated) {
    this.whoCreated = whoCreated;
  }

  public @NotNull DbPerson getSdkPerson() {
    return sdkPerson;
  }

  public void setSdkPerson(@NotNull DbPerson sdkPerson) {
    this.sdkPerson = sdkPerson;
  }

  public @NotNull DbPortfolio getPortfolio() {
    return portfolio;
  }

  public void setPortfolio(@NotNull DbPortfolio portfolio) {
    this.portfolio = portfolio;
  }

  public @NotNull String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public @NotNull Set<DbServiceAccountEnvironment> getServiceAccountEnvironments() {
    return serviceAccountEnvironments;
  }

  public @NotNull String getApiKeyServerEval() {
    return apiKeyServerEval;
  }

  public void setApiKeyServerEval(@NotNull String apiKeyServerEval) {
    this.apiKeyServerEval = apiKeyServerEval;
  }

  public void setServiceAccountEnvironments(@NotNull Set<DbServiceAccountEnvironment> serviceAccountEnvironments) {
    this.serviceAccountEnvironments = serviceAccountEnvironments;
  }

  public @NotNull DbPerson getWhoChanged() {
    return whoChanged;
  }

  public void setWhoChanged(@NotNull DbPerson whoChanged) {
    this.whoChanged = whoChanged;
  }

  public @Nullable LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(@Nullable LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public @NotNull String getApiKeyClientEval() {
    return apiKeyClientEval;
  }

  public void setApiKeyClientEval(@NotNull String apiKeyClientEval) {
    this.apiKeyClientEval = apiKeyClientEval;
  }
}
