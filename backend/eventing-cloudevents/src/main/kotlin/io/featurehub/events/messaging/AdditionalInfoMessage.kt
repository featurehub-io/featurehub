package io.featurehub.events.messaging

interface AdditionalInfoMessage<T> {
  fun additionalInfo(additionalInfo: Map<String, String>?): T
  fun putAdditionalInfoItem(key: String, additionalInfoItem: String): T

  var additionalInfo: Map<String, String>?
}
