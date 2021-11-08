package io.featurehub.edge.db.sql

import io.ebean.annotation.Transactional
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.edge.rest.FeatureUpdatePublisher
import io.featurehub.mr.messaging.StreamedFeatureUpdate

class DbFeatureUpdateProcessor: FeatureUpdatePublisher {
  @Transactional(readOnly = false)
  override fun publishFeatureChangeRequest(featureUpdate: StreamedFeatureUpdate, namedCache: String) {
    var upd = QDbFeatureValue().feature.key.eq(featureUpdate.featureKey).environment.id.eq(featureUpdate.environmentId).asUpdate()

    if (featureUpdate.lock != null) {
      upd = upd.set("locked", featureUpdate.lock)
    }

    if (featureUpdate.updatingValue == true) {
      var updateValue: String? = determineDefaultValue(featureUpdate)

      upd = upd.set("defaultValue", updateValue)
    }

    upd.setRaw("version = version + 1")

    if (upd.update() == 0) {
      val builder = DbFeatureValue.Builder().locked(featureUpdate.lock ?: false)
      if (featureUpdate.updatingValue == true) {
        builder.defaultValue(determineDefaultValue(featureUpdate))
      }
      builder.environment(QDbEnvironment().id.eq(featureUpdate.environmentId).findOne())
      builder.feature(QDbApplicationFeature().select(QDbApplicationFeature.Alias.id).key.eq(featureUpdate.featureKey).parentApplication.id.eq(featureUpdate.applicationId!!).findOne())
      builder.build().save()
    }
  }

  private fun determineDefaultValue(featureUpdate: StreamedFeatureUpdate): String? {
    var updateValue: String? = null
    if (featureUpdate.valueBoolean != null) {
      updateValue = featureUpdate.valueBoolean.toString()
    } else if (featureUpdate.valueNumber != null) {
      updateValue = featureUpdate.valueNumber.toString()
    } else if (featureUpdate.valueString != null) {
      updateValue = featureUpdate.valueString
    }

    // we have a rule - empty strings should be null (not set)
    if (updateValue?.isEmpty() == true) {
      updateValue = null
    }
    return updateValue
  }
}
