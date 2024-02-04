package io.featurehub.db.services

import cd.connect.cloudevents.TaggedCloudEvent
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ebean.annotation.Transactional
import io.featurehub.db.api.CloudEventLinkType
import io.featurehub.db.api.TrackingEventApi
import io.featurehub.db.model.DbCloudEventLog
import io.featurehub.db.model.query.QDbCloudEventLog
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.DynamicCloudEventDestinationMapper
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.trackedevent.models.TrackedEventResult
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import java.time.OffsetDateTime
import java.util.*

@LifecyclePriority(LifecyclePriority.APPLICATION_PRIORITY_START)
class TrackingEventListener @Inject constructor(cloudEventReceiverRegistry: CloudEventReceiverRegistry, trackingEventApi: TrackingEventApi) : LifecycleListener {
  init {
    cloudEventReceiverRegistry.listen(TrackedEventResult::class.java) { msg, _ -> trackingEventApi.trackEvent(msg) }
  }
}

class TrackingEventSqlApi : TrackingEventApi {
  private val defaultKeepSuccesses = FallbackPropertyConfig.getConfig("tracking-event.keep-successes", "false") == "true"
  private val individualTypeSuccess = mutableMapOf<String, Boolean>()

  companion object {
    private val mapper = ObjectMapper().apply { registerModule(KotlinModule.Builder().build()) }
    private val teRef = object: TypeReference<List<TrackedEventResult>>() {}
  }

  override fun registerTrackingConfig(mappers: List<DynamicCloudEventDestinationMapper>) {
    val defaultVal = defaultKeepSuccesses.toString()
    mappers.forEach { mapper ->
      val config = FallbackPropertyConfig.getConfig("tracking-event.${mapper.configInfix}.keep-successes", defaultVal) == "true"
      individualTypeSuccess[mapper.cloudEventType] = config
    }
  }

  @Transactional
  override fun trackEvent(te: TrackedEventResult) {
    val finder = QDbCloudEventLog().id.eq(te.originatingCloudEventMessageId).type.eq(te.originatingCloudEventType)

    // we should delete the successes by default to avoid cluttering the database
    if (te.status == 200 && deleteCloudEventTypes(te.originatingCloudEventType)) {
      finder.delete()
      return
    }

    finder.findOne()?.let { event ->
      val teData = event.trackedEvents
      val newContent = if (teData == null) {
        listOf(te)
      } else {
        parseData(teData).apply {
          add(te)
        }
      }

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
    DbCloudEventLog(messageId, cloudEventType, linkType.name, linkId, mapper.writeValueAsString(data), metadata).save()
  }

  private fun parseData(resultData: String): MutableList<TrackedEventResult> {
    return mapper.readValue(resultData, teRef).toMutableList()
  }

  private fun deleteCloudEventTypes(originatingCloudEventType: String): Boolean {
    return individualTypeSuccess[originatingCloudEventType] ?: defaultKeepSuccesses
  }
}
