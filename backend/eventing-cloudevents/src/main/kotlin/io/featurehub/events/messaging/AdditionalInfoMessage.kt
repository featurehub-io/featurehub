package io.featurehub.events.messaging

import cd.connect.cloudevents.TaggedCloudEvent

interface AdditionalInfoMessage<T> : TaggedCloudEvent {
  fun additionalInfo(additionalInfo: Map<String, String>?): T
  fun putAdditionalInfoItem(key: String, additionalInfoItem: String): T

  var additionalInfo: Map<String, String>?
}
