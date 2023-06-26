package io.featurehub.db.services

import io.ebean.Database
import io.ebean.annotation.Transactional
import io.featurehub.db.api.SetupApi
import io.featurehub.db.model.DbPerson
import io.featurehub.mr.model.SetupSiteAdmin
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class SetupSqlApi @Inject constructor(private val convertUtils: Conversions) :
  SetupApi {
  override fun initialized(): Boolean {
    return convertUtils.hasOrganisation()
  }

  @Transactional
  override fun setup(setupSiteAdmin: SetupSiteAdmin): Boolean {
    assert(setupSiteAdmin.emailAddress != null && setupSiteAdmin.name != null)
    if (!initialized()) {
      DbPerson.Builder().email(setupSiteAdmin.emailAddress).name(setupSiteAdmin.name).build().save()
      return true
    }
    return false
  }
}
