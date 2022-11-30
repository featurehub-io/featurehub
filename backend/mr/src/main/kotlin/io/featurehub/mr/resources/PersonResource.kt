package io.featurehub.mr.resources

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.db.api.*
import io.featurehub.db.services.Conversions
import io.featurehub.mr.api.PersonServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.*
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.*
import java.util.Set
import java.util.stream.Collectors

class PersonResource @Inject constructor(
  private val personApi: PersonApi,
  private val groupApi: GroupApi,
  private val authManager: AuthManagerService
) : PersonServiceDelegate {

  @ConfigKey("register.url")
  private var registrationUrl: String? = "http://localhost:register-url?token=%s"

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun createPerson(
    createPersonDetails: CreatePersonDetails, holder: PersonServiceDelegate.CreatePersonHolder,
    securityContext: SecurityContext
  ): RegistrationUrl {
    val currentUser = authManager.from(securityContext)

    if (!authManager.isAnyAdmin(currentUser.id!!.id)) {
      throw ForbiddenException("Not allowed")
    }

    if (createPersonDetails.personType == PersonType.SERVICEACCOUNT) {
      return createServiceAccount(createPersonDetails, currentUser)
    }

    if (createPersonDetails.email == null) {
      throw BadRequestException("Email is a required parameter for creating a person.")
    }

    //create new user in the db
    return try {
      val person = personApi.create(
        createPersonDetails.email!!,
        createPersonDetails.name, currentUser.id!!.id
      )
      if (person?.id == null) {
        throw BadRequestException()
      }

      createPersonDetails.groupIds?.forEach { id ->
        groupApi.addPersonToGroup(id, person.id, Opts.empty())
      }

      //return registration url
      RegistrationUrl()
        .personId(person.id)
        .registrationUrl(
          String.format(
            registrationUrl!!,
            person.token
          )
        ) // hard code the return value, it will be ignored by the client from now on
        .token(person.token)
    } catch (e: PersonApi.DuplicatePersonException) {
      throw WebApplicationException(Response.status(Response.Status.CONFLICT).build())
    }
  }

  private fun createServiceAccount(
    createPersonDetails: CreatePersonDetails,
    currentUser: Person
  ): RegistrationUrl {
    if (createPersonDetails.name == null) {
      throw BadRequestException("Name is required parameter")
    }

    val (person, token) = personApi.createServicePerson(
      createPersonDetails.name!!,
      currentUser.id!!.id
    ) ?: throw BadRequestException()
    val servicePersonId = person.id!!.id

    createPersonDetails.groupIds?.forEach { id ->
      groupApi.addPersonToGroup(id, servicePersonId, Opts.empty())
    }

    // return registration url
    return RegistrationUrl()
      .personId(servicePersonId)
      .token(token)
  }

  private fun peopleOpts(includeAcls: Boolean?, includeGroups: Boolean?): Opts {
    return Opts()
      .add(FillOpts.Groups, includeGroups)
      .add(FillOpts.Acls, includeAcls)
  }

  private fun getPerson(
    id: String,
    includeAcls: Boolean?,
    includeGroups: Boolean?,
    securityContext: SecurityContext
  ): Person {
    val currentUser = authManager.from(securityContext)

    val personId: UUID?
    if ("self" == id) {
      personId = currentUser.id!!.id
      log.debug("User requested their own details: {}", id)
    } else {
      personId = Conversions.checkUuid(id)
    }

    if (currentUser.id!!.id == personId || authManager.isAnyAdmin(currentUser.id!!.id)) {
      val person = if (personId == null) {
        personApi[id, peopleOpts(includeAcls, includeGroups)]
      } else {
        personApi[personId, peopleOpts(includeAcls, includeGroups)]
      }
      if (person == null) {
        throw NotFoundException()
      }
      return person
    }
    throw ForbiddenException("You are not allowed the details of this person")
  }

  override fun deletePerson(
    id: UUID,
    holder: PersonServiceDelegate.DeletePersonHolder,
    securityContext: SecurityContext
  ): Boolean {
    if (authManager.isOrgAdmin(authManager.from(securityContext))) {
      val p = getPerson(id.toString(), false, false, securityContext)
      if (p.email != null) {
        return personApi.delete(p.email!!, holder.includeGroups == true)
      }
    }
    throw ForbiddenException("No permission")
  }

  override fun findPeople(
    holder: PersonServiceDelegate.FindPeopleHolder,
    securityContext: SecurityContext
  ): SearchPersonResult {
    val currentUser = authManager.from(securityContext)
    if (!authManager.isAnyAdmin(currentUser.id!!.id)) throw ForbiddenException("Not Admin")

    val start = if (holder.startAt == null) 0 else holder.startAt
    val page = if (holder.pageSize == null) 20 else holder.pageSize

    val pp = personApi.search(
      holder.filter, holder.order, start, page,
      if (holder.personTypes == null) Set.of(PersonType.PERSON) else HashSet(holder.personTypes),
      holder.sortBy,
      Opts().add(FillOpts.Groups, holder.includeGroups)
        .add(FillOpts.CountGroups, holder.countGroups)
        .add(FillOpts.Archived, holder.includeDeactivated)
        .add(FillOpts.PersonLastLoggedIn, holder.includeLastLoggedIn)
    )

    val persons = if (holder.countGroups == true)
      SearchPersonResult().summarisedPeople(pp.searchPeople)
    else
      SearchPersonResult().people(pp.people)

    return persons
      .outstandingRegistrations(
        pp.personsWithOutstandingTokens.stream().map { pt: PersonApi.PersonToken ->
          OutstandingRegistration().id(pt.id).token(pt.token)
            .expired(pp.personIdsWithExpiredTokens.contains(pt.id))
        }
          .collect(Collectors.toList()))
      .max(pp.max)
  }

  override fun getPerson(
    id: String,
    holder: PersonServiceDelegate.GetPersonHolder,
    securityContext: SecurityContext
  ): Person {
    return getPerson(id, holder.includeAcls, holder.includeGroups, securityContext)
  }

  override fun resetSecurityToken(id: UUID, securityContext: SecurityContext): AdminServiceResetTokenResponse {
    val currentUser = authManager.from(securityContext)
    if (authManager.isAnyAdmin(currentUser.id!!.id)) {
      val updatedServicePerson = personApi.resetServicePersonToken(id)
      if (updatedServicePerson != null) {
        return AdminServiceResetTokenResponse().token(updatedServicePerson.token)
      }
      throw NotFoundException()
    }
    throw ForbiddenException()
  }

  override fun updatePerson(
    id: UUID,
    person: Person,
    holder: PersonServiceDelegate.UpdatePersonHolder,
    securityContext: SecurityContext
  ): Person {
    val from = authManager.from(securityContext)
    if (authManager.isAnyAdmin(from)) {
      return try {
        personApi.update(
          id, person,
          peopleOpts(holder.includeAcls, holder.includeGroups),
          from.id!!.id
        )
      } catch (e: OptimisticLockingException) {
        throw WebApplicationException(422)
      } catch (iae: IllegalArgumentException) {
        throw BadRequestException()
      } ?: throw NotFoundException()
    }
    throw ForbiddenException("Not authorised")
  }

  override fun updatePersonV2(
    id: UUID,
    updatePerson: UpdatePerson,
    securityContext: SecurityContext
  ) {
    val from = authManager.from(securityContext)

    if (authManager.isAnyAdmin(from)) {
      try {
        personApi.updateV2(
          id, updatePerson,
          from.id!!.id
        )
      } catch (e: OptimisticLockingException) {
        throw WebApplicationException(422)
      } catch (iae: IllegalArgumentException) {
        throw BadRequestException()
      } ?: throw NotFoundException()

      return
    }

    throw ForbiddenException("Not authorised")
  }

  companion object {
    private val log = LoggerFactory.getLogger(PersonResource::class.java)
  }
}
