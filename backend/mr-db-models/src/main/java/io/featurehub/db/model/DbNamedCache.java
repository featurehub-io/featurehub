package io.featurehub.db.model;

import io.ebean.Model;
import io.ebean.annotation.ChangeLog;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;

@Entity
@Table(name = "fh_cache")
@ChangeLog
public class DbNamedCache extends Model {
  // if this cache is specified, all organizations that are not in a named cache are used.
  public static final String DEFAULT_CACHE_NAME = "default";

  @Id
  private String cacheName;

//  @OneToMany(orphanRemoval = false)
//  @JoinColumn(name = "fk_named_cache", referencedColumnName = "cache_name")
  private List<DbOrganization> organizations;

  public DbNamedCache() {
  }

  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  public List<DbOrganization> getOrganizations() {
    return organizations;
  }

  public void setOrganizations(List<DbOrganization> organizations) {
    this.organizations = organizations;
  }

  private DbNamedCache(Builder builder) {
    cacheName = builder.cacheName;
    organizations = builder.organizations;
  }

  public static final class Builder {
    private String cacheName;
    private List<DbOrganization> organizations;

    public Builder() {
    }

    public Builder cacheName(String val) {
      cacheName = val;
      return this;
    }

    public Builder organizations(List<DbOrganization> val) {
      organizations = val;
      return this;
    }

    public DbNamedCache build() {
      return new DbNamedCache(this);
    }
  }
}
