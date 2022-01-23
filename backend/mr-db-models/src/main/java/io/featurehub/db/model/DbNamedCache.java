package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "fh_cache")
@ChangeLog
public class DbNamedCache {
  // if this cache is specified, all organizations that are not in a named cache are used.
  public static final String DEFAULT_CACHE_NAME = "default";

  @Id
  private String cacheName;

  @OneToMany(orphanRemoval = false)
  @JoinColumn(name = "fk_named_cache", referencedColumnName = "cache_name")
  private Set<DbOrganization> organizations;

  public DbNamedCache() {
  }

  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  public Set<DbOrganization> getOrganizations() {
    return organizations;
  }

  public void setOrganizations(Set<DbOrganization> organizations) {
    this.organizations = organizations;
  }

  private DbNamedCache(Builder builder) {
    cacheName = builder.cacheName;
    organizations = builder.organizations;
  }

  public static final class Builder {
    private String cacheName;
    private Set<DbOrganization> organizations;

    public Builder() {
    }

    public Builder cacheName(String val) {
      cacheName = val;
      return this;
    }

    public Builder organizations(Set<DbOrganization> val) {
      organizations = val;
      return this;
    }

    public DbNamedCache build() {
      return new DbNamedCache(this);
    }
  }
}
