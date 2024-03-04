package io.featurehub.events

import cd.connect.cloudevents.TaggedCloudEvent
import io.cloudevents.CloudEvent

class FakeSimpleRegistryListener<T: TaggedCloudEvent>(val clazz: Class<T>, reg: CloudEventBaseReceiverRegistry) {
  var data: Any? = null
  var ce: CloudEvent? = null

  init {
    reg.listen(clazz, this::listen)
  }

  private fun listen(any: T, cloudEvent: CloudEvent) {
    data = any
    ce = cloudEvent
  }
}
class FakeSubjectRegistryListener<T: TaggedCloudEvent>(val clazz: Class<T>, type: String, subject: String, reg: CloudEventBaseReceiverRegistry) {
  var data: Any? = null
  var ce: CloudEvent? = null

  init {
    reg.listen(clazz, type, subject, this::listen)
  }

  private fun listen(any: T, cloudEvent: CloudEvent) {
    data = any
    ce = cloudEvent
  }
}

class FakeTypeRegistryListener<T: TaggedCloudEvent>(val clazz: Class<T>, type: String, reg: CloudEventBaseReceiverRegistry) {
  var data: Any? = null
  var ce: CloudEvent? = null

  init {
    reg.listen(clazz, type, null, this::listen)
  }

  private fun listen(any: T, cloudEvent: CloudEvent) {
    data = any
    ce = cloudEvent
  }
}


