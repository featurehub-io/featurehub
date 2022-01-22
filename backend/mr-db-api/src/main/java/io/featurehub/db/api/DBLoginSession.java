package io.featurehub.db.api;

import io.featurehub.mr.model.Person;

import java.time.Instant;
import java.time.LocalDateTime;

public class DBLoginSession {
  private final Person person;
  private final String token;
  private final Instant lastSeen;

  public DBLoginSession(Person person, String token, Instant lastSeen) {
    this.person = person;
    this.token = token;
    this.lastSeen = lastSeen;
  }

  public Person getPerson() {
    return person;
  }

  public String getToken() {
    return token;
  }

  public Instant getLastSeen() {
    return lastSeen;
  }
}
