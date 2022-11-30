package io.featurehub.db.api

import io.featurehub.mr.model.*
import java.util.*

interface EnvironmentApi {
  fun personRoles(current: Person, eid: UUID): EnvironmentRoles?
  fun setOrdering(app: Application, environments: List<Environment>): List<Environment>?
  class DuplicateEnvironmentException : Exception()
  class InvalidEnvironmentChangeException : Exception()

  fun delete(id: UUID?): Boolean
  operator fun get(id: UUID, opts: Opts?, current: Person?): Environment?

  @Throws(
    OptimisticLockingException::class,
    DuplicateEnvironmentException::class,
    InvalidEnvironmentChangeException::class
  )
  fun update(envId: UUID?, env: Environment?, opts: Opts?): Environment?

  @Throws(DuplicateEnvironmentException::class, InvalidEnvironmentChangeException::class)
  fun create(env: Environment?, app: Application?, whoCreated: Person?): Environment?
    fun search(appId: UUID?, filter: String?, order: EnvironmentSortOrder?, opts: Opts?, current: Person?): List<Environment?>?
  fun findPortfolio(envId: UUID?): Portfolio?
  fun unpublishEnvironments(appId: UUID, environments: List<UUID>?): Int
  fun updateEnvironment(eid: UUID, env: UpdateEnvironment, opts: Opts): Environment?
}
