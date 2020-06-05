package io.featurehub.mr.auth;

import io.featurehub.mr.model.Person;

import java.time.LocalDateTime;

public class SessionToken {
  public Person person;
  public LocalDateTime lastSeen;
  public String sessionToken;

  private SessionToken(Builder builder) {
    person = builder.person;
    lastSeen = builder.lastSeen;
    sessionToken = builder.sessionToken;
  }

  public static final class Builder {
    private Person person;
    private LocalDateTime lastSeen;
    private String sessionToken;

    public Builder() {
    }

    public Builder person(Person val) {
      person = val;
      return this;
    }

    public Builder lastSeen(LocalDateTime val) {
      lastSeen = val;
      return this;
    }

    public Builder sessionToken(String val) {
      sessionToken = val;
      return this;
    }

    public SessionToken build() {
      return new SessionToken(this);
    }
  }
}
