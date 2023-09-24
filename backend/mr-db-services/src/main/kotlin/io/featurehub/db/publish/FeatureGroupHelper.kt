package io.featurehub.db.publish

import io.featurehub.mr.model.FeatureValueType
import java.math.BigDecimal

class FeatureGroupHelper {
  companion object {
    fun cast(value: String?, valueType: FeatureValueType): Any? {
      if (value == null) return null
      return when (valueType) {
        FeatureValueType.BOOLEAN -> "true" == value
        FeatureValueType.STRING -> value.toString()
        FeatureValueType.NUMBER -> BigDecimal(value.toString())
        FeatureValueType.JSON -> value.toString()
      }
    }

  }
}
