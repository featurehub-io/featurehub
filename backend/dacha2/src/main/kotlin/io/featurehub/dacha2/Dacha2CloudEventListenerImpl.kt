package io.featurehub.dacha2

import io.cloudevents.CloudEvent
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.jersey.config.CacheJsonMapper
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Dacha2CloudEventListener {
  fun process(event: CloudEvent)
}

class Dacha2CloudEventListenerImpl @Inject constructor(private val dacha2Cache: Dacha2Cache) : Dacha2CloudEventListener {
  private val log: Logger = LoggerFactory.getLogger(Dacha2CloudEventListenerImpl::class.java)

  override fun process(event: CloudEvent) {
    log.debug("processing cloud event {}: {}", event.subject, event.type)
    when (event.subject) {
      PublishEnvironment.CLOUD_EVENT_SUBJECT -> processEnvironment(event)
      PublishServiceAccount.CLOUD_EVENT_SUBJECT -> processServiceAccount(event)
      PublishFeatureValues.CLOUD_EVENT_SUBJECT -> processFeature(event)
      else ->
        log.warn("dacha2 received unknown event {}", event.toString())
    }
  }

  private fun processFeature(event: CloudEvent) {
    when (event.type) {
      PublishFeatureValues.CLOUD_EVENT_TYPE ->
        CacheJsonMapper.fromEventData(event, PublishFeatureValues::class.java)?.let {
          log.trace("received feature values {}", it)
          for(feature in it.features) {
            dacha2Cache.updateFeature(feature)
          }
        } ?: log.error("Unable to decode event {}", event)
      else ->
        log.info("Unknown feature update format ignored {}", event.type)
    }
  }

  private fun processServiceAccount(event: CloudEvent) {
    when (event.type) {
      PublishServiceAccount.CLOUD_EVENT_TYPE ->
        CacheJsonMapper.fromEventData(event, PublishServiceAccount::class.java)?.let {
          log.trace("received service account update {}", it)
          dacha2Cache.updateServiceAccount(it)
        } ?: log.error("Unable to decode event {}", event)
      else ->
        log.info("Unknown service account update format ignored {}", event.type)
    }
  }

  private fun processEnvironment(event: CloudEvent) {
    when (event.type) {
      PublishEnvironment.CLOUD_EVENT_TYPE ->
        CacheJsonMapper.fromEventData(event, PublishEnvironment::class.java)?.let {
          log.trace("received environment {}", it)
          dacha2Cache.updateEnvironment(it)
        } ?: log.error("Unable to decode event {}", event)
      else ->
        log.info("Unknown environment update format ignored {}", event.type)
    }
  }
}
