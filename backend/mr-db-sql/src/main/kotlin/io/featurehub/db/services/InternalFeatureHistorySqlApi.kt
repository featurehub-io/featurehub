package io.featurehub.db.services

import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.query.QDbFeatureValueVersion
import java.util.UUID

interface InternalFeatureHistoryApi {
}



class InternalFeatureHistorySqlApi : InternalFeatureHistoryApi {
  fun history(environmentId: UUID, applicationFeature: UUID, featureValue: UUID): List<DbFeatureValueVersion> {
    return QDbFeatureValueVersion().feature.id.eq(applicationFeature).id.id.eq(featureValue).findList();
  }
}
