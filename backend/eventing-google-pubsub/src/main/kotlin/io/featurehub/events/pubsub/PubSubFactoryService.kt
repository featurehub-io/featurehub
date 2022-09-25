package io.featurehub.events.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.*
import com.google.protobuf.Duration
import com.google.pubsub.v1.*
import io.cloudevents.CloudEvent
import io.featurehub.health.HealthSource
import io.grpc.ManagedChannelBuilder
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface PubSubFactory {
  fun makePublisher(topicName: String): PubSubPublisher

  fun makeSubscriber(subscriber: String, message: (msg: CloudEvent) -> Boolean): PubSubSubscriber
  fun makeSubscriber(subscriber: String, message: MessageReceiver): PubSubSubscriber

  fun getTopics(): List<String>
  fun getGoogleProjectId(): String

  fun makeUniqueSubscriber(topicName: String, subscriptionPrefix: String, message: (msg: CloudEvent) -> Boolean): PubSubSubscriber
}

interface PubSubLocalEnricher {
  fun enrichPublisherClient(publisherBuilder: Publisher.Builder)
  fun enrichSubscriberClient(subscriptionBuilder: Subscriber.Builder)
}

class PubSubFactoryService  : PubSubFactory, PubSubLocalEnricher, HealthSource {
  private val log: Logger = LoggerFactory.getLogger(PubSubFactoryService::class.java)

  @ConfigKey("cloudevents.pubsub.local.host")
  var pubsubHost: String = ""

  @ConfigKey("cloudevents.pubsub.project")
  var projectId: String = ""

  @ConfigKey("cloudevents.pubsub.local.topics")
  var pubSubTopics: List<String> = arrayListOf()

  // this is used to dynamically create topics/subscriptions when running locally
  @ConfigKey("cloudevents.pubsub.local.sub-pairs")
  var pairs: MutableMap<String, String> = mutableMapOf()

  @ConfigKey("cloudevents.pubsub.min-backoff-delay-seconds")
  var minBackoffDelayInSeconds: Int? = null

  private val topicAdminClient: TopicAdminClient
  private val subscriptionAdminClient: SubscriptionAdminClient
  private val channelProvider: FixedTransportChannelProvider?
  private var credsProvider: CredentialsProvider? = null
  private val knownSubscribers = mutableListOf<PubSubSubscriber>()
  private var unknownSubscribers = mutableListOf<PubSubSubscriber>()
  private val dynamicSubscriber = mutableListOf<String>()
  private val publisherCache = mutableMapOf<String, PubSubPublisher>()

  init {
    DeclaredConfigResolver.resolve(this)

    if (projectId.isEmpty()) {
      log.error("You must specify a project name for pubsub - cloudevents.pubsub.project")
      throw RuntimeException("No google project name")
    }

    if (pubsubHost.isNotEmpty()) {
      log.info("Google Pub/Sub is hosted locally, attempting to connect to {}", pubsubHost)

      val channel = ManagedChannelBuilder.forTarget(pubsubHost).usePlaintext().build()

      channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
      credsProvider = NoCredentialsProvider.create()

      topicAdminClient = TopicAdminClient.create(
        TopicAdminSettings.newBuilder()
          .setTransportChannelProvider(channelProvider)
          .setCredentialsProvider(credsProvider)
          .build()
      )

      subscriptionAdminClient = SubscriptionAdminClient.create(
        SubscriptionAdminSettings.newBuilder()
          .setTransportChannelProvider(channelProvider)
          .setCredentialsProvider(credsProvider)
          .build()
      )

      createTopicSubscriberPairs()
    } else {
      topicAdminClient = TopicAdminClient.create()
      subscriptionAdminClient = SubscriptionAdminClient.create()
      channelProvider = null
    }

    // we only start when everything is wired up and ready to go
    if (ApplicationLifecycleManager.isReady()) {
      startSubscribers()
    } else {
      // we only start when everything is wired up and ready to go
      ApplicationLifecycleManager.registerListener { trans ->
        if (trans.next == LifecycleStatus.STARTED) {
          startSubscribers()
        }
      }
    }

    // register so when the system shuts down we stop listening
    ApplicationLifecycleManager.registerListener { trans ->
      if (trans.next == LifecycleStatus.TERMINATING) {
        deleteDynamicSubscribers()
        stopSubscribers()
      }
    }
  }

  private fun deleteDynamicSubscribers() {
    log.info("pubsub: deleting dynamic subscribers before shutdown")
    dynamicSubscriber.forEach {
      deleteSubscription(it)
    }
  }

  private fun stopSubscribers() {
    knownSubscribers.toMutableList().forEach { sub -> sub.stop() }
  }

  private fun startSubscribers() {
    if (unknownSubscribers.isNotEmpty()) {
      val existingSubs = unknownSubscribers;
      unknownSubscribers = mutableListOf()
      log.info("starting subscribers {}", existingSubs.size)
      existingSubs.toMutableList().forEach { sub -> sub.start() }
      knownSubscribers.addAll(existingSubs)
    }
  }

  override fun getTopics(): List<String> {
    val prefixLen = ProjectTopicName.of(projectId, "x").toString().length - 1
    return topicAdminClient.listTopics(ProjectName.of(projectId)).iterateAll()
      .map { it.name.substring(prefixLen) }
      .toList()
  }

  override fun getGoogleProjectId(): String = this.projectId

  private fun createTopicSubscriberPairs() {
    log.debug("Ensuring the following topics exist {}", pubSubTopics)
    val topics = topicAdminClient.listTopics(ProjectName.of(projectId)).iterateAll().toList()
    val desiredTopics =
      pubSubTopics.map { TopicName.of(projectId, it).toString() }
        .filter { wantedTopic -> topics.find { existingTopic -> existingTopic.name == wantedTopic  } == null }

    desiredTopics.forEach { log.info("pubsub-local: creating topic `{}`", it) }

    desiredTopics.forEach { topicName ->
      log.info("pubsub: creating topic {}", topicName)
      topicAdminClient.createTopic(topicName)
    }

    val pairsTransformed = pairs.entries.associate {
      ProjectSubscriptionName.of(projectId, it.key)!!.toString() to TopicName.of(projectId, it.value)!!
        .toString()
    }.toMutableMap()

    val subscriptions = subscriptionAdminClient.listSubscriptions(ProjectName.of(projectId))

    subscriptions.iterateAll().forEach { sub ->
      log.info("pubsub: found subscription {} for topic {}", sub.name, sub.topic)

      if (pairsTransformed.containsKey(sub.name)) {
        if (!pairsTransformed[sub.name].equals(sub.topic)) {
          log.error(
            "pubsub: subscription {} is subscribed to a different topic ({}) from the one you want! {}",
            sub.name,
            sub.topic,
            pairsTransformed[sub.name]
          )
        } else {
          log.info("pubsub: subscription {} already exists", sub.name)
        }

        pairsTransformed.remove(sub.name)
      }
    }

    pairsTransformed.forEach { (subscription, topic) ->
      makeSubscription(subscription, topic)
    }
  }

  private fun makeSubscription(subscription: String, topic: String): Subscription {
    log.info("pubsub: connecting subscription {} for topic {}", subscription, topic)

    val retryPolicy = RetryPolicy.newBuilder()
      .setMinimumBackoff(Duration.newBuilder().setSeconds(minBackoffDelayInSeconds!!.toLong()).build())
      .build()
    log.info("pubsub: setting retry policy with minimum backoff duration of $minBackoffDelayInSeconds seconds")

    val subscriptionRequest: Subscription = Subscription.newBuilder()
      .setName(subscription)
      .setTopic(topic)
      .setPushConfig(PushConfig.getDefaultInstance())
      .setRetryPolicy(retryPolicy)
      .setAckDeadlineSeconds(20)
      .build()

    return subscriptionAdminClient.createSubscription(subscriptionRequest)
  }

  private fun deleteSubscription(subscription: String) {
    log.info("pubsub: deleting unique subscription")
    subscriptionAdminClient.deleteSubscription(ProjectSubscriptionName.of(projectId, subscription).toString())
  }

  override fun makePublisher(topicName: String): PubSubPublisher {
    return publisherCache.computeIfAbsent(topicName) { PubSubPublisherImpl(projectId, topicName, this) }
  }

  override fun makeSubscriber(subscriber: String, message: (msg: CloudEvent) -> Boolean): PubSubSubscriber {
    return makeSubscriber(subscriber, PubSubscriberMessageReceiver(message))
  }

  override fun makeSubscriber(subscriber: String, message: MessageReceiver): PubSubSubscriber {
    val sub = PubSubSubscriberImpl(projectId, subscriber, message, this)

    // for health checks, we make sure on health checks that we are subscribed and aren't healthy until we are
    unknownSubscribers.add(sub)

    // in case we get this registered after the process starts, we just start it straight away
    // as we assume all services that should be registered are registered
    if (ApplicationLifecycleManager.isAlive()) {
      startSubscribers()
    }

    return sub
  }

  override fun makeUniqueSubscriber(topicName: String, subscriptionPrefix: String, message: (msg: CloudEvent) -> Boolean): PubSubSubscriber {
    val subscriptionName =  subscriptionPrefix + "-" + RandomStringUtils.randomAlphabetic(12).lowercase()
    log.info("pubsub: making dynamic subscription for {} to topic {}", subscriptionName, topicName)
    makeSubscription(ProjectSubscriptionName.of(projectId, subscriptionName).toString(),
      ProjectTopicName.of(projectId, topicName).toString())
    if (dynamicSubscriber.isEmpty()) {
      Runtime.getRuntime().addShutdownHook(object: Thread() {
        override fun run() {
          deleteDynamicSubscribers()
        }
      })
    }
    dynamicSubscriber.add(subscriptionName)
    return makeSubscriber(subscriptionName, message)
  }

  override fun enrichPublisherClient(publisherBuilder: Publisher.Builder) {
    if (credsProvider != null) {
      publisherBuilder.setCredentialsProvider(credsProvider)
    }

    if (channelProvider != null)
      publisherBuilder.setChannelProvider(channelProvider)
  }

  override fun enrichSubscriberClient(subscriptionBuilder: Subscriber.Builder) {
    if (credsProvider != null) {
      subscriptionBuilder.setCredentialsProvider(credsProvider)
    }

    if (channelProvider != null)
      subscriptionBuilder.setChannelProvider(channelProvider)
  }

  override val healthy: Boolean
    get() {
      log.trace("listening subscriber count {}", knownSubscribers.size)

      for (sub in knownSubscribers) {
        if (!sub.isActive()) {
          log.warn("PubSub Subscriber {} is not ready", sub.subscriptionName())
          return false
        }
      }

      return true
    }

  override val sourceName: String
    get() = "pubsub-subscribers"
}
