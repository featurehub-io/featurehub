package io.featurehub.events.kafka

import io.cloudevents.CloudEvent
import io.cloudevents.kafka.CloudEventDeserializer
import io.cloudevents.kafka.CloudEventSerializer
import io.featurehub.health.HealthSource
import io.featurehub.utils.FallbackPropertyConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

interface KafkaCloudEventsPublisher {
  fun publish(msg: CloudEvent)
}

interface KafkaListener {
  fun close()
}

interface KafkaFactory {
  fun makePublisher(topic: String): KafkaCloudEventsPublisher
  fun makeSubscriber(
    groupId: String,
    topic: String,
    messageHandler: (CloudEvent) -> Unit
  ): KafkaListener
}

class KafkaFactoryImpl : KafkaFactory, HealthSource {
  private val log: Logger = LoggerFactory.getLogger(KafkaFactoryImpl::class.java)

  private val bootstrapServers: String = FallbackPropertyConfig.getMandatoryConfig("cloudevents.kafka.bootstrap.servers")
  private val securityProtocol: String? = FallbackPropertyConfig.getConfig("cloudevents.kafka.security.protocol")
  private val saslMechanism: String? = FallbackPropertyConfig.getConfig("cloudevents.kafka.sasl.mechanism")
  private val saslJaasConfig: String? = FallbackPropertyConfig.getConfig("cloudevents.kafka.sasl.jaas.config")
  private val producerAcks: String = FallbackPropertyConfig.getConfig("cloudevents.kafka.producer.acks", "1")
  private val producerLingerMs: String = FallbackPropertyConfig.getConfig("cloudevents.kafka.producer.linger.ms", "5")
  private val pollTimeoutMs: Long =
    FallbackPropertyConfig.getConfig("cloudevents.kafka.consumer.poll.timeout.ms", "1000").toLong()

  private val sharedProducer: KafkaProducer<String, CloudEvent> by lazy { createProducer() }
  private val consumers = ConcurrentHashMap<String, ConsumerTracker>()

  private fun baseProperties(): Properties {
    val props = Properties()
    props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
    if (securityProtocol != null) props["security.protocol"] = securityProtocol
    if (saslMechanism != null) props["sasl.mechanism"] = saslMechanism
    if (saslJaasConfig != null) props["sasl.jaas.config"] = saslJaasConfig
    return props
  }

  private fun createProducer(): KafkaProducer<String, CloudEvent> {
    val props = baseProperties()
    props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
    props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = CloudEventSerializer::class.java.name
    props[ProducerConfig.ACKS_CONFIG] = producerAcks
    props[ProducerConfig.LINGER_MS_CONFIG] = producerLingerMs
    log.info("kafka: creating shared producer for {}", bootstrapServers)
    return KafkaProducer(props)
  }

  private fun createConsumerProperties(groupId: String): Properties {
    val props = baseProperties()
    props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = CloudEventDeserializer::class.java.name
    props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
    props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
    props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
    return props
  }

  internal inner class ConsumerTracker(
    val groupId: String,
    val topic: String,
    val handler: (CloudEvent) -> Unit
  ) : KafkaListener {
    private val running = AtomicBoolean(true)
    private val consumer = KafkaConsumer<String, CloudEvent>(createConsumerProperties(groupId))
    @Volatile
    var healthy = true

    init {
      consumer.subscribe(listOf(topic))
      log.info("kafka: consumer group {} subscribing to topic {}", groupId, topic)

      val thread = Thread(::pollLoop, "kafka-consumer-$groupId-$topic")
      thread.isDaemon = true
      thread.start()
    }

    private fun pollLoop() {
      try {
        while (running.get()) {
          try {
            val records = consumer.poll(Duration.ofMillis(pollTimeoutMs))
            for (record in records) {
              val ce = record.value()
              if (ce == null) {
                log.error("kafka: received null CloudEvent on topic {}", topic)
                continue
              }
              log.trace("kafka: received {}/{} on topic {}", ce.type, ce.subject, topic)
              try {
                handler(ce)
              } catch (e: Exception) {
                log.error("kafka: unable to process cloud event on topic {}, skipping", topic, e)
              }
            }
          } catch (e: org.apache.kafka.common.errors.WakeupException) {
            if (running.get()) {
              log.warn("kafka: unexpected wakeup on topic {}, re-polling", topic)
            }
            // if !running, exit the loop
          } catch (e: Exception) {
            log.error("kafka: poll error on topic {}, will retry", topic, e)
            healthy = false
          }
        }
      } finally {
        try {
          consumer.close()
        } catch (e: Exception) {
          log.warn("kafka: error closing consumer for topic {}", topic, e)
        }
        log.info("kafka: consumer group {} unsubscribed from topic {}", groupId, topic)
      }
    }

    override fun close() {
      running.set(false)
      consumer.wakeup()
    }
  }

  override fun makePublisher(topic: String): KafkaCloudEventsPublisher {
    return object : KafkaCloudEventsPublisher {
      override fun publish(msg: CloudEvent) {
        try {
          log.trace("kafka: publishing {}/{} to topic {}", msg.type, msg.subject, topic)
          val record = ProducerRecord<String, CloudEvent>(topic, msg.type, msg)
          sharedProducer.send(record) { metadata, ex ->
            if (ex != null) {
              log.error("kafka: failed to publish to topic {}", topic, ex)
            } else {
              log.trace("kafka: published to topic {} partition {} offset {}", topic, metadata.partition(), metadata.offset())
            }
          }
        } catch (e: Exception) {
          log.error("kafka: unable to publish cloud event to topic {}", topic, e)
        }
      }
    }
  }

  override fun makeSubscriber(groupId: String, topic: String, messageHandler: (CloudEvent) -> Unit): KafkaListener {
    val tracker = ConsumerTracker(groupId, topic, messageHandler)
    consumers["$groupId:$topic"] = tracker
    return tracker
  }

  override val healthy: Boolean
    get() = consumers.values.all { it.healthy }

  override val sourceName: String
    get() = "kafka-factory"
}
