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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface PubSubFactory {
  fun makePublisher(topicName: String): PubSubPublisher

  fun makeSubscriber(subscriber: String, message: (msg: CloudEvent) -> Boolean): PubSubSubscriber
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

  @ConfigKey("pubsub.min-backoff-delay-seconds")
  var minBackoffDelayInSeconds: Int? = null

  private val topicAdminClient: TopicAdminClient?
  private val subscriptionAdminClient: SubscriptionAdminClient?
  private val channelProvider: FixedTransportChannelProvider?
  private var credsProvider: CredentialsProvider? = null
  private val knownSubscribers = mutableListOf<PubSubSubscriber>()
  private var unknownSubscribers = mutableListOf<PubSubSubscriber>()

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
      topicAdminClient = null
      subscriptionAdminClient = null
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
        stopSubscribers()
      }
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

  private fun createTopicSubscriberPairs() {
    log.debug("Ensuring the following topics exist {}", pubSubTopics)
    val topics = topicAdminClient!!.listTopics(ProjectName.of(projectId))
    val desiredTopics = pubSubTopics.map { TopicName.of(projectId, it)!!.toString() }.toMutableList()

    topics.iterateAll().forEach { topic ->
      if (desiredTopics.remove(topic.name) != null) {
        log.info("pubsub: topic {} already exists", topic.name)
      } else {
        log.info("pubsub: topic {} found", topic.name)
      }
    }

    desiredTopics.forEach { topic ->
      log.info("pubsub: creating topic {}", topic)
      topicAdminClient.createTopic(topic)
    }

    val pairsTransformed = pairs.entries.associate {
      ProjectSubscriptionName.of(projectId, it.key)!!.toString() to TopicName.of(projectId, it.value)!!
        .toString()
    }.toMutableMap()

    val subscriptions = subscriptionAdminClient!!.listSubscriptions(ProjectName.of(projectId))

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

      subscriptionAdminClient.createSubscription(subscriptionRequest)
    }
  }

  override fun makePublisher(topicName: String): PubSubPublisher {
    return PubSubPublisherImpl(projectId, topicName, this)
  }

  override fun makeSubscriber(subscriber: String, message: (msg: CloudEvent) -> Boolean): PubSubSubscriber {
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
