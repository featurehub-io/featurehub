package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.Index;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fh_userstate")
@Index(unique = true, name = "idx_user_state", columnNames = {"fk_person", "fk_portfolio_id", "fk_app_id", "fk_env_id"})
@ChangeLog
public class DbUserState extends DbVersionedBase {
  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_person")
  @Column(name = "fk_person")
  private DbPerson person;

  @ManyToOne
  @JoinColumn(name = "fk_portfolio_id")
  @Column(name = "fk_portfolio_id")
  private DbPortfolio portfolio;

  @ManyToOne
  @JoinColumn(name = "fk_app_id")
  @Column(name = "fk_app_id")
  private DbApplication application;

  @ManyToOne
  @JoinColumn(name = "fk_env_id")
  @Column(name = "fk_env_id")
  private DbEnvironment environment;

  @Enumerated(value = EnumType.STRING)
  private UserState userState;

  @Lob
  private String data;

  private DbUserState(Builder builder) {
    setPerson(builder.person);
    setPortfolio(builder.portfolio);
    setApplication(builder.application);
    setEnvironment(builder.environment);
    setUserState(builder.userState);
    setData(builder.data);
  }

  public DbPerson getPerson() {
    return person;
  }

  public void setPerson(DbPerson person) {
    this.person = person;
  }


  public DbPortfolio getPortfolio() {
    return portfolio;
  }

  public void setPortfolio(DbPortfolio portfolio) {
    this.portfolio = portfolio;
  }

  public DbApplication getApplication() {
    return application;
  }

  public void setApplication(DbApplication application) {
    this.application = application;
  }

  public DbEnvironment getEnvironment() {
    return environment;
  }

  public void setEnvironment(DbEnvironment environment) {
    this.environment = environment;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public UserState getUserState() {
    return userState;
  }

  public void setUserState(UserState userState) {
    this.userState = userState;
  }


  public static final class Builder {
    private DbPerson person;
    private DbPortfolio portfolio;
    private DbApplication application;
    private DbEnvironment environment;
    private UserState userState;
    private String data;

    public Builder() {
    }

    public Builder person(DbPerson val) {
      person = val;
      return this;
    }

    public Builder portfolio(DbPortfolio val) {
      portfolio = val;
      return this;
    }

    public Builder application(DbApplication val) {
      application = val;
      return this;
    }

    public Builder environment(DbEnvironment val) {
      environment = val;
      return this;
    }

    public Builder userState(UserState val) {
      userState = val;
      return this;
    }

    public Builder data(String val) {
      data = val;
      return this;
    }

    public DbUserState build() {
      return new DbUserState(this);
    }
  }
}
