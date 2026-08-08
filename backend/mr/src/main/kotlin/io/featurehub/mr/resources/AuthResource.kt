package io.featurehub.mr.resources

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.db.api.AuthenticationApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonApi
import io.featurehub.db.password.PasswordPolicy
import io.featurehub.db.password.PasswordPolicyViolationException
import io.featurehub.mr.api.AuthServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.auth.AuthenticationRepository
import io.featurehub.mr.model.*
import io.featurehub.web.security.oauth.AuthProviderCollection
import jakarta.annotation.security.PermitAll
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.String

class AuthResource @Inject constructor(
  private val authenticationApi: AuthenticationApi,
  private val authManager: AuthManagerService, private val personApi: PersonApi,
  private val authRepository: AuthenticationRepository, private val authProviderCollection: AuthProviderCollection,
  private val passwordPolicy: PasswordPolicy
) : AuthServiceDelegate {
  private val log = LoggerFactory.getLogger(AuthResource::class.java)

  @ConfigKey("auth.disable-login")
  private var loginDisabled:Boolean? = false

  init {
    DeclaredConfigResolver.resolve(this)
  }

  /**
   * Rejects a password that fails the complexity policy with a 400 carrying every rule it violated,
   * so the (localised) admin console can render its own guidance.
   *
   * Only ever called on a path that *sets* a password. Login and the old-password half of a change
   * request deliberately do not go through here - stored passwords predate this policy and their
   * owners must still be able to authenticate. See PasswordPolicy.
   */
  private fun enforcePasswordPolicy(password: String?) {
    try {
      passwordPolicy.validate(password)
    } catch (e: PasswordPolicyViolationException) {
      throw WebApplicationException(
        Response.status(Response.Status.BAD_REQUEST)
          .entity(
            PasswordPolicyViolationReport()
              .violatedRules(e.violations.map { PasswordPolicyRule.valueOf(it.name) })
              .minLength(e.minLength)
              .maxLength(e.maxLength)
          )
          .build()
      )
    }
  }

  override fun changePassword(id: UUID, passwordUpdate: PasswordUpdate, securityContext: SecurityContext?): Person {
    val personByToken = authManager.from(securityContext)

    // yourself or a superuser can change your password. This allows a superuser to change the password immediately
    // after reset without having to go to any further trouble.
    if (personByToken.id!!.id == id || authManager.isOrgAdmin(personByToken)) {
      // only the new password is policed - the old one is an existing credential being verified
      enforcePasswordPolicy(passwordUpdate.newPassword)

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

  @PermitAll
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

    enforcePasswordPolicy(personRegistrationDetails.password)

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
        enforcePasswordPolicy(passwordReset.password)

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
      // after the admin check, so an unauthorised caller gets a bare 403 and learns nothing about the policy
      enforcePasswordPolicy(passwordReset.password)

      return authenticationApi.resetPassword(
        id, passwordReset.password,
        authManager.from(context).id!!.id, true == passwordReset.reactivate
      )
        ?: throw NotFoundException()
    }
    throw ForbiddenException()
  }
}
