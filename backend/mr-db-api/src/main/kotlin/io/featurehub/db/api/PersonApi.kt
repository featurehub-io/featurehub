package io.featurehub.db.api

import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonType
import io.featurehub.mr.model.SearchPerson
import io.featurehub.mr.model.SortOrder
import java.util.*

interface PersonApi {
    @Throws(OptimisticLockingException::class)
    fun update(id: UUID, person: Person, opts: Opts, updatedBy: UUID): Person?

    // used to determine if the database has no user, which is possible if using external auth
    fun noUsersExist(): Boolean
    data class PersonPagination(val max: Int,
                                val searchPeople: List<SearchPerson>,
                                val people: List<Person>,
                                val personsWithOutstandingTokens: List<PersonToken>,
                                val personIdsWithExpiredTokens: List<UUID>
    )

    data class PersonToken(val token: String, val id: UUID)
    class DuplicatePersonException : Exception()

    fun search(
        filter: String?, sortOrder: SortOrder?, offset: Int, max: Int,
        personTypes: Set<PersonType?>,
        opts: Opts
    ): PersonPagination

    operator fun get(id: UUID, opts: Opts): Person?
    operator fun get(email: String, opts: Opts): Person?
    fun getByToken(token: String, opts: Opts): Person?

    @Throws(DuplicatePersonException::class)
    fun create(email: String, name: String?, createdBy: UUID?): PersonToken?
    fun createServicePerson(name: String, createdBy: UUID?): CreatedServicePerson?
    fun resetServicePersonToken(serviceAccountId: UUID): CreatedServicePerson?
    fun delete(email: String): Boolean

}
