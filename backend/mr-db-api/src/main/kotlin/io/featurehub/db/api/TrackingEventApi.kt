package io.featurehub.db.api

import cd.connect.cloudevents.TaggedCloudEvent
import io.featurehub.events.DynamicCloudEventDestinationMapper
import io.featurehub.trackedevent.models.TrackedEventResult
import java.time.OffsetDateTime
import java.util.*

enum class CloudEventLinkType {
  env, // environment
}

interface TrackingEventApi {
  /**
   * This is called by a message generator to indicate that extension config should be looked for - such as overrides on
   * keeping success messages.
   */
  fun registerTrackingConfig(mappers: List<DynamicCloudEventDestinationMapper>)

  /**
   * This allows you to track one or more events against a cloud event. If it cannot be found, it is ignored.
   * Typically a success response will cause the original message to be deleted as successful operation isn't
   * interesting.
   */
  fun trackEvent(te: TrackedEventResult)

  /**
   * This is called by the subsystem that creates the initial cloud event. You cannot track events
   * in the system if they aren't initially created against this API.
   */
  fun <T: TaggedCloudEvent> createInitialRecord(messageId: UUID,
                                                cloudEventType: String,
                                                linkType: CloudEventLinkType,
                                                linkId: UUID, data: T,
                                                whenCreated: OffsetDateTime,
                                                metadata: String?)
}
