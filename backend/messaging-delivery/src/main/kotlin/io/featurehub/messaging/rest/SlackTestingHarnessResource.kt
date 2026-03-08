package io.featurehub.messaging.rest

import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.messaging.utils.TestingSlackMessage
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Path("/mr-api")
class SlackTestingHarnessResource @Inject constructor(private val publisherRegistry: CloudEventPublisherRegistry) {
  @POST
  @Path("/testing/slack-testing")
  @Consumes("application/json")
  @Produces("application/json")
  public fun updateSlackForTesting(update: Map<String,String>): Map<String,String>  {
    val testingMessage = TestingSlackMessage()

    testingMessage.let{ message ->
      message.newTargetUrl = update.get("slack-url")
    }

    // we publish the update because the SlackWebClient is not registered as a bind target, it is purely a listener for messages
    publisherRegistry.publish(testingMessage)

    return mapOf<String, String>()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(SlackTestingHarnessResource::class.java)
  }
}
