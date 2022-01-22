package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class DBLoginSession {
  private final Person person;
  private final String token;
  private final Instant lastSeen;

  public DBLoginSession(@NotNull Person person, @NotNull String token, @NotNull Instant lastSeen) {
    this.person = person;
    this.token = token;
    this.lastSeen = lastSeen;
  }

  @NotNull
  public Person getPerson() {
    return person;
  }

  @NotNull
  public String getToken() {
    return token;
  }

  @NotNull
  public Instant getLastSeen() {
    return lastSeen;
  }
}
