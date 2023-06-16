package io.featurehub.db.services

import io.featurehub.db.model.DbPerson
import java.util.*

interface InternalPersonApi {
  // expects the calling code to save it
  fun createSdkServiceAccountUser(name: String, createdBy: DbPerson, archived: Boolean): DbPerson
  fun deleteSdkServiceAccountUser(personId: UUID, createdBy: DbPerson)

  fun updateSdkServiceAccountUser(personId: UUID, updatedBy: DbPerson, name: String)

  /**
   * If we need to do some internal operation we need to blame someone
   */
  fun findSuperUserToBlame(orgId: UUID): DbPerson
}
