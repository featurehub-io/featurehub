package io.featurehub.db.services

import io.ebean.Database
import io.ebean.annotation.Transactional
import io.featurehub.db.api.*
import io.featurehub.db.model.DbLogin
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.query.QDbLogin
import io.featurehub.db.model.query.QDbPerson
import io.featurehub.db.password.PasswordSalter
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import io.featurehub.mr.model.PersonType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

/**
 * Salting should be at the layer above this one as otherwise it would need to be re-implemented for
 * every database layer.
 */
@Singleton
class AuthenticationSqlApi @Inject constructor(private val convertUtils: Conversions) :
  AuthenticationApi, SessionApi {
  private val passwordSalter = PasswordSalter()
  override fun login(email: String, password: String): Person? {
    return QDbPerson().email
      .eq(email.lowercase(Locale.getDefault())).personType.eq(PersonType.PERSON).whenArchived.isNull()
      .findOne()?.let { p: DbPerson ->
        if (passwordSalter.validatePassword(password, p.password, p.passwordAlgorithm)) {
          updateLastAuthenticated(p, Instant.now())

          // update the password algorithm for their password if it is "old" now we know their password
          if (p.passwordAlgorithm != DbPerson.DEFAULT_PASSWORD_ALGORITHM) {
            log.info(
              "password: password for {} using old algorithm {}, replacing with {}", email,
              p.passwordAlgorithm, DbPerson.DEFAULT_PASSWORD_ALGORITHM
            )
            p.password = passwordSalter.saltAnyPassword(password, DbPerson.DEFAULT_PASSWORD_ALGORITHM)
            p.passwordAlgorithm = DbPerson.DEFAULT_PASSWORD_ALGORITHM
            updateUser(p)
          }
          convertUtils
            .toPerson(p, Opts.opts(FillOpts.Groups, FillOpts.Acls))!!
            .passwordRequiresReset(p.isPasswordRequiresReset)
        } else {
          null
        }
      }
  }

  @Transactional
  fun updateLastAuthenticated(p: DbPerson, whenLastAuthenticated: Instant?) {
    p.whenLastAuthenticated = whenLastAuthenticated
    p.save()
  }

  override fun register(name: String?, email: String, password: String?, opts: Opts?): Person? {
    return QDbPerson().email
      .eq(email.lowercase(Locale.getDefault()))
      .findOne()?.let { person: DbPerson ->
        if (person.token == null) {
          return@let null
        }

        // its ok password is null
        val saltedPassword = passwordSalter.saltAnyPassword(password, DbPerson.DEFAULT_PASSWORD_ALGORITHM)
        if (saltedPassword == null && password != null) {
          return@let null
        }
        person.name = name
        person.password = saltedPassword
        person.passwordAlgorithm = DbPerson.DEFAULT_PASSWORD_ALGORITHM
        person.token = null
        person.tokenExpiry = null
        person.whenLastAuthenticated = Instant.now()
        updateUser(person)

        convertUtils.toPerson(person, opts ?: Opts.opts(FillOpts.Groups, FillOpts.Acls))
      }
  }

  override fun resetPassword(id: UUID, password: String, changedBy: UUID, reactivate: Boolean): Person? {
    val whoChanged = convertUtils.byPerson(changedBy) ?: return null
    val person = convertUtils.byPerson(id) ?: return null
    if (person.id != whoChanged.id && person.personType == PersonType.PERSON) {
      return passwordSalter
        .saltPassword(password, DbPerson.DEFAULT_PASSWORD_ALGORITHM)
        .map { saltedPassword: String? ->
          person.password = saltedPassword
          person.isPasswordRequiresReset = true
          person.passwordAlgorithm = DbPerson.DEFAULT_PASSWORD_ALGORITHM
          person.whoChanged = whoChanged
          person.whenLastAuthenticated = Instant.now()
          if (reactivate) {
            person.whenArchived = null
          }
          updateUser(person)
          log.debug("reset user {} password", id)
          convertUtils.toPerson(person)
        }
        .orElse(null)
    }
    return null
  }

  @Transactional
  private fun updateUser(person: DbPerson) {
    person.save()
  }

  override fun replaceTemporaryPassword(pId: UUID, password: String): Person? {
    val person = convertUtils.byPerson(pId) ?: return null
    return if (person.isPasswordRequiresReset && person.personType == PersonType.PERSON) {
      passwordSalter
        .saltPassword(password, DbPerson.DEFAULT_PASSWORD_ALGORITHM)
        .map { saltedPassword: String? ->
          person.password = saltedPassword
          person.passwordAlgorithm = DbPerson.DEFAULT_PASSWORD_ALGORITHM
          person.isPasswordRequiresReset = false
          person.whoChanged = null
          updateUser(person)
          convertUtils.toPerson(person, Opts.empty())
        }
        .orElse(null)
    } else null
  }

  override fun changePassword(id: UUID, oldPassword: String, newPassword: String): Person? {
    val person = convertUtils.byPerson(id) ?: return null
    return if (person.personType == PersonType.PERSON && person.password != null && passwordSalter.validatePassword(
        oldPassword, person.password, person.passwordAlgorithm
      )
    ) {
      passwordSalter
        .saltPassword(newPassword, DbPerson.DEFAULT_PASSWORD_ALGORITHM)
        .map { saltedPassword: String? ->
          person.password = saltedPassword
          person.passwordAlgorithm = DbPerson.DEFAULT_PASSWORD_ALGORITHM
          person.isPasswordRequiresReset = false
          person.whoChanged = null
          updateUser(person)
          convertUtils.toPerson(person, Opts.empty())
        }
        .orElse(null)
    } else null
  }

  override fun getPersonByToken(token: String): Person? {
    return convertUtils.toPerson(QDbPerson().token.eq(token).findOne(), Opts.empty())
  }

  override fun findSession(token: String): DBLoginSession? {
    // severely limit the data for person
    return QDbLogin().token
      .eq(token).person
      .fetch(
        QDbPerson.Alias.id,
        QDbPerson.Alias.passwordRequiresReset,
        QDbPerson.Alias.email,
        QDbPerson.Alias.personType
      )
      .findOne()?.let { login ->
      val lastSeen = login.lastSeen
      login.lastSeen = Instant.now()
      login.save()
      return DBLoginSession(
        Person()
          .passwordRequiresReset(login.person.isPasswordRequiresReset)
          .email(login.person.email)
          .personType(login.person.personType)
          .id(PersonId().id(login.person.id)),
        token,
        lastSeen
      )
    }
  }

  override fun createSession(session: DBLoginSession): DBLoginSession? {
    Conversions.nonNullPerson(session.person)

    // we cannot create sessions for non-persons
    if (session.person.personType != PersonType.PERSON) {
      return null
    }
    val login = DbLogin.Builder()
      .lastSeen(session.lastSeen)
      .token(session.token)
      .person(convertUtils.byPerson(session.person))
      .build()
    if (login.person != null) {
      login.save()
      return session
    }
    return null
  }

  override fun invalidateSession(sessionToken: String) {
    QDbLogin().token.eq(sessionToken).delete()
  }

  override fun resetExpiredRegistrationToken(email: String): String? {
    val person = QDbPerson().email.eq(email.lowercase(Locale.getDefault())).personType.eq(PersonType.PERSON).findOne()
    if (person != null && person.token != null) {
      person.token = UUID.randomUUID().toString()
      person.tokenExpiry = LocalDateTime.now().plusDays(7)
      person.save()
      return person.token
    }
    return null
  }

  override fun updateLastAuthenticated(id: UUID) {
    val person = QDbPerson().id.eq(id).findOne()
    if (person != null) {
      updateLastAuthenticated(person, Instant.now())
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(AuthenticationSqlApi::class.java)
  }
}
