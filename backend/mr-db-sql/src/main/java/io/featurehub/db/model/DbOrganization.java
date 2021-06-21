package io.featurehub.db.model;

import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbForeignKey;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "fh_organization")
public class DbOrganization {

  public DbOrganization() {}

  @Id
  private UUID id;

  @Version
  private long version;

  private DbOrganization(Builder builder) {
    id = builder.id;
    setName(builder.name);
    setNamedCache(builder.namedCache);
  }

  @WhenModified
  @Column(name = "when_updated")
  private LocalDateTime whenUpdated;
  @WhenCreated
  @Column(name = "when_created")
  private LocalDateTime whenCreated;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  private String name;

  @DbForeignKey(onDelete = ConstraintMode.SET_NULL)
  @ManyToOne
  @Column(name = "fk_named_cache")
  @JoinColumn(name = "fk_named_cache")
  private DbNamedCache namedCache;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_org_id")
  private Set<DbPortfolio> portfolios;

  @OneToOne
  private DbGroup group;

  public DbGroup getGroup() {
    return group;
  }

  public void setGroup(DbGroup group) {
    this.group = group;
  }

  public DbNamedCache getNamedCache() {
    return namedCache;
  }

  public void setNamedCache(DbNamedCache namedCache) {
    this.namedCache = namedCache;
  }

  public long getVersion() {
    return version;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<DbPortfolio> getPortfolios() {
    return portfolios;
  }

  public void setPortfolios(Set<DbPortfolio> portfolios) {
    this.portfolios = portfolios;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getId() { return id; }

  public LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public static final class Builder {
    private UUID id;
    private String name;
    private DbNamedCache namedCache;

    public Builder() {
    }

    public Builder id(UUID val) {
      id = val;
      return this;
    }

    public Builder name(String val) {
      name = val;
      return this;
    }

    public Builder namedCache(DbNamedCache val) {
      namedCache = val;
      return this;
    }

    public DbOrganization build() {
      return new DbOrganization(this);
    }
  }
}
