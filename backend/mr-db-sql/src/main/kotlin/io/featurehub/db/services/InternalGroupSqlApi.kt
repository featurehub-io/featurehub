package io.featurehub.db.services

import io.featurehub.db.model.DbGroup
import io.featurehub.db.model.DbOrganization
import io.featurehub.db.model.DbPerson
import java.util.*

// if we just added a person to a group, don't add them again
class SuperuserChanges(var organization: DbOrganization) {
  val removedSuperusers = mutableListOf<UUID>()
  val addedSuperusers = mutableListOf<DbPerson>()
  val ignoredGroups = mutableListOf<UUID>()
}

interface InternalGroupSqlApi {
  fun updateSuperusersFromPortfolioGroups(superuserChanges: SuperuserChanges)
  fun superuserGroup(org: DbOrganization): DbGroup?
}
