package io.featurehub.messaging.utils

import cd.connect.cloudevents.CloudEventSubject
import cd.connect.cloudevents.CloudEventType
import cd.connect.cloudevents.TaggedCloudEvent

@CloudEventType("testing-slack-v1")
@CloudEventSubject("io.featurehub.events.testing.slack")
class TestingSlackMessage : TaggedCloudEvent {

  companion object {
    const val CLOUD_EVENT_TYPE: String = "testing-slack-v1"
    const val CLOUD_EVENT_SUBJECT: String = "io.featurehub.events.testing.slack"
  }

  var newTargetUrl: String? = null
}
