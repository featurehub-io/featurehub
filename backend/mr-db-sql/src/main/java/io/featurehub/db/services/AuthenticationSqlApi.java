package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.AuthenticationApi;
import io.featurehub.db.api.DBLoginSession;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.SessionApi;
import io.featurehub.db.model.DbLogin;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.query.QDbLogin;
import io.featurehub.db.model.query.QDbPerson;
import io.featurehub.db.password.PasswordSalter;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonId;
import io.featurehub.mr.model.PersonType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Salting should be at the layer above this one as otherwise it would need to be re-implemented for
 * every database layer.
 */
@Singleton
public class AuthenticationSqlApi implements AuthenticationApi, SessionApi {
  private static final Logger log = LoggerFactory.getLogger(AuthenticationSqlApi.class);
  private final Database database;
  private final Conversions convertUtils;
  private final PasswordSalter passwordSalter = new PasswordSalter();

  @Inject
  public AuthenticationSqlApi(Database database, Conversions convertUtils) {
    this.database = database;
    this.convertUtils = convertUtils;
  }

  @Override
  public Person login(@NotNull String email, @NotNull String password) {
    return new QDbPerson()
        .email
        .eq(email.toLowerCase())
        .personType.eq(PersonType.PERSON)
        .whenArchived.isNull()
        .findOneOrEmpty()
        .map(
            p -> {
              if (passwordSalter.validatePassword(password, p.getPassword(), p.getPasswordAlgorithm())) {
                updateLastAuthenticated(p, Instant.now());

                // update the password algorithm for their password if it is "old" now we know their password
                if (!p.getPasswordAlgorithm().equals(DbPerson.DEFAULT_PASSWORD_ALGORITHM)) {
                  log.info("password: password for {} using old algorithm {}, replacing with {}", email,
                    p.getPasswordAlgorithm(), DbPerson.DEFAULT_PASSWORD_ALGORITHM);
                  p.setPassword(passwordSalter.saltAnyPassword(password, DbPerson.DEFAULT_PASSWORD_ALGORITHM));
                  p.setPasswordAlgorithm(DbPerson.DEFAULT_PASSWORD_ALGORITHM);
                  updateUser(p);
                }

                return convertUtils
                    .toPerson(p, Opts.opts(FillOpts.Groups, FillOpts.Acls))
                    .passwordRequiresReset(p.isPasswordRequiresReset());
              } else {
                return null;
              }
            })
        .orElse(null);
  }

  @Transactional
  public void updateLastAuthenticated(DbPerson p, Instant whenLastAuthenticated) {
    p.setWhenLastAuthenticated(whenLastAuthenticated);
    p.save();
  }

  @Override
  public Person register(@Nullable String name, @NotNull String email, @Nullable String password, Opts opts) {
    return new QDbPerson()
        .email
        .eq(email.toLowerCase())
        .findOneOrEmpty()
        .map(
            person -> {
              if (person.getToken() == null) {
                return null;
              }

              // its ok password is null
              String saltedPassword = passwordSalter.saltAnyPassword(password, DbPerson.DEFAULT_PASSWORD_ALGORITHM);

              if (saltedPassword == null && password != null) {
                return null;
              }

              person.setName(name);
              person.setPassword(saltedPassword);
              person.setPasswordAlgorithm(DbPerson.DEFAULT_PASSWORD_ALGORITHM);
              person.setToken(null);
              person.setTokenExpiry(null);
              person.setWhenLastAuthenticated(Instant.now());
              updateUser(person);

              return convertUtils.toPerson(person, opts == null ?  Opts.opts(FillOpts.Groups, FillOpts.Acls) : opts );
            })
        .orElse(null);
  }

  @Override
  public Person resetPassword(@NotNull UUID id, @NotNull String password, @NotNull UUID changedBy, boolean reactivate) {
    Conversions.nonNullPersonId(id);
    Conversions.nonNullPersonId(changedBy);

    if (password == null) return null;

    DbPerson whoChanged = convertUtils.byPerson(changedBy);
    if (whoChanged != null) {
      DbPerson person = convertUtils.byPerson(id);

      if (person != null && !person.getId().equals(whoChanged.getId()) && person.getPersonType() == PersonType.PERSON) {
        return passwordSalter
            .saltPassword(password, DbPerson.DEFAULT_PASSWORD_ALGORITHM)
            .map(
                saltedPassword -> {
                  person.setPassword(saltedPassword);
                  person.setPasswordRequiresReset(true);
                  person.setPasswordAlgorithm(DbPerson.DEFAULT_PASSWORD_ALGORITHM);
                  person.setWhoChanged(whoChanged);
                  person.setWhenLastAuthenticated(Instant.now());

                  if (reactivate) {
                    person.setWhenArchived(null);
                  }

                  updateUser(person);
                  log.debug("reset user {} password", id);

                  return convertUtils.toPerson(person);
                })
            .orElse(null);
      }
    }

    return null;
  }

  @Transactional
  private void updateUser(DbPerson person) {
    database.save(person);
  }

  @Override
  public Person replaceTemporaryPassword(@NotNull UUID pId, @NotNull String password) {
    Conversions.nonNullPersonId(pId);

    DbPerson person = convertUtils.byPerson(pId);

    if (person != null && person.isPasswordRequiresReset() && person.getPersonType() == PersonType.PERSON) {
      return passwordSalter
          .saltPassword(password, DbPerson.DEFAULT_PASSWORD_ALGORITHM)
          .map(
              saltedPassword -> {
                person.setPassword(saltedPassword);
                person.setPasswordAlgorithm(DbPerson.DEFAULT_PASSWORD_ALGORITHM);
                person.setPasswordRequiresReset(false);
                person.setWhoChanged(null);

                updateUser(person);

                return convertUtils.toPerson(person, Opts.empty());
              })
          .orElse(null);
    }

    return null;
  }

  @Override
  public Person changePassword(@NotNull UUID id, @NotNull String oldPassword, @NotNull String newPassword) {
    Conversions.nonNullPersonId(id);

    DbPerson person = convertUtils.byPerson(id);

    if (person != null
        && person.getPersonType() == PersonType.PERSON
        && person.getPassword() != null
        && passwordSalter.validatePassword(
            oldPassword, person.getPassword(), person.getPasswordAlgorithm())) {
      return passwordSalter
          .saltPassword(newPassword, DbPerson.DEFAULT_PASSWORD_ALGORITHM)
          .map(
              saltedPassword -> {
                person.setPassword(saltedPassword);
                person.setPasswordAlgorithm(DbPerson.DEFAULT_PASSWORD_ALGORITHM);
                person.setPasswordRequiresReset(false);
                person.setWhoChanged(null);
                updateUser(person);
                return convertUtils.toPerson(person, Opts.empty());
              })
          .orElse(null);
    }

    return null;
  }

  @Override
  public Person getPersonByToken(@NotNull String token) {
    return convertUtils.toPerson(new QDbPerson().token.eq(token).findOne(), Opts.empty());
  }

  @Override
  public DBLoginSession findSession(@NotNull String token) {
    // severely limit the data for person
    final DbLogin login =
        new QDbLogin()
            .token
            .eq(token)
            .person
            .fetch(QDbPerson.Alias.id, QDbPerson.Alias.passwordRequiresReset, QDbPerson.Alias.email, QDbPerson.Alias.personType)
            .findOne();

    if (login != null) {
      Instant lastSeen = login.getLastSeen();
      login.setLastSeen(Instant.now());
      database.save(login);

      return new DBLoginSession(
          new Person()
              .passwordRequiresReset(login.getPerson().isPasswordRequiresReset())
              .email(login.getPerson().getEmail())
              .personType(login.getPerson().getPersonType())
              .id(new PersonId().id(login.getPerson().getId())),
          token,
          lastSeen);
    }
    return null;
  }

  @Override
  public DBLoginSession createSession(DBLoginSession session) {
    Conversions.nonNullPerson(session.getPerson());

    // we cannot create sessions for non-persons
    if (session.getPerson().getPersonType() != PersonType.PERSON) {
      return null;
    }

    final DbLogin login =
        new DbLogin.Builder()
            .lastSeen(session.getLastSeen())
            .token(session.getToken())
            .person(convertUtils.byPerson(session.getPerson()))
            .build();

    if (login.getPerson() != null) {
      database.save(login);
      return session;
    }

    return null;
  }

  @Override
  public void invalidateSession(@NotNull String token) {
    new QDbLogin().token.eq(token).person.personType.eq(PersonType.PERSON).delete();
  }

  @Override
  public String resetExpiredRegistrationToken(String email) {
    DbPerson person = new QDbPerson().email.eq(email.toLowerCase()).personType.eq(PersonType.PERSON).findOne();

    if (person != null && person.getToken() != null) {
      person.setToken(UUID.randomUUID().toString());
      person.setTokenExpiry(LocalDateTime.now().plusDays(7));
      database.save(person);
      return person.getToken();
    }

    return null;
  }

  @Override
  public void updateLastAuthenticated(@NotNull UUID id) {
    final DbPerson person = new QDbPerson().id.eq(id).findOne();

    if (person != null) {
      updateLastAuthenticated(person, Instant.now());
    }
  }
}
