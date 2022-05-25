package io.featurehub.db.model;

import io.ebean.Model;
import io.ebean.annotation.ChangeLog;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "fh_login")
@ChangeLog
public class DbLogin extends Model {
  @Id
  private String token;

  @Column(name = "fk_person")
  @ManyToOne
  private DbPerson person;

  private Instant lastSeen;

  private DbLogin(Builder builder) {
    setToken(builder.token);
    setPerson(builder.person);
    setLastSeen(builder.lastSeen);
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public DbPerson getPerson() {
    return person;
  }

  public void setPerson(DbPerson person) {
    this.person = person;
  }

  public Instant getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(Instant lastSeen) {
    this.lastSeen = lastSeen;
  }


  public static final class Builder {
    private String token;
    private DbPerson person;
    private Instant lastSeen;

    public Builder() {
    }

    public Builder token(String val) {
      token = val;
      return this;
    }

    public Builder person(DbPerson val) {
      person = val;
      return this;
    }

    public Builder lastSeen(Instant val) {
      lastSeen = val;
      return this;
    }

    public DbLogin build() {
      return new DbLogin(this);
    }
  }
}
