package io.featurehub.edge.db.sql

import io.ebean.annotation.Transactional
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

      upd = upd.set("defaultValue", updateValue)
    }


    upd.setRaw("version = version + 1")

    upd.update()
  }
}
