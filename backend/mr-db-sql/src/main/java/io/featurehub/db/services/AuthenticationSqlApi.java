package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.AuthenticationApi;
import io.featurehub.db.api.DBLoginSession;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbLogin;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.query.QDbLogin;
import io.featurehub.db.model.query.QDbPerson;
import io.featurehub.db.password.PasswordSalter;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Salting should be at the layer above this one as otherwise it would need to be re-implemented for
 * every database layer.
 */
@Singleton
public class AuthenticationSqlApi implements AuthenticationApi {
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
  public Person login(String email, String password) {
    if (email == null || password == null) return null;

    return new QDbPerson()
        .email
        .eq(email.toLowerCase())
        .findOneOrEmpty()
        .map(
            p -> {
              if (passwordSalter.validatePassword(password, p.getPassword())) {
                updateLastAuthenticated(p);

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
  private void updateLastAuthenticated(DbPerson p) {
    database
        .update(DbPerson.class)
        .set("whenLastAuthenticated", LocalDateTime.now())
        .where()
        .idEq(p.getId())
        .update();
  }

  @Override
  public Person register(String name, String email, String password) {
    if (name == null || email == null) return null;

    return new QDbPerson()
        .email
        .eq(email.toLowerCase())
        .findOneOrEmpty()
        .map(
            person -> {
              if (person.getToken() == null) {
                return null;
              }

              String saltedPassword = passwordSalter.saltAnyPassword(password);

              if (saltedPassword == null && password != null) {
                return null;
              }

              person.setName(name);
              person.setPassword(saltedPassword);
              person.setToken(null);
              person.setTokenExpiry(null);
              updateUser(person);

              return convertUtils.toPerson(person, Opts.opts(FillOpts.Groups, FillOpts.Acls));
            })
        .orElse(null);
  }

  @Override
  public Person resetPassword(UUID id, String password, UUID changedBy, boolean reactivate) {
    Conversions.nonNullPersonId(id);
    Conversions.nonNullPersonId(changedBy);

    if (password == null) return null;

    DbPerson whoChanged = convertUtils.byPerson(changedBy);
    if (whoChanged != null) {
      DbPerson person = convertUtils.byPerson(id);

      if (person != null && !person.getId().equals(whoChanged.getId())) {
        return passwordSalter
            .saltPassword(password)
            .map(
                saltedPassword -> {
                  person.setPassword(saltedPassword);
                  person.setPasswordRequiresReset(true);
                  person.setWhoChanged(whoChanged);

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
  public Person replaceTemporaryPassword(UUID pId, String password) {
    Conversions.nonNullPersonId(pId);

    if (password == null) return null;

    DbPerson person = convertUtils.byPerson(pId);

    if (person != null && person.isPasswordRequiresReset()) {
      return passwordSalter
          .saltPassword(password)
          .map(
              saltedPassword -> {
                person.setPassword(saltedPassword);
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
  public Person changePassword(UUID id, String oldPassword, String newPassword) {
    Conversions.nonNullPersonId(id);

    if (oldPassword == null || newPassword == null) return null;

    DbPerson person = convertUtils.byPerson(id);

    if (person != null
        && person.getPassword() != null
        && passwordSalter.validatePassword(oldPassword, person.getPassword())) {
      return passwordSalter
          .saltPassword(newPassword)
          .map(
              saltedPassword -> {
                person.setPassword(saltedPassword);
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
  public Person getPersonByToken(String token) {
    return convertUtils.toPerson(new QDbPerson().token.eq(token).findOne(), Opts.empty());
  }

  @Override
  public DBLoginSession findSession(String token) {
    // severely limit the data for person
    final DbLogin login =
        new QDbLogin()
            .token
            .eq(token)
            .person
            .fetch(QDbPerson.Alias.id, QDbPerson.Alias.passwordRequiresReset)
            .findOne();

    if (login != null) {
      LocalDateTime lastSeen = login.getLastSeen();
      login.setLastSeen(LocalDateTime.now());
      database.save(login);

      return new DBLoginSession(
          new Person()
              .passwordRequiresReset(login.getPerson().isPasswordRequiresReset())
              .id(new PersonId().id(login.getPerson().getId())),
          token,
          lastSeen);
    }
    return null;
  }

  @Override
  public DBLoginSession createSession(DBLoginSession session) {
    Conversions.nonNullPerson(session.getPerson());

    if (session.getToken() == null) {
      throw new IllegalArgumentException("Session token cannot be null");
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
  public void invalidateSession(String token) {
    final DbLogin login = new QDbLogin().token.eq(token).findOne();

    if (login != null) {
      database.delete(login);
    }
  }

  @Override
  public String resetExpiredRegistrationToken(String email) {
    DbPerson person = new QDbPerson().email.eq(email.toLowerCase()).findOne();

    if (person != null && person.getToken() != null) {
      person.setToken(UUID.randomUUID().toString());
      person.setTokenExpiry(LocalDateTime.now().plusDays(7));
      database.save(person);
      return person.getToken();
    }

    return null;
  }
}
