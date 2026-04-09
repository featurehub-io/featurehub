# Kafka Eventing Implementation Plan

This document captures enough architectural context to plan and implement a `eventing-kafka` module for FeatureHub, following the same pattern as `eventing-nats` and `eventing-kinesis`.

---

## How the Eventing Plugin System Works

### Discovery (boot time)

`CloudEventsFeature.configure()` calls `findEventingLayer()`, which uses `ServiceLoader.load(EventingFeatureSource::class.java)` to find all available transport implementations on the classpath. Each one is asked `enabled` — if true, its `featureSource` class is registered as a Jersey `Feature`.

Every transport therefore requires:

1. A `META-INF/services/io.featurehub.events.EventingFeatureSource` file listing the implementation class.
2. An `EventingFeatureSource` impl that checks a config property to decide if it is active.
3. A JAX-RS `Feature` class that registers DI bindings and lifecycle listeners.

### Configuration wiring (lifecycle start)

When the application starts, the transport's `LifecycleStarted` bean calls `ceConfigDiscovery.discover("<transport-key>", this)`. The `CloudEventConfigDiscoveryService`:

1. Loads every `META-INF/cloud-events/cloud-events.yaml` on the classpath.
2. Deep-merges the `default:` section into the transport-specific section (e.g. `kafka:`).
3. Filters channels and sub/pub entries by `tags` (matched against the running application name) and `conditional` properties.
4. Calls back `CloudEventConfigDiscoveryProcessor.processPublisher()` / `processSubscriber()` for each matching entry.

The transport's `ConfiguredSource` implements `CloudEventConfigDiscoveryProcessor` and, for each callback, creates the actual Kafka producer/consumer.

### Publishing

`CloudEventPublisherRegistry.registerForPublishing(ceType, metric, compress, handler)` is called once per CE type per channel. When application code calls `publisherRegistry.publish(data)`, the registry serialises the event and calls every registered handler (in a thread pool).

### Subscribing

`CloudEventSubscriberConfig.handler` is a lambda `(CloudEvent) -> Unit` baked in by `CloudEventConfigDiscoveryService`. It routes to `CloudEventReceiverRegistry.process(ce)` (or a named sub-registry). The transport just needs to call this lambda when a message arrives.

---

## Broadcast vs Queue in Kafka Terms

| FeatureHub concept | `broadcast: true` | `broadcast: false` |
|--------------------|-------------------|--------------------|
| Who gets the message | Every instance | One instance in the group |
| Kafka equivalent | Unique consumer group per instance | Shared consumer group |
| Implementation | Append `UUID` or hostname to group id | Fixed group id from `subscriber.prefix` |

This mirrors the Kinesis pattern exactly (`"${subscriber.prefix}-${UUID.randomUUID()}"` for broadcast).

---

## Files to Create: `backend/eventing-kafka`

### `pom.xml`

Mirror `eventing-nats/pom.xml`. Key dependencies:

```xml
<dependency>
  <groupId>org.apache.kafka</groupId>
  <artifactId>kafka-clients</artifactId>
  <version>3.7.x</version>  <!-- pin to a stable version -->
</dependency>
<dependency>
  <groupId>io.cloudevents</groupId>
  <artifactId>cloudevents-kafka</artifactId>
  <version>4.x</version>  <!-- CloudEvents Kafka binding -->
</dependency>
<dependency>
  <groupId>io.featurehub.events</groupId>
  <artifactId>eventing-cloudevents</artifactId>
  <version>1.1-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>io.featurehub.common</groupId>
  <artifactId>common-web</artifactId>
  <version>1.1-SNAPSHOT</version>
</dependency>
```

Build tile: `io.featurehub.tiles:tile-java:[1.1,2)` (same as NATS).

---

### `src/main/resources/META-INF/services/io.featurehub.events.EventingFeatureSource`

```
io.featurehub.events.kafka.KafkaEventingFeatureSource
```

---

### Class structure (all in `io.featurehub.events.kafka`)

#### `KafkaEventingFeatureSource.kt`

```kotlin
class KafkaEventingFeatureSource : EventingFeatureSource {
  override val featureSource: Class<out Feature>?
    get() = if (KafkaEventFeature.isEnabled()) KafkaEventFeature::class.java else null
  override val enabled: Boolean
    get() = KafkaEventFeature.isEnabled()
}
```

Enable check when `cloudevents.kafka.bootstrap.servers` is set (similar to how NATS enables when `nats.urls` is present). 

---

#### `KafkaEventFeature.kt`

```kotlin
class KafkaEventFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (!isEnabled()) return false

    context.register(object : AbstractBinder() {
      override fun configure() {
        bind(KafkaFactoryImpl::class.java)
          .to(KafkaFactory::class.java)
          .to(HealthSource::class.java)
          .`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.starter(KafkaDynamicPublisher::class.java, context)  // optional
    LifecycleListeners.wrap(KafkaConfiguredSource::class.java, context)

    return true
  }

  companion object {
    fun isEnabled() =
      FallbackPropertyConfig.getConfig("cloudevents.kafka.bootstrap.servers") != null &&
      FallbackPropertyConfig.getConfig("cloudevents.kafka.enabled") != "false"
  }
}
```

---

#### `KafkaFactory.kt` (interface + impl)

```kotlin
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
```

`KafkaFactoryImpl`:
- Reads config via `FallbackPropertyConfig` (same pattern as Kinesis):
  - `cloudevents.kafka.bootstrap.servers` — required
  - `cloudevents.kafka.security.protocol` — optional (SASL_SSL etc.)
  - `cloudevents.kafka.sasl.mechanism` / `cloudevents.kafka.sasl.jaas.config` — optional
  - `cloudevents.kafka.producer.linger.ms` — optional tuning
  - `cloudevents.kafka.consumer.poll.timeout.ms` — default 1000
- Producer: create one `KafkaProducer<String, CloudEvent>` per topic (cache by topic name). Use the CloudEvents Kafka binding's `CloudEventSerializer`.
- Consumer: each `makeSubscriber` creates a new `KafkaConsumer<String, CloudEvent>`, subscribes to the topic, and runs a polling loop on a daemon thread. Use `CloudEventDeserializer`.
- Implements `HealthSource`: healthy if no consumer has failed.

**Key serialisation note:** The `cloudevents-kafka` library provides:
- `io.cloudevents.kafka.CloudEventSerializer` — for `KafkaProducer` value serializer
- `io.cloudevents.kafka.CloudEventDeserializer` — for `KafkaConsumer` value deserializer

---

#### `KafkaConfiguredSource.kt`

```kotlin
@LifecyclePriority(priority = LifecyclePriority.APPLICATION_PRIORITY_END)
class KafkaConfiguredSource @Inject constructor(
  private val ceConfigDiscovery: CloudEventConfigDiscovery,
  private val kafkaFactory: KafkaFactory
) : LifecycleStarted, LifecycleShutdown, CloudEventConfigDiscoveryProcessor {

  private val listeners = mutableListOf<KafkaListener>()

  override fun started() {
    ceConfigDiscovery.discover("kafka", this)
  }

  override fun processPublisher(publisher: CloudEventPublisherConfig, config: CloudEventConfig) {
    publisher.channelNames.forEach { topic ->
      val pub = kafkaFactory.makePublisher(topic)
      config.registerPublisher(publisher, topic, true) { msg -> pub.publish(msg) }
    }
  }

  override fun processSubscriber(subscriber: CloudEventSubscriberConfig, config: CloudEventConfig) {
    subscriber.channelNames.forEach { topic ->
      val groupId = if (subscriber.broadcast)
        "${subscriber.prefix}-${UUID.randomUUID()}"
      else
        subscriber.prefix

      listeners.add(kafkaFactory.makeSubscriber(groupId, topic, subscriber.handler))
    }
  }

  override fun shutdown() {
    listeners.forEach { it.close() }
    listeners.clear()
  }
}
```

---

#### `KafkaDynamicPublisher.kt` (optional — needed for webhook-style dynamic destinations)

Mirror `NATSDynamicPublisher`. Register prefix `"kafka://"` with `dynamicPublisher.registerDynamicPublisherProvider`.

---

### `cloud-events.yaml` additions

Add a `kafka:` section to `eventing-cloudevents/src/main/resources/META-INF/cloud-events/cloud-events.yaml` (exactly as `nats:` and `kinesis:` are structured):

```yaml
kafka:
  "cache-channel":
    channelName:
      property: "cloudevents.inbound.kafka.mr-features-name"
      default: "featurehub-mr-dacha2"

  "feature-only-channel":
    channelName:
      property: "cloudevents.mr-edge.kafka.topic-name"
      default: "featurehub-mr-edge"
    subscribers:
      dacha2-enricher:
        name: "enricher"

  "mr-notifications-channel":
    channelName:
      property: "cloudevents.edge-mr.kafka.topic-name"
      default: "featurehub-edge-updates"
    subscribers:
      notifications:
        prefixConfig:
          property: "cloudevents.edge-mr.kafka.group-id"
          default: "featurehub-mr-update"

  "edge-stats-emitter":
    channelName:
      property: "cloudevents.stats.kafka.topic-name"
      default: "featurehub-stats"
```

The key difference from NATS: NATS uses `/` in default channel names (`featurehub/mr-dacha2-channel`) because NATS subjects are hierarchical; Kafka topics use `-` separated names.

---

## Configuration Properties Summary

| Property | Default | Notes |
|----------|---------|-------|
| `cloudevents.kafka.enabled` | `true` when bootstrap set | Set `false` to disable |
| `cloudevents.kafka.bootstrap.servers` | (required) | e.g. `localhost:9092` |
| `cloudevents.kafka.security.protocol` | `PLAINTEXT` | |
| `cloudevents.kafka.sasl.mechanism` | — | |
| `cloudevents.kafka.sasl.jaas.config` | — | |
| `cloudevents.kafka.producer.acks` | `1` | |
| `cloudevents.kafka.consumer.poll.timeout.ms` | `1000` | |
| `cloudevents.inbound.kafka.mr-features-name` | `featurehub-mr-dacha2` | cache-channel topic |
| `cloudevents.mr-edge.kafka.topic-name` | `featurehub-mr-edge` | feature-only-channel topic |
| `cloudevents.edge-mr.kafka.topic-name` | `featurehub-edge-updates` | mr-notifications-channel topic |
| `cloudevents.stats.kafka.topic-name` | `featurehub-stats` | edge-stats-emitter topic |
| `cloudevents.edge-mr.kafka.group-id` | `featurehub-mr-update` | consumer group for mr notifications |

---

## Differences from NATS and Kinesis

| Concern | NATS | Kinesis | Kafka |
|---------|------|---------|-------|
| Enable check | `nats.urls` present | `cloudevents.kinesis.enabled=true` | `cloudevents.kafka.bootstrap.servers` present |
| Connection object | `NATSConnectionSource` implements `EventingConnection` | No `EventingConnection` | Implement `EventingConnection` optional (Kafka auto-reconnects) |
| Broadcast | Topic subscriber (no queue group) | UUID-suffixed app name | UUID-suffixed consumer group |
| Queue | Queue subscriber (shared queue group) | Fixed app name | Fixed consumer group |
| Shutdown | `connection.closeDispatcher()` | `scheduler.startGracefulShutdown()` | `consumer.wakeup()` + thread join |
| CE serialisation | `io.cloudevents:cloudevents-nats` | `io.cloudevents:cloudevents-kinesis` | `io.cloudevents:cloudevents-kafka` |
| Health | `NATSHealthSource` checks connection status | `KinesisFactoryImpl` tracks lease loss | Check consumer thread liveness / last-poll time |
| Ordering | Not guaranteed | Per-partition ordering via partition key | Per-partition ordering; use consistent message key for ordered channels |

---

## Integration Into Deployables

The `eventing-kafka` jar only activates if `cloudevents.kafka.bootstrap.servers` is on the classpath/env. It can be added to `party-server` and `mr`/`edge` etc. pom.xml as an optional runtime dependency — exactly as `eventing-nats` is included today — without breaking anything when Kafka is not configured.

---

## Testing Strategy

1. **Unit tests** — Mock `KafkaFactory`, test `KafkaConfiguredSource.processPublisher/processSubscriber` logic (consumer group naming for broadcast vs queue).
2. **Integration tests** — Use `testcontainers-kafka` (or `embedded-kafka`) to spin up a real broker. Mirror the existing pattern in `eventing-nats` tests.
3. **E2E** — The existing `adks/e2e-sdk` tests can run against a deployment configured with Kafka; they are transport-agnostic since they only care about feature state delivery.
