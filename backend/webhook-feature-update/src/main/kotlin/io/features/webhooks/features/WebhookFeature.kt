package io.features.webhooks.features

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.jersey.common.LoggingConfiguration
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.enriched.model.EnrichedFeatures
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.jersey.config.CommonConfiguration
import io.featurehub.lifecycle.BaggageChecker
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.utils.FallbackPropertyConfig
import io.featurehub.webhook.events.WebhookEnvironmentResult
import io.featurehub.webhook.events.WebhookMethod
import jakarta.inject.Inject
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import jakarta.ws.rs.core.MediaType
import org.glassfish.jersey.client.ClientProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime

class WebhookFeature : Feature {
  private val log: Logger = LoggerFactory.getLogger(WebhookFeature::class.java)

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun configure(context: FeatureContext): Boolean {
    if (enabled) {
      log.info("webhooks: registering for outbound feature webhook processing")
//      context.register(EnricherListenerFeature::class.java)

      LifecycleListeners.starter(WebhookEnricherListener::class.java, context)
    }

    return true
  }

  companion object {
    val enabled: Boolean = FallbackPropertyConfig.getConfig("webhooks.features.enabled")?.lowercase() != "false"

  }
}

@LifecyclePriority(priority = 12)
class WebhookEnricherListener @Inject constructor(
  cloudEventReceiverRegistry: CloudEventReceiverRegistry,
  private val cloudEventPublisher: CloudEventPublisherRegistry,
  private val baggageSource: BaggageChecker,
  private val encryptionService: WebhookEncryptionService
  ) : LifecycleListener {
  private val client: Client

  @ConfigKey("webhooks.features.timeout.connect")
  var connectTimeout: Int? = 4000

  @ConfigKey("webhooks.features.timeout.read")
  var readTimeout: Int? = 4000

  @ConfigKey("webhooks.features.cloudevent-source")
  var cloudEventSource: String? = "https://featurehub.io"

  private val replySource = URI.create(SOURCE_SYSTEM)

  private val log: Logger = LoggerFactory.getLogger(WebhookEnricherListener::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    log.debug("listening for webhooks on enriched feature channel")
    cloudEventReceiverRegistry.listen(EnrichedFeatures::class.java, this::process)

    client = ClientBuilder.newClient()
      .register(CommonConfiguration::class.java)
      .register(LoggingConfiguration::class.java)

    client.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout)
    client.property(ClientProperties.READ_TIMEOUT, readTimeout)
  }

  // TODO update this to get webhook data from webhookEnvironmentInfo instead
  // Handle header values coming in as split entries instead of "key=value" string
  /**
   * webhook.features.enabled: “true”
   * webhook.features.encrypt:”webhook.features.endpoint, webhook.features.headers.authorisation, webhook.features.headers.xfoof”
   * webhook.features.endpoint: “encrypted”
   * webhook.features.endpoint.encrypted: “sdfjasdk”
   * webhook.features.endpoint.salt: “3423”
   * webhook.features.headers.authorisation: "updated”
   * webhook.features.headers.authorisation,encrypted=“xxxx”
   * webhook.features.headers.authorisation.salt=“sdfsd”
   * webhook.features.headers.xfoof=“blah”
   *
   */

  /**
   * expects a fully decrypted set of data
   */
  private fun extractHeaders(conf: Map<String, String>): Map<String, String> {
    val len = WEBHOOK_HEADERS.length
    return conf.filter { it.key.startsWith(WEBHOOK_HEADERS) }.mapKeys { it.key.substring(len) }
  }

  private fun removeEncryptedHeaders(originalWebhookEnvironment: Map<String, String>, postedHeaders: Map<String, String>): Map<String, String> {
    // these are the keys that were encrypted, select out only those that are headers
    val keys = encryptionService.getAllKeysEnabledForEncryption(originalWebhookEnvironment)
      .filter { it.startsWith(WEBHOOK_HEADERS) }
    // remove any headers that held encrypted data
    return postedHeaders.filter { !keys.contains("${WEBHOOK_HEADERS}${it.key}") }
  }

  private fun checkUrlEncrypted(originalWebhookEnvironment: Map<String, String>, url: String): String {
    return if (encryptionService.getAllKeysEnabledForEncryption(originalWebhookEnvironment).contains(WEBHOOK_ENDPOINT)) {
      "[encrypted-url]"
    } else {
      url
    }
  }

  fun process(ef: EnrichedFeatures, ce: CloudEvent) {
    log.debug("enriched: checking for environment info (will exit if null)")
    val originalConf = ef.environment.environment.webhookEnvironment ?: return

    log.trace("webhook: environment info is {}", originalConf)

    if ((ef.targetEnrichmentDestination != null && ef.targetEnrichmentDestination != WebhookEnvironmentResult.CLOUD_EVENT_TYPE )) {
      return
    }

    if (originalConf[WEBHOOK_ENABLED] == "true") {
      val conf = encryptionService.decryptAndStripEncrypted(originalConf)
      val endpoint = conf[WEBHOOK_ENDPOINT] ?: return
      val headers = extractHeaders(conf)

      // strip out management headers
      ef.environment.environment.environmentInfo = ef.environment.environment.environmentInfo.filter { !it.key.startsWith("mgmt.") }.toMap()
      // don't want anything here
      ef.environment.environment.webhookEnvironment = null

      try {
        val target = client.target(endpoint).request()

        val outboundHeaders = mutableMapOf<String, String>()

        headers.forEach { header ->
          val key = header.key

          val value = header.value
          outboundHeaders[key] = value
          target.header(key, value)
        }

        // no bindings for jakarta yet (in 2.4.0)
        target.header("ce-specversion", "1.0")
        target.header("ce-type", EnrichedFeatures.CLOUD_EVENT_TYPE)
        target.header("ce-source", cloudEventSource)
        target.header("ce-subject", EnrichedFeatures.CLOUD_EVENT_SUBJECT)
        val time = OffsetDateTime.now()
        target.header("ce-time", ce.time ?: time)
        target.header("ce-id", ce.id)

        ce.extensionNames.forEach {
          target.header("ce-${it}", ce.getExtension(it))
        }

        val baggage = baggageSource.asMap().entries.joinToString(",") {
          "${it.key}=${
            URLEncoder.encode(
              it.value.value,
              StandardCharsets.UTF_8
            )
          }"}

        if (baggage.isNotEmpty()) {
          target.header("baggage", baggage)
        }

//        val checksum = Integer.toHexString(ef.environment.featureValues.joinToString(",") { fv ->
//          if (fv.value == null) {
//            fv.feature.id.toString()
//          } else {
//            fv.value!!.id.toString() + "-" + fv.value!!.version
//          }
//        }.hashCode())
//
//        val id = "${ef.environment.environment.id}-${checksum}"
//        target.header("ce-id", id)

        val notifyEvent = CloudEventBuilder().newBuilder()
          .withType(WebhookEnvironmentResult.CLOUD_EVENT_TYPE)
          .withSubject(WebhookEnvironmentResult.CLOUD_EVENT_SUBJECT)
          .withSource(replySource)
          .withId(ce.id)
          .withTime(time)

        val data = WebhookEnvironmentResult()
          .environmentId(ef.environment.environment.id)
          .organisationId(ef.environment.organizationId)
          .method(WebhookMethod.POST)
          .sourceSystem(SOURCE_SYSTEM)
          .cloudEventType(EnrichedFeatures.CLOUD_EVENT_TYPE)
          .whenSent(time)
          .url(endpoint)
          .outboundHeaders(outboundHeaders)
          .content(CacheJsonMapper.mapper.writeValueAsString(ef))

        try {
          val response = target.post(Entity.entity(ef, MediaType.APPLICATION_JSON))

          data.status(response.status).result(response.readEntity(String::class.java)?.take(1000))
          data.incomingHeaders(mutableMapOf())
          response.stringHeaders.forEach { (k, v) ->
            data.incomingHeaders?.put(k, v.joinToString(";"))
          }
        } catch (e: Exception) {
          data.status(0).result(e.message?.take(1000))
        }

        // strip out encrypted headers
        data.outboundHeaders = removeEncryptedHeaders(originalConf, data.outboundHeaders)
        data.url = checkUrlEncrypted(originalConf, data.url)
        cloudEventPublisher.publish(WebhookEnvironmentResult.CLOUD_EVENT_TYPE, data, notifyEvent)
      } catch (e: Exception) {
        log.debug("Failed to construct webhook", e)
      }
    }
  }

  companion object {
    const val WEBHOOK_ENABLED = "webhook.features.enabled"
    const val WEBHOOK_ENDPOINT = "webhook.features.endpoint"
    const val WEBHOOK_HEADERS = "webhook.features.headers." // prefix for headers

    const val SOURCE_SYSTEM = "/webhook-features"
  }
}
