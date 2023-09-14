package io.featurehub.db.api

import io.featurehub.mr.model.*
import java.util.*

interface FeatureGroupApi {
  class DuplicateNameException: Exception()
  class OptimisticLockingException: Exception()

  fun createGroup(appId: UUID, current: Person, featureGroup: FeatureGroupCreate): FeatureGroup?
  fun deleteGroup(appId: UUID, current: Person, fgId: UUID): Boolean
  fun getGroup(appId: UUID, fgId: UUID): FeatureGroup?
  fun listGroups(appId: UUID, maxPerPage: Int, filter: String?, pageNum: Int, sortOrder: SortOrder, environmentId: UUID?, appPerms: ApplicationPermissions): FeatureGroupList
  fun updateGroup(appId: UUID, current: Person, fgId: UUID, update: FeatureGroupUpdate): FeatureGroup?
  fun getFeaturesForEnvironment(appId: UUID, envId: UUID): List<FeatureGroupFeature>
}
