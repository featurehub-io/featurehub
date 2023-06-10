package io.featurehub.db.services

import io.featurehub.db.api.FeatureGroupApi
import io.featurehub.mr.model.*
import java.util.*

/**
 * We never check the appId as we assume from the front end that it always does that as part of its
 * permissions check. We do not check permissions either.
 */
class FeatureGroupSqlApi : FeatureGroupApi {
  override fun createGroup(appId: UUID, current: Person, featureGroupUpdate: FeatureGroupUpdate): FeatureGroup {
    TODO("Not yet implemented")
  }

  override fun deleteGroup(appId: UUID, current: Person, fgId: UUID): Boolean {
    TODO("Not yet implemented")
  }

  override fun getGroup(appId: UUID, current: Person, fgId: UUID): FeatureGroup? {
    TODO("Not yet implemented")
  }

  override fun listGroups(
    appId: UUID,
    maxPerPage: Int,
    filter: String?,
    pageNum: Int,
    sortOrder: SortOrder
  ): FeatureGroupList {
    TODO("Not yet implemented")
  }

  override fun updateGroup(
    appId: UUID,
    current: Person,
    fgId: UUID,
    featureGroupUpdate: FeatureGroupUpdate
  ): FeatureGroup? {
    TODO("Not yet implemented")
  }
}
