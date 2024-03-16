package io.featurehub.db.api

import cd.connect.cloudevents.TaggedCloudEvent
import io.featurehub.events.DynamicCloudEventDestination
import io.featurehub.mr.model.TrackEventsSummary
import io.featurehub.trackedevent.models.TrackedEventResult
import java.time.Instant
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
  fun registerTrackingConfig(mappers: List<DynamicCloudEventDestination>)

  /**
   * This allows you to track one or more events against a cloud event. If it cannot be found, it is ignored.
   * Typically a success response will cause the original message to be deleted as successful operation isn't
   * interesting.
   */
  fun trackEvent(te: TrackedEventResult, whenReceived: Instant)

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

  /**
   * Given the source and the id, find the associated data in reverse date order and return a page of it
   * determined by the page size.
   */
  fun findEvents(source: String, id: UUID, cloudEventType: String, page: Int, pageSize: Int, firstOnly: Boolean): TrackEventsSummary
}
