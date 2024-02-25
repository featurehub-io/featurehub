package io.featurehub.db.services

import cd.connect.cloudevents.TaggedCloudEvent
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JSR310Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ebean.annotation.Transactional
import io.featurehub.db.api.CloudEventLinkType
import io.featurehub.db.api.TrackingEventApi
import io.featurehub.db.model.DbCloudEventLog
import io.featurehub.db.model.query.QDbCloudEventLog
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.DynamicCloudEventDestination
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.mr.model.TrackEventResponse
import io.featurehub.mr.model.TrackEventsSummary
import io.featurehub.mr.model.TrackEventsSummaryItem
import io.featurehub.trackedevent.models.TrackedEventResult
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.math.max
import kotlin.math.min

@JsonIgnoreProperties(ignoreUnknown = true)
data class TrackedEventDeliveryResult(val method: String,
                                      val content: String?,
                                      val status: Int,
                                      val headers: Map<String, String>?,
                                      val whenR: Instant,
                                      val extra: Map<String,String>?)

@LifecyclePriority(LifecyclePriority.APPLICATION_PRIORITY_START)
class TrackingEventListener @Inject constructor(cloudEventReceiverRegistry: CloudEventReceiverRegistry, trackingEventApi: TrackingEventApi) : LifecycleListener {
  init {
    cloudEventReceiverRegistry.listen(TrackedEventResult::class.java) { msg, ce -> trackingEventApi.trackEvent(msg, ce.time?.toInstant() ?: Instant.now()) }
  }
}

/**
 * We store DbCloudEventLog entries whenever we send an interesting event. It is uniquely indexed by its ID.
 *
 * Every time a response comes in from the delivery of said event, we record it against the event. If the event has
 * already been deleted, we drop it.
 *
 * We keep the first successful response after it gets sent or when recovering from a failure. We keep all failures.
 */

class TrackingEventSqlApi @Inject constructor(private val conversions: Conversions) : TrackingEventApi {
  private val defaultKeepSuccesses = FallbackPropertyConfig.getConfig("tracking-event.keep-successes", "false") == "true"
  private val keepIndividualTypeSuccess = mutableMapOf<String, Boolean>()

  companion object {
    private val mapper = ObjectMapper().apply { registerModule(KotlinModule.Builder().build())
      .registerModule(JavaTimeModule()) }
    private val teRef = object: TypeReference<List<TrackedEventDeliveryResult>>() {}
  }

  override fun registerTrackingConfig(mappers: List<DynamicCloudEventDestination>) {
    val defaultVal = defaultKeepSuccesses.toString()
    mappers.forEach { mapper ->
      val config = FallbackPropertyConfig.getConfig("tracking-event.TODO.keep-successes", defaultVal) == "true"
      keepIndividualTypeSuccess[mapper.cloudEventType] = config
    }
  }

  @Transactional
  override fun trackEvent(te: TrackedEventResult, whenReceived: Instant) {
    // we make sure this message id is of the right type and is owned by said organisation
    val finder = QDbCloudEventLog()
      .id.eq(te.originatingCloudEventMessageId)
      .owner.id.eq(te.originatingOrganisationId)
      .type.eq(te.originatingCloudEventType)

    // if we can't find it, its been deleted so we don't worry about it
    finder.findOne()?.let { event ->
      // only look up  the last one if this is successful and we delete successful cloud events
      if (te.status == 200 && !keepSuccessfulCloudEvents(te.originatingCloudEventType)) {
      // this one will always _at least_ find this one
      val lastOne = QDbCloudEventLog()
        .link.eq(event.link)
        .linkType.eq(event.linkType)
        .status.isNotNull
        .type.eq(te.originatingCloudEventType)
        .orderBy().whenUpdated.desc()
        .setMaxRows(1).findOne()
      if (lastOne != null && lastOne.id != te.originatingCloudEventMessageId && lastOne.status!! < 300) {
        // we should delete the successes by default to avoid cluttering the database
          finder.delete()
          return
        }
      }

      val teData = event.trackedEvents
      val saveData = TrackedEventDeliveryResult(te.method.name, te.content, te.status, te.incomingHeaders, whenReceived, te.extra)
      val newContent = if (teData == null) {
        listOf(saveData)
      } else {
        parseData(teData).apply {
          add(saveData)
        }
      }

      event.status = te.status // update the status with the last one that was returned

      event.trackedEvents = mapper.writeValueAsString(newContent)
      event.save()
    }
  }

  @Transactional
  override fun <T : TaggedCloudEvent> createInitialRecord(
    messageId: UUID,
    cloudEventType: String,
    linkType: CloudEventLinkType,
    linkId: UUID,
    data: T,
    whenCreated: OffsetDateTime, metadata: String?
  ) {
    DbCloudEventLog(messageId, conversions.dbOrganization(),
      cloudEventType, linkType.name, linkId, mapper.writeValueAsString(data), whenCreated.toInstant(), metadata).save()
  }

  override fun findEvents(source: String, id: UUID, cloudEventType: String, page: Int, pageSize: Int, firstOnly: Boolean): TrackEventsSummary {
    val minPage = max(0, page)
    val maxRows = min(25, pageSize)
    // e.g. env and env-id
    val finder = QDbCloudEventLog().linkType.eq(source).link.eq(id).type.eq(cloudEventType)
      .orderBy().whenUpdated.desc()

    val finderCount = finder.findFutureCount()
    val finderData = finder.setFirstRow(minPage * maxRows).setMaxRows(maxRows)
      .select(QDbCloudEventLog.Alias.trackedEvents, QDbCloudEventLog.Alias.whenCreated).findFutureList()

    return TrackEventsSummary().count(finderCount.get().toLong())
      .items( finderData.get()
        .map { logToTrackEvent(it, firstOnly) } )
  }

  private fun makeSummaryItem(tr: TrackedEventDeliveryResult): TrackEventResponse {
      return TrackEventResponse()
        .status(tr.status)
        .whenReceived(tr.whenR.atOffset(ZoneOffset.UTC))
        .headers(tr.headers).message(tr.content)
  }

  private fun logToTrackEvent(log: DbCloudEventLog, firstOnly: Boolean): TrackEventsSummaryItem {
    val item = TrackEventsSummaryItem()
      .id(log.id!!)
      .whenSent(log.whenCreated.atOffset(ZoneOffset.UTC))

    log.trackedEvents?.let {
      val data = parseData(it)

      if (data.isNotEmpty()) {
        if (firstOnly) {
          item.eventResponses = listOf(makeSummaryItem(data.first()))
        } else {
          item.eventResponses = data.map { makeSummaryItem(it) }
        }
      }
    }

    return item
  }

  private fun parseData(resultData: String): MutableList<TrackedEventDeliveryResult> {
    return mapper.readValue(resultData, teRef).toMutableList()
  }

  private fun keepSuccessfulCloudEvents(originatingCloudEventType: String): Boolean {
    return keepIndividualTypeSuccess[originatingCloudEventType] ?: defaultKeepSuccesses
  }
}
