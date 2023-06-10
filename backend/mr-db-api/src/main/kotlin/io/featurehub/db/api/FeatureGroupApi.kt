package io.featurehub.db.api

import io.featurehub.mr.model.*
import java.util.*

interface FeatureGroupApi {
  class DuplicateNameException: Exception()

  fun createGroup(appId: UUID, current: Person, featureGroupUpdate: FeatureGroupUpdate): FeatureGroup
  fun deleteGroup(appId: UUID, current: Person, fgId: UUID): Boolean
  fun getGroup(appId: UUID, current: Person, fgId: UUID): FeatureGroup?
  fun listGroups(appId: UUID, maxPerPage: Int, filter: String?, pageNum: Int, sortOrder: SortOrder): FeatureGroupList
  fun updateGroup(appId: UUID, current: Person, fgId: UUID, featureGroupUpdate: FeatureGroupUpdate): FeatureGroup?
}
