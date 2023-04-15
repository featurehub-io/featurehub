package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbForeignKey;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "fh_organization")
@ChangeLog
public class DbOrganization extends DbVersionedBase {

  public DbOrganization() {}

  private DbOrganization(Builder builder) {
    setId(builder.id);
    setName(builder.name);
    setNamedCache(builder.namedCache);
  }

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
