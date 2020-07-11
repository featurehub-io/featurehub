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
import io.featurehub.db.model.query.QDbOrganization;
import io.featurehub.db.model.query.QDbPerson;
import io.featurehub.db.password.PasswordSalter;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;

/**
 *
 * Salting should be at the layer above this one as otherwise it would need to be re-implemented for
 * every database layer.
 *
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


    return new QDbPerson().email.eq(email.toLowerCase()).findOneOrEmpty()
      .map(p -> {
        if (passwordSalter.validatePassword(password, p.getPassword())) {
          updateLastAuthenticated(p);

          return convertUtils.toPerson(p, new QDbOrganization().findOne(), Opts.opts(FillOpts.Groups))
            .passwordRequiresReset(p.isPasswordRequiresReset());
        } else {
          return null;
        }
      }).orElse(null);

  }

  @Transactional
  private void updateLastAuthenticated(DbPerson p) {
    database.update(DbPerson.class).set("whenLastAuthenticated", LocalDateTime.now()).where().idEq(p.getId()).update();
  }

  @Override
  public Person register(String name, String email, String password) {
    if (name == null || email == null || password == null) return null;

    return new QDbPerson().email.eq(email.toLowerCase()).findOneOrEmpty()
      .map(person -> {
        if (person.getToken() == null) {
          return null;
        }

        return passwordSalter.saltPassword(password)
          .map(saltedPassword -> {
            log.info("salted password {} password {}", saltedPassword, password);
            person.setName(name);
            person.setPassword(saltedPassword);
            person.setToken(null);
            person.setTokenExpiry(null);
            updateUser(person);

            return convertUtils.toPerson(person, new QDbOrganization().findOne(), Opts.opts(FillOpts.Groups,
              FillOpts.Acls));
          }).orElse((Person) null);
        }
      ).orElse(null);
  }

  @Override
  public Person resetPassword(String id, String password, String changedBy, boolean reactivate) {
    if (id == null || password == null || changedBy == null) return null;

    DbPerson whoChanged = convertUtils.uuidPerson(changedBy);
    if (whoChanged != null) {
      DbPerson person = convertUtils.uuidPerson(id);

      if (person != null) {
        return passwordSalter.saltPassword(password).map(saltedPassword -> {
          person.setPassword(saltedPassword);
          person.setPasswordRequiresReset(true);
          person.setWhoChanged(whoChanged);

          if (reactivate) {
            person.setWhenArchived(null);
          }

          updateUser(person);
          log.debug("reset user {} password", id);

          return convertUtils.toPerson(person);
        }).orElse(null);
      }
    }

    return null;
  }

  @Transactional
  private void updateUser(DbPerson person) {
    database.save(person);
  }

  @Override
  public Person replaceTemporaryPassword(String id, String password) {
    if (id == null || password == null) return null;

    return Conversions.uuid(id).map(
      pId -> new QDbPerson().id.eq(pId)
      .findOneOrEmpty().map(p -> {
        if (p.isPasswordRequiresReset()) {
          return passwordSalter.saltPassword(password).map(saltedPassword -> {
            p.setPassword(saltedPassword);
            p.setPasswordRequiresReset(false);
            p.setWhoChanged(null);

            updateUser(p);

            return convertUtils.toPerson(p, new QDbOrganization().findOne(), Opts.empty());
          }).orElse(null);
        }

        return null;
      }).orElse(null)).orElse(null);

  }

  @Override
  public Person changePassword(String id, String oldPassword, String newPassword) {
    if (id == null || oldPassword == null || newPassword == null) return null;

    return Conversions.uuid(id)
      .map(pId -> new QDbPerson().id.eq(pId)
      .findOneOrEmpty().map(p -> {
        if (p.getPassword() != null && passwordSalter.validatePassword(oldPassword, p.getPassword())) {
          return passwordSalter.saltPassword(newPassword).map(saltedPassword -> {
            p.setPassword(saltedPassword);
            p.setPasswordRequiresReset(false);
            p.setWhoChanged(null);
            updateUser(p);
            return convertUtils.toPerson(p, new QDbOrganization().findOne(), Opts.empty());
          }).orElse(null);
        }

        return null;
      }).orElse(null)).orElse(null);

  }

  @Override
  public Person getPersonByToken(String token) {
    return convertUtils.toPerson(new QDbPerson().token.eq(token).findOne(),new QDbOrganization().findOne(),
      Opts.empty());
  }

  @Override
  public DBLoginSession findSession(String token) {
    // severely limit the data for person
    final DbLogin login = new QDbLogin().token.eq(token).person.fetch(QDbPerson.Alias.id, QDbPerson.Alias.passwordRequiresReset).findOne();

    if (login != null) {
      LocalDateTime lastSeen = login.getLastSeen();
      login.setLastSeen(LocalDateTime.now());
      database.save(login);

      return new DBLoginSession(
        new Person()
          .passwordRequiresReset(login.getPerson().isPasswordRequiresReset())
          .id(new PersonId().id(login.getPerson().getId().toString())),
        token, lastSeen);
    }
    return null;
  }

  @Override
  public DBLoginSession createSession(DBLoginSession session) {
    final DbLogin login = new DbLogin.Builder()
      .lastSeen(session.getLastSeen())
      .token(session.getToken())
      .person(convertUtils.uuidPerson(session.getPerson().getId().getId())).build();

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
}
