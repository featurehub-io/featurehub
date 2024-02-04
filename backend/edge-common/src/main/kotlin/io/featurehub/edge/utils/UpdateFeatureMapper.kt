package io.featurehub.edge.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper

class UpdateFeatureMapper : UpdateMapper {
  override val mapper = ObjectMapper().apply {
    enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
  }
}
