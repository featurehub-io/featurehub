package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbDefault;
import io.ebean.annotation.DbEnumValue;
import io.ebean.annotation.DbForeignKey;
import io.ebean.annotation.Index;
import io.featurehub.mr.model.PersonType;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Index(unique = true, name = "idx_person_email", columnNames = {"email"})
@Entity
@Table(name = "fh_person")
@ChangeLog
public class DbPerson extends DbVersionedBase {
  public static final String DEFAULT_PASSWORD_ALGORITHM = "PBKDF2WithHmacSHA512";

  @Column(name = "password_alg", length = 60, nullable = false)
  @DbDefault("PBKDF2WithHmacSHA1")
  private String passwordAlgorithm = DEFAULT_PASSWORD_ALGORITHM;

  public DbPerson() {}

  private Instant whenLastAuthenticated;

  @Column(name = "name", length = 100)
  private String name;
  @Column(name = "email", nullable = false, length = 100)
  private String email;
  @Column(name = "password", length = 255)
  private String password;
  private boolean passwordRequiresReset;
  private String token;
  private LocalDateTime tokenExpiry;

  @Column(name = "person_type", length = 100)
  @DbDefault("person")
  @Enumerated(value = EnumType.STRING)
  private PersonType personType;

  @Column(name = "fk_person_who_changed")
  @ManyToOne
  private DbPerson whoChanged;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  @ManyToOne(optional = true)
  @JoinColumn(name = "fk_person_who_created")
  @Column(name = "fk_person_who_created")
  private DbPerson whoCreated;

  @DbForeignKey(onDelete = ConstraintMode.CASCADE)
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DbGroupMember> groupMembers;

  public String getPasswordAlgorithm() {
    return passwordAlgorithm;
  }

  public void setPasswordAlgorithm(String passwordAlgorithm) {
    this.passwordAlgorithm = passwordAlgorithm;
  }

  public Instant getWhenLastAuthenticated() {
    return whenLastAuthenticated;
  }

  public void setWhenLastAuthenticated(Instant whenLastAuthenticated) {
    this.whenLastAuthenticated = whenLastAuthenticated;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isPasswordRequiresReset() {
    return passwordRequiresReset;
  }

  public void setPasswordRequiresReset(boolean passwordRequiresReset) {
    this.passwordRequiresReset = passwordRequiresReset;
  }

  public DbPerson getWhoChanged() {
    return whoChanged;
  }

  public void setWhoChanged(DbPerson whoChanged) {
    this.whoChanged = whoChanged;
  }

  public DbPerson getWhoCreated() {
    return whoCreated;
  }

  public void setWhoCreated(DbPerson whoCreated) {
    this.whoCreated = whoCreated;
  }

  public List<DbGroupMember> getGroupMembers() {
    return groupMembers;
  }

  public void setGroupMembers(List<DbGroupMember> groupMembers) {
    this.groupMembers = groupMembers;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public LocalDateTime getTokenExpiry() {
    return tokenExpiry;
  }

  public void setTokenExpiry(LocalDateTime tokenExpiry) {
    this.tokenExpiry = tokenExpiry;
  }

  public LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public PersonType getPersonType() {
    return personType;
  }

  public void setPersonType(PersonType personType) {
    this.personType = personType;
  }

  public static final class Builder {
    private String token;
    private LocalDateTime tokenExpiry;
    private String name;
    private String email;
    private DbPerson whoCreated;
    private PersonType personType = PersonType.PERSON;

    public Builder() {
    }

    public Builder personType(PersonType val) {
      personType = val;
      return this;
    }

    public Builder name(String val) {
      name = val;
      return this;
    }

    public Builder email(String val) {
      email = val;
      return this;
    }

    public Builder token(String val) {
      token = val;
      return this;
    }

    public Builder tokenExpiry(LocalDateTime val) {
      tokenExpiry = val;
      return this;
    }

    public Builder whoCreated(DbPerson val) {
      whoCreated = val;
      return this;
    }

    public DbPerson build() {
      DbPerson dbPerson = new DbPerson();
      dbPerson.setToken(token);
      dbPerson.setTokenExpiry(tokenExpiry);
      dbPerson.setName(name);
      dbPerson.setEmail(email);
      dbPerson.setWhoCreated(whoCreated);
      dbPerson.setPersonType(personType);
      return dbPerson;
    }
  }
}
