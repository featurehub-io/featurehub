package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.Index;
import io.featurehub.mr.model.FeatureValueType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Index(
    unique = true,
    name = "idx_app_features",
    columnNames = {"fk_app_id", "feature_key"})
@Entity
@Table(name = "fh_app_feature")
@ChangeLog
public class DbApplicationFeature extends DbVersionedBase {
  private DbApplicationFeature(Builder builder) {
    setParentApplication(builder.parentApplication);
    setKey(builder.key);
    setAlias(builder.alias);
    setName(builder.name);
    setSecret(builder.secret);
    setLink(builder.link);
    setValueType(builder.valueType);
    setMetaData(builder.metaData);
    setDescription(builder.description);
  }

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_app_id")
  @Column(name = "fk_app_id", nullable = false)
  private DbApplication parentApplication;

  @Column(name = "when_archived")
  @Nullable
  private LocalDateTime whenArchived;

  @Column(name = "feature_key")
  private String key;

  private String alias;
  private String name;
  private boolean secret;

  @Column(length = 600)
  private String link;

  @Column(length = 300)
  private String description;

  @Lob private String metaData;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "value_type", nullable = false)
  @NotNull
  private FeatureValueType valueType;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_feature_id")
  private Set<DbFeatureValue> environmentFeatures;

  @NotNull
  public DbApplication getParentApplication() {
    return parentApplication;
  }

  public void setParentApplication(@NotNull DbApplication parentApplication) {
    this.parentApplication = parentApplication;
  }

  @NotNull
  public String getKey() {
    return key;
  }

  public void setKey(@NotNull String key) {
    this.key = key;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  @NotNull
  public Set<DbFeatureValue> getEnvironmentFeatures() {
    if (environmentFeatures == null) {
      environmentFeatures = new HashSet<>();
    }

    return environmentFeatures;
  }

  public void setEnvironmentFeatures(@NotNull Set<DbFeatureValue> environmentFeatures) {
    this.environmentFeatures = environmentFeatures;
  }

  public boolean isSecret() {
    return secret;
  }

  public void setSecret(boolean secret) {
    this.secret = secret;
  }

  public @NotNull FeatureValueType getValueType() {
    return valueType;
  }

  public void setValueType(@NotNull FeatureValueType valueType) {
    this.valueType = valueType;
  }

  public String getLink() {
    return link;
  }

  public LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getMetaData() {
    return metaData;
  }

  public void setMetaData(String metaData) {
    this.metaData = metaData;
  }

  public static final class Builder {
    public String metaData;
    public String description;
    private DbApplication parentApplication;
    private String key;
    private String alias;
    private String name;
    private boolean secret;
    private String link;
    private FeatureValueType valueType;

    public Builder() {}

    public Builder parentApplication(DbApplication val) {
      parentApplication = val;
      return this;
    }

    public Builder description(String val) {
      description = val;
      return this;
    }

    public Builder metaData(String val) {
      metaData = val;
      return this;
    }

    public Builder key(String val) {
      key = val;
      return this;
    }

    public Builder alias(String val) {
      alias = val;
      return this;
    }

    public Builder name(String val) {
      name = val;
      return this;
    }

    public Builder secret(boolean val) {
      secret = val;
      return this;
    }

    public Builder link(String val) {
      link = val;
      return this;
    }

    public Builder valueType(FeatureValueType val) {
      valueType = val;
      return this;
    }

    public DbApplicationFeature build() {
      return new DbApplicationFeature(this);
    }
  }
}
