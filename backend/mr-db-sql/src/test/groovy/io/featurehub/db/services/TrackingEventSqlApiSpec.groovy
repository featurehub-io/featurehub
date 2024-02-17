package io.featurehub.db.services

import cd.connect.app.config.ThreadLocalConfigurationSource
import cd.connect.cloudevents.TaggedCloudEvent
import io.ebean.DB
import io.featurehub.db.api.CloudEventLinkType
import io.featurehub.events.DynamicCloudEventDestination
import io.featurehub.trackedevent.models.TrackedEventMethod
import io.featurehub.trackedevent.models.TrackedEventResult

import java.time.Instant
import java.time.ZoneOffset

class TrackingEventSqlApiSpec extends Base2Spec {
  Conversions conversions
  TrackingEventSqlApi api
  DynamicCloudEventDestination dynamicListener

  static class Phred implements TaggedCloudEvent {
    String val
  }

  def setup() {
    conversions = Mock()
    dynamicListener = Mock()
    api = new TrackingEventSqlApi(conversions)
  }

  def "i can record a newly sent message and then track events against it"() {
    when:
      ThreadLocalConfigurationSource.createContext(['tracking-event.phred.keep-successes': 'false'])
      api.registerTrackingConfig([dynamicListener])
      def ceType = "integration/slack"
    then:
      1 * dynamicListener.configInfix >> 'phred'
      1 * dynamicListener.cloudEventType >> "integration/slack"
    when: "i create the first record"
      UUID messageId = UUID.randomUUID()
      UUID envId = UUID.randomUUID()
      def whenSent = Instant.now().atOffset(ZoneOffset.UTC)
      api.createInitialRecord(messageId, ceType, CloudEventLinkType.env, envId, new Phred(val: "hello"), whenSent, null)
    then:
      1 * conversions.dbOrganization() >> findOrganization()
    when: "i record a success against the message it will be saved"
      api.trackEvent(new TrackedEventResult()
        .method(TrackedEventMethod.ASYNC)
        .content("content1").status(200)
        .incomingHeaders(["hail": "mary"])
        .originatingCloudEventMessageId(messageId)
        .originatingCloudEventType(ceType)
        .originatingOrganisationId(org.id), Instant.now())
      DB.currentTransaction().commitAndContinue()
      def pages = api.findEvents("env", envId, ceType, 0, 20, true)
    then:
      pages.count == 1
//      pages.items[0].whenSent == whenSent
      pages.items[0].eventResponses.first().status == 200
      pages.items[0].eventResponses[0].headers == ['hail': 'mary']
    when: "i record another success against the same record, i can still find this record"
      api.trackEvent(new TrackedEventResult()
        .method(TrackedEventMethod.ASYNC)
        .content("content1").status(200)
        .incomingHeaders(["mail": "hairy"])
        .originatingCloudEventMessageId(messageId)
        .originatingCloudEventType(ceType)
        .originatingOrganisationId(org.id), Instant.now())
      DB.currentTransaction().commitAndContinue()
      pages = api.findEvents("env", envId, ceType, 0, 20, true)
    then:
      pages.count == 1
      pages.items[0].eventResponses.first().status == 200
    when: "create another event"
      Thread.sleep(100) // this one is defintely later
      UUID message2Id = UUID.randomUUID()
      whenSent = Instant.now().atOffset(ZoneOffset.UTC)
      api.createInitialRecord(message2Id, ceType, CloudEventLinkType.env, envId, new Phred(val: "hello"), whenSent, null)
      DB.currentTransaction().commitAndContinue()
      pages = api.findEvents("env", envId, ceType, 0, 20, true)
    then: "we have 3 records but the 1st one has no response"
      1 * conversions.dbOrganization() >> findOrganization()
      pages.count == 2

      pages.items[0].eventResponses == null
    when: "we send a track event that is a success, it will delete the message rather than recording its result"
      api.trackEvent(new TrackedEventResult()
        .method(TrackedEventMethod.POST)
        .content("content13").status(200)
        .incomingHeaders(["zoom": "bah"])
        .originatingCloudEventMessageId(message2Id)
        .originatingCloudEventType(ceType)
        .originatingOrganisationId(org.id), Instant.now())
      DB.currentTransaction().commitAndContinue()
      pages = api.findEvents("env", envId, ceType, 0, 20, true)
      def unflattenedPages = api.findEvents("env", envId, ceType, 0, 20, false)
    then:
      pages.count == 1
      pages.items.first().eventResponses.size() == 1
      pages.items[0].eventResponses[0].status == 200
      pages.items[0].eventResponses[0].headers == ['hail': 'mary'] // we get the 1st one
      unflattenedPages.count == 1
      unflattenedPages.items.size() == 1
      unflattenedPages.items.first().eventResponses.size() == 2
      unflattenedPages.items.first().eventResponses[0].status == 200
      unflattenedPages.items.first().eventResponses[0].headers == ['hail': 'mary']
      unflattenedPages.items.first().eventResponses[1].status == 200
      unflattenedPages.items.first().eventResponses[1].headers == ['mail': 'hairy']
    when: "we create another event (this will fail)"
      Thread.sleep(100) // this one is defintely later
      UUID message3Id = UUID.randomUUID()
      whenSent = Instant.now().atOffset(ZoneOffset.UTC)
      api.createInitialRecord(message3Id, ceType, CloudEventLinkType.env, envId, new Phred(val: "hello"), whenSent, null)
      DB.currentTransaction().commitAndContinue()
    then: "we have 3 records but the 1st one has no response"
      1 * conversions.dbOrganization() >> findOrganization()
    when: "we send a track event that is failure, it will get recorded and be the first item returned"
      api.trackEvent(new TrackedEventResult()
        .method(TrackedEventMethod.POST)
        .content("content13").status(418)
        .incomingHeaders(["zoom": "bahhhhh"])
        .originatingCloudEventMessageId(message3Id)
        .originatingCloudEventType(ceType)
        .originatingOrganisationId(org.id), Instant.now())
      DB.currentTransaction().commitAndContinue()
      pages = api.findEvents("env", envId, ceType, 0, 20, true)
    then:
      pages.count == 2
      pages.items[0].eventResponses.first().status == 418
      pages.items[1].eventResponses.first().status == 200
  }

  // not tested - searching for cloud event type with no data in environment that is or isn't there
  //
}
