package io.featurehub.events

import io.cloudevents.core.builder.CloudEventBuilder
import java.util.function.Consumer

/**
 * This just passes the request straight through
 */
class FakeTelemetryWriteCallback : CloudEventsTelemetryWriter {
  private var type: String? = null
  private var builder: CloudEventBuilder? = null
  private var metrics: CloudEventChannelMetric? = null
  var triggered: Int = 0

  override fun publish(
    type: String,
    builder: CloudEventBuilder,
    metrics: CloudEventChannelMetric,
    broadcastWriter: (event: CloudEventBuilder) -> Unit
  ) {
    triggered ++
    broadcastWriter.invoke(builder)
  }
}
