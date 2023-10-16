package io.featurehub.mr.resources

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.db.api.AuthenticationApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonApi
import io.featurehub.mr.api.AuthServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.auth.AuthenticationRepository
import io.featurehub.mr.model.*
import io.featurehub.web.security.oauth.AuthProviderCollection
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.String

class AuthResource @Inject constructor(
  private val authenticationApi: AuthenticationApi,
  private val authManager: AuthManagerService, private val personApi: PersonApi,
  private val authRepository: AuthenticationRepository, private val authProviderCollection: AuthProviderCollection
) : AuthServiceDelegate {
  private val log = LoggerFactory.getLogger(AuthResource::class.java)

  @ConfigKey("auth.disable-login")
  private var loginDisabled:Boolean? = false

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun changePassword(id: UUID, passwordUpdate: PasswordUpdate, securityContext: SecurityContext?): Person {
    val personByToken = authManager.from(securityContext)

    // yourself or a superuser can change your password. This allows a superuser to change the password immediately
    // after reset without having to go to any further trouble.
    if (personByToken.id!!.id == id || authManager.isOrgAdmin(personByToken)) {
      return authenticationApi.changePassword(id, passwordUpdate.oldPassword, passwordUpdate.newPassword)
        ?: throw BadRequestException("Old password does not match.")
    }
    throw ForbiddenException()
  }

  /**
   * We have to do this at request time, because some urls are time sensitive (e.g. oauth ones with the state
   * parameter).
   *
   * @param provider
   * @return
   */
  override fun getLoginUrlForProvider(provider: String): ProviderRedirect {
    val authProviderSource = authProviderCollection.find(provider)
    if (authProviderSource != null) {
      return ProviderRedirect().redirectUrl(authProviderSource.redirectUrl!!)
    }
    throw NotFoundException()
  }

  override fun login(userCredentials: UserCredentials): TokenizedPerson {
    // if access via this API is forbidden (for example only GUI based OAuth or SAML login is allowed)
    // then fail requests automatically
    if (loginDisabled == true) {
      throw ForbiddenException()
    }

    // can't try and login with a null or empty password
    if (userCredentials.password.trim { it <= ' ' }.isEmpty()) {
      throw ForbiddenException()
    }

    val login = authenticationApi.login(userCredentials.email, userCredentials.password)
      ?: throw NotFoundException()
    return TokenizedPerson().accessToken(authRepository.put(login)).person(login)
  }

  override fun logout(securityContext: SecurityContext?) {
    authRepository.invalidate(securityContext)
  }

  override fun personByToken(token: String): Person {
    return authenticationApi.getPersonByToken(token)
      ?: throw NotFoundException("No person by that token")
  }

  override fun registerPerson(personRegistrationDetails: PersonRegistrationDetails): TokenizedPerson {

    //check user found by token and token hasn't expired
    val person = personApi.getByToken(personRegistrationDetails.registrationToken, Opts.opts(FillOpts.Groups))
      ?: throw NotFoundException("Person already registered using token")

    if (person.email == null || !person.email.equals(
        personRegistrationDetails.email.lowercase(Locale.getDefault()),
        ignoreCase = true
      )
    ) {
      log.info("db user email `{}` does not match passed email `{}`", person.email, personRegistrationDetails.email)
      throw BadRequestException()
    }

    if (personRegistrationDetails.password != personRegistrationDetails.confirmPassword) {
      //passwords don't match
      throw BadRequestException()
    }

    val newPerson = authenticationApi.register(
      personRegistrationDetails.name,
      personRegistrationDetails.email,
      personRegistrationDetails.password, null
    ) ?: throw NotFoundException("Cannot find person to register")

    return TokenizedPerson().accessToken(authRepository.put(person.copy())).person(newPerson)
  }

  override fun replaceTempPassword(
    id: UUID,
    passwordReset: PasswordReset,
    context: SecurityContext?
  ): TokenizedPerson {
    val person = authManager.from(context)
    if (true == person.passwordRequiresReset) {
      if (person.id!!.id == id) { // its me
        val newPerson = authenticationApi.replaceTemporaryPassword(id, passwordReset.password)
        authRepository.invalidate(context)
        return TokenizedPerson().accessToken(authRepository.put(newPerson)).person(newPerson)
      }
    }
    throw ForbiddenException()
  }

  override fun resetExpiredToken(email: String, context: SecurityContext?): RegistrationUrl {
    val person = authManager.from(context)
    if (authManager.isAnyAdmin(person)) {
      val token = authenticationApi.resetExpiredRegistrationToken(email)
      return RegistrationUrl()
        .personId(person.id!!.id)
        .registrationUrl(token)
        .token(token)
    }
    throw ForbiddenException()
  }

  override fun resetPassword(id: UUID, passwordReset: PasswordReset, context: SecurityContext?): Person {
    if (authManager.isAnyAdmin(authManager.from(context))) {
      return authenticationApi.resetPassword(
        id, passwordReset.password,
        authManager.from(context).id!!.id, true == passwordReset.reactivate
      )
        ?: throw NotFoundException()
    }
    throw ForbiddenException()
  }
}
