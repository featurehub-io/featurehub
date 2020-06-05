package io.featurehub.db.model;

import io.ebean.annotation.Index;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Index(unique = true, name = "idx_group_names", columnNames = {"fk_portfolio_id", "group_name"})
@Entity
@Table(name = "fh_group")
public class DbGroup {

  @Id
  private UUID id;

  @WhenModified
  @Column(name = "when_updated")
  private LocalDateTime whenUpdated;
  @WhenCreated
  @Column(name = "when_created")
  private LocalDateTime whenCreated;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  @Version
  private long version;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_person_who_created")
  @Column(name = "fk_person_who_created", nullable = false)
  private DbPerson whoCreated;

  public DbPerson getWhoCreated() {
    return whoCreated;
  }

  public void setWhoCreated(DbPerson whoCreated) {
    this.whoCreated = whoCreated;
  }

  @ManyToOne(optional = true) // could be in superadmin, which is no portfolio
  @JoinColumn(name = "fk_portfolio_id")
  @Column(name = "fk_portfolio_id")
  private DbPortfolio owningPortfolio;

  // is this an admin group
  @Column(name = "is_admin_group")
  private boolean adminGroup;

  @ManyToOne(optional = true)
  @JoinColumn(name = "fk_organization_id")
  @Column(name = "fk_organization_id")
  private DbOrganization owningOrganization;

  @Column(name = "group_name")
  private String name;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "group_id")
  private Set<DbAcl> groupRolesAcl;

  @ManyToMany(cascade = CascadeType.ALL, mappedBy = "groupsPersonIn")
  private Set<DbPerson> peopleInGroup;


  public DbGroup() {}

  private DbGroup(Builder builder) {
    setWhoCreated(builder.whoCreated);
    setOwningPortfolio(builder.owningPortfolio);
    setAdminGroup(builder.adminGroup);
    setOwningOrganization(builder.owningOrganization);
    setName(builder.name);
    setGroupRolesAcl(builder.groupRolesAcl);
    setPeopleInGroup(builder.peopleInGroup);
  }


  public DbPortfolio getOwningPortfolio() {
    return owningPortfolio;
  }

  public void setOwningPortfolio(DbPortfolio owningPortfolio) {
    this.owningPortfolio = owningPortfolio;
  }

  public DbOrganization getOwningOrganization() {
    return owningOrganization;
  }

  public void setOwningOrganization(DbOrganization owningOrganization) {
    this.owningOrganization = owningOrganization;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<DbAcl> getGroupRolesAcl() {
    return groupRolesAcl;
  }

  public void setGroupRolesAcl(Set<DbAcl> groupRolesAcl) {
    this.groupRolesAcl = groupRolesAcl;
  }

  public Set<DbPerson> getPeopleInGroup() {
    return peopleInGroup;
  }

  public void setPeopleInGroup(Set<DbPerson> peopleInGroup) {
    this.peopleInGroup = peopleInGroup;
  }

  public boolean isAdminGroup() {
    return adminGroup;
  }

  public void setAdminGroup(boolean adminGroup) {
    this.adminGroup = adminGroup;
  }

  public UUID getId() { return id; }

  public LocalDateTime getWhenUpdated() {
    return whenUpdated;
  }

  public LocalDateTime getWhenCreated() {
    return whenCreated;
  }

  public long getVersion() {
    return version;
  }

  public LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public static final class Builder {
    private DbPerson whoCreated;
    private DbPortfolio owningPortfolio;
    private boolean adminGroup;
    private DbOrganization owningOrganization;
    private String name;
    private Set<DbAcl> groupRolesAcl;
    private Set<DbPerson> peopleInGroup;

    public Builder() {
    }

    public Builder whoCreated(DbPerson val) {
      whoCreated = val;
      return this;
    }

    public Builder owningPortfolio(DbPortfolio val) {
      owningPortfolio = val;
      return this;
    }

    public Builder adminGroup(boolean val) {
      adminGroup = val;
      return this;
    }

    public Builder owningOrganization(DbOrganization val) {
      owningOrganization = val;
      return this;
    }

    public Builder name(String val) {
      name = val;
      return this;
    }

    public Builder groupRolesAcl(Set<DbAcl> val) {
      groupRolesAcl = val;
      return this;
    }

    public Builder peopleInGroup(Set<DbPerson> val) {
      peopleInGroup = val;
      return this;
    }

    public DbGroup build() {
      return new DbGroup(this);
    }
  }

  @Override
  public String toString() {
    return "DbGroup{" +
      "id=" + id +
      ", whenUpdated=" + whenUpdated +
      ", whenCreated=" + whenCreated +
      ", whenArchived=" + whenArchived +
      ", version=" + version +
      ", whoCreated=" + whoCreated +
      ", owningPortfolio=" + owningPortfolio +
      ", adminGroup=" + adminGroup +
      ", owningOrganization=" + owningOrganization +
      ", name='" + name + '\'' +
      ", groupRolesAcl=" + groupRolesAcl +
      ", peopleInGroup=" + peopleInGroup +
      '}';
  }
}
