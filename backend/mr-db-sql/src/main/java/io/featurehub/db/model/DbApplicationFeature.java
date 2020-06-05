package io.featurehub.db.model;

import io.ebean.annotation.Index;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.featurehub.mr.model.FeatureValueType;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Index(unique = true, name = "idx_app_features", columnNames = {"fk_app_id", "feature_key"})
@Entity
@Table(name = "fh_app_feature")
public class DbApplicationFeature {
  @Id
  private UUID id;

  @Version
  private long version;

  private DbApplicationFeature(Builder builder) {
    setParentApplication(builder.parentApplication);
    setKey(builder.key);
    setAlias(builder.alias);
    setName(builder.name);
    setSecret(builder.secret);
    setLink(builder.link);
    setValueType(builder.valueType);
  }

  public UUID getId() { return id; }

  @WhenModified
  @Column(name = "when_updated")
  public LocalDateTime whenUpdated;
  @WhenCreated
  @Column(name = "when_created")
  public LocalDateTime whenCreated;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_app_id")
  @Column(name = "fk_app_id")
  private DbApplication parentApplication;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  @Column(name = "feature_key")
  private String key;
  private String alias;
  private String name;
  private boolean secret;
  @Column(length = 600)
  private String link;

  @Enumerated(value = EnumType.STRING)
  private FeatureValueType valueType;


  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_feature_id")
  private Set<DbEnvironmentFeatureStrategy> environmentFeatures;

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public DbApplication getParentApplication() {
    return parentApplication;
  }

  public void setParentApplication(DbApplication parentApplication) {
    this.parentApplication = parentApplication;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<DbEnvironmentFeatureStrategy> getEnvironmentFeatures() {
    return environmentFeatures;
  }

  public void setEnvironmentFeatures(Set<DbEnvironmentFeatureStrategy> environmentFeatures) {
    this.environmentFeatures = environmentFeatures;
  }

  public boolean isSecret() {
    return secret;
  }

  public void setSecret(boolean secret) {
    this.secret = secret;
  }

  public FeatureValueType getValueType() {
    return valueType;
  }

  public void setValueType(FeatureValueType valueType) {
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

  public static final class Builder {
    private DbApplication parentApplication;
    private String key;
    private String alias;
    private String name;
    private boolean secret;
    private String link;
    private FeatureValueType valueType;

    public Builder() {
    }

    public Builder parentApplication(DbApplication val) {
      parentApplication = val;
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
