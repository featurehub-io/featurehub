# Party Server without embedded NATS

Status: plan
Scope: open-source FeatureHub only

## 1. Goal

Ship the `party-server` Docker image **without a NATS server inside it**.

The party-server runs MR, Dacha2 and Edge in one JVM, so it doesn't need a network message bus to talk to itself.

- **No external streaming configured**: route cloud events **in-process**. Assume this is the **only** party-server
  instance using the database.
- **External streaming configured** (NATS, Kafka, Google Pub/Sub, Kinesis): use it exactly as today. Several
  party-servers can then share one database, as they can now with an external NATS.

Non-goals: changing the separate `mr` / `dacha2` / `edge` deployments, changing `party-server-ish`, or changing the
cloud event channel model.

## 2. How it works today

### 2.1 The image and process

- `backend/party-server/pom.xml` sets `app.baseimage` to `featurehub/base_party:2.2`. That image
  (`pipeline/build/base_party/Dockerfile`) is `eclipse-temurin:25-jre-alpine` plus the `nats-server` binary at
  `/target/nats-server` and its config. It also creates `/db`, `/target`, `/etc/app-config`, `/etc/common-config` and
  **chowns them to UID 999**.
- `BatheInitializerNatsServer` (registered in `META-INF/services/bathe.BatheInitializer`) runs before the app starts. It
  spawns `/target/nats-server` as a child process if the file exists, and kills it on shutdown.
  - Bug: `dont-run-nats-server` only *logs* that it won't run NATS. It doesn't return, so NATS is started anyway.
- `src/main/resources/deploy-defaults/common.properties` sets `nats.urls=nats://localhost:4222`. That is what makes
  `NATSFeature.isNatsConfigured()` true, so `NATSEventingFeatureSource` registers NATS as the eventing platform.

### 2.2 How events flow in the party-server

`Application.main` sets `dacha1.enabled=false`, `dacha2.enabled=true` and the application name `party-server`, and
adds the tags `dacha2`, `edge-dacha2`, `mr`, `mr-dacha2`. Against
`eventing-cloudevents/src/main/resources/META-INF/cloud-events/cloud-events.yaml`, that gives:

| Channel | Published here by | Subscribed here by | What it carries |
|---------|------------------|--------------------|-----------------|
| `cache-channel` | `cache` (tag `mr-dacha2`) | `dacha2` (tag `dacha2`, registry `common`) | `publish-feature-v1`, `publish-environment-v1`, `publish-serviceaccount-v1` |
| `feature-only-channel` | `party-enricher` (`enricher-ping-v1` only); `mr-features` is excluded with `!party-server` | `edge-features` (`edge-dacha2`, `common`); `dacha2-enricher` (`dacha2`, registry `enricher`) | features to Edge and enricher pings to Dacha2 |
| `mr-notifications-channel` | `notifications` (tag `edge-dacha2`) | `notifications` (tag `mr`) | `feature-update-v1` (Edge PUT), webhook and tracked-event results back to MR |
| `enriched-environment-channel` (only if `webhooks.features.enabled`) | `enricher` (tag `dacha2`) | `enricher` (tag `edge-dacha2`) | enriched features for webhooks |

So the party-server already publishes to and subscribes from the *same* channels: every event leaves the process, goes
through NATS, and comes back. A feature change published once on `cache-channel` arrives in the `common` registry, where
**both** the Dacha2 and Edge listeners pick it up. That is why `mr-features` excludes `party-server`; without the
exclusion, Edge would get each feature twice.

### 2.3 What breaks if you just remove NATS

- **No cache updates.** With no enabled `EventingFeatureSource`, `CloudEventsFeature.publishingEnabled()` is false.
  `mr-eventing/.../EventingFeature.kt` then binds `DummyPublisher` as `CacheSource` / `CacheRefresherApi` instead of
  `CloudEventCacheBroadcaster`, so MR never broadcasts feature, environment or service account changes. Dacha2 and Edge
  would serve stale data forever.
- **Accidental routing only.** `CloudEventPublisherRegistryProcessor.publishFullyFormed` already falls back to delivering
  directly into the `common` receiver registry when a type has no publishing handlers. That fallback only covers the
  `common` registry; it misses `enricher`, so pings log "Attempting to publish event with no destination". It also
  bypasses the per-channel `cloudEventsInclude` filters in `cloud-events.yaml`. It isn't a sound basis for this.
- **Dacha2 can't start.** Dacha2's default `dacha2.streaming.disconnect-behaviour=on-reconnect` binds
  `Dacha2NewDumpOnReconnectCache`, which injects `EventingConnection`. Only the NATS, Kafka and Pub/Sub features bind
  `EventingConnection`. With none of them, HK2 fails to start Dacha2.
- **NATS connection failure.** `nats.urls` in the baked-in `common.properties` would still point at
  `localhost:4222`. `NATSConnectionSource` throws if it can't connect, so the server fails at startup.
- **H2 can't write.** `featurehub/base_jdk:2.2`, the default image from `tile-app-with-ui`, creates `/db` but does
  **not** chown it to UID 999. The party-server runs as `999:999` and writes H2 to `/db`, so a plain base-image swap
  breaks evaluation installs.

`party-server-ish` gets away without NATS only because it has no Dacha2 and no streaming Edge (`EdgeGetFeature`, REST
only, reading MR directly).

## 3. Design: an in-process ("local") eventing platform

Add a fifth eventing platform alongside NATS, Kafka, Pub/Sub and Kinesis. It is a loopback that delivers each published
event straight to the in-process subscribers of the same channel. Because it is driven by the same `cloud-events.yaml`
discovery and tags, the routing is identical to the NATS round trip in 2.2. MR, Dacha2 and Edge code doesn't change.

### 3.1 Where it lives

All in `backend/eventing-cloudevents` (no new dependencies):

- `LocalEventingFeature`: a JAX-RS `Feature` that
  - binds `DefaultEventingConnection` to `EventingConnection` (always `CONNECTED`, so Dacha2's dump-on-reconnect cache
    never dumps);
  - registers `LocalConfiguredSource` as a `LifecycleStarted` listener, at the same priority as `NATSConfiguredSource`
    (`APPLICATION_PRIORITY_END`).
- `LocalConfiguredSource : CloudEventConfigDiscoveryProcessor`. On `started()` it calls
  `ceConfigDiscovery.discover("local", this)`.
- `cloud-events.yaml`: add a top-level `local: {}` section. It must be an empty **map**, not a bare `local:`.
  `recode()` returns nothing for a null section, but an empty map gets the `default:` channels merged in. No channel
  names are needed.

### 3.2 Routing

`discover()` processes all publishers first, then all subscribers, so `LocalConfiguredSource` resolves subscribers when
an event is published, not at registration:

```kotlin
private val subscribersByChannel = ConcurrentHashMap<String, CopyOnWriteArrayList<LocalSubscriber>>()

override fun processPublisher(publisher: CloudEventPublisherConfig, config: CloudEventConfig) {
  // logical channel name (e.g. "cache-channel"); transport channel names are irrelevant in-process
  config.registerPublisher(publisher, config.name, false) { ce ->
    val subs = subscribersByChannel[config.name]
    if (subs.isNullOrEmpty()) log.debug("local: no in-process subscriber for {} on {}", ce.type, config.name)
    else subs.forEach { it.deliver(ce) }
  }
}

override fun processSubscriber(subscriber: CloudEventSubscriberConfig, config: CloudEventConfig) {
  subscribersByChannel.computeIfAbsent(config.name) { CopyOnWriteArrayList() }
    .add(LocalSubscriber(subscriber))   // subscriber.handler already applies cloudEventsInclude and the ceRegistry
}
```

- **Registry and filtering.** `subscriber.handler`, built in `CloudEventConfigDiscoveryService.recodeSubscriber`,
  already filters on the subscriber's `cloudEventsInclude` and routes into the right registry (`common` or a named
  one such as `enricher`). Local delivery calls it, so behaviour matches NATS exactly.
- **Broadcast vs queue.** Irrelevant in one process. Each subscriber definition that matches this application's tags
  receives each event once, which is what NATS delivers to a single instance.
- **No compression.** Pass `compress = false`. There is no network, so gzip costs CPU for nothing. The event still goes
  through `CacheJsonMapper.toEventData` and `receiverRegistry.process(ce)`, the same serialize/deserialize and OTEL
  telemetry path as NATS. This keeps parity and is the safest first version. A later optimisation could hand the
  already-built object through `process(data, event)` and skip serialisation.
- **Ordering.** The publisher registry already submits each handler call to its thread pool
  (`cloudevents.publisher.thread-pool`). If delivery ran directly on those threads, two quick updates to the same
  feature could reach Dacha2 out of order. A NATS subscription delivers in order on one dispatcher thread. To match
  that, `LocalSubscriber.deliver` enqueues onto a **single-threaded executor per subscriber**, from `ExecutorSupplier`,
  so it is shut down on lifecycle shutdown. Dacha2's own `dacha2.thread-processors` fan-out downstream is unchanged.
- **Publishers with no subscribers.** For example `edge-stats-emitter` in a build that includes the usage emitter. These
  log once at startup at INFO ("local: channel X has publishers but no in-process subscribers; events will be
  dropped") and at DEBUG per event.

### 3.3 Choosing the platform

In `CloudEventsFeature`:

```
external = ServiceLoader<EventingFeatureSource>.filter { it.enabled }
if external is not empty          -> register those (today's behaviour, unchanged)
else if cloudevents.local.enabled -> register LocalEventingFeature
else                              -> nothing (today's behaviour for e.g. a misconfigured mr)
```

- `publishingEnabled()` returns true in the local case too, so `EventingFeature` binds the real
  `CloudEventCacheBroadcaster` / `DbCacheSource`, and `RedisCacheFeature` can still be used if `redis.url` is set.
  Consider renaming it `eventingEnabled()`; it has two callers.
- An external platform always wins. Local is never combined with one, because that would double-deliver: once
  in-process, once through the stream.
- `cloudevents.local.enabled` defaults to **false** everywhere. Only the party-server turns it on (3.4), so standalone
  `mr`, `dacha2` and `edge` can't fall into local mode by accident and silently stop talking to each other.

### 3.4 Party-server changes

1. `Application.main`: `System.setProperty("cloudevents.local.enabled", "true")` unless the property is already set
   (an operator can set `false` to make "no transport" a hard error). At startup, log one clear line saying which mode
   is active:
   - `party-server: cloud events via NATS (nats://…); multiple party-server instances supported`
   - `party-server: no external event streaming configured, using in-process events. This must be the ONLY
     party-server instance using this database.` (WARN)
2. `deploy-defaults/common.properties`: **remove** `nats.urls=nats://localhost:4222`.
3. Delete `BatheInitializerNatsServer.java` and its `META-INF/services/bathe.BatheInitializer` entry. With no binary in
   the image it would only log "No NATS server in /target", and its `dont-run-nats-server` handling is broken anyway.
4. `pom.xml`: remove the `app.baseimage` override so it inherits `featurehub/base_jdk` from `tile-app-with-ui`. **First**
   fix `base_jdk` (3.5).
5. Keep the `eventing-nats`, `eventing-kafka` and `eventing-google-pubsub` dependencies. They are Java libraries, not the
   server, and they are what lets the party-server use an external stream.
6. `DachaClientFeature` needs no change: without NATS it already binds `NoNatsBackupDachaFactory`, and the party-server
   registers the in-process `DachaApiKeyService` in `PartyInjectorStarter`.

### 3.5 Base image

`base_jdk:2.2` lacks the `chown -R 999:999` on `/db`, `/target`, `/etc/app-config` and `/etc/common-config` that
`base_party` has. Two options:

- **A (recommended).** Add the chown to `pipeline/build/base_jdk/Dockerfile`, publish `base_jdk:2.3`, and bump
  `tile-app/tile.xml` and `tile-app-with-ui/tile.xml`. It's harmless for the other images, which don't write there,
  and it leaves one base image to maintain.
- **B.** Publish `base_party:3.0` without NATS. That keeps the party-server on its own base image, which is more
  surface for no gain.

Either way, stop building `base_party` with NATS. Existing `base_party:2.2` tags stay on DockerHub so older party-server
versions still pull.

### 3.6 External streaming with the party-server

No change in behaviour. Set `nats.urls` (or the Kafka, Pub/Sub or Kinesis properties) and that platform is used. The
routing in 2.2 already handles several party-servers on one stream: each instance has its own `dacha2` and
`edge-features` broadcast subscriptions, and `mr-notifications` is a queue group, so only one MR handles each update.

What users lose is the "free" NATS server inside the container. Anyone who pointed several party-servers at one
container's embedded NATS (`nats.urls=nats://party-1:4222`) must now run NATS themselves. Running the official
`nats` image as a container or sidecar is a few lines in compose.

### 3.7 Single-instance assumption

In local mode, a second party-server on the same database would never hear about the first one's changes. Its
Dacha2/Edge would serve stale features until restart. It can't be detected for free, so:

- **v1:** the startup WARN line (3.4), plus documentation in the party-server README, `installation.adoc` and
  `configuration.adoc`.
- **Optional later:** a lightweight heartbeat row. A party-server in local mode writes `(instance id, last seen)` every
  30s to a small table, and logs ERROR (optionally refuses readiness) if it sees another live local-mode instance. That
  needs a migration, so leave it out unless users actually hit this.

### 3.8 Things that stay unavailable without a stream

- **`nats://` dynamic destinations** (`NATSDynamicPublisher`, used by `CloudEventDynamicPublisherRegistry` for
  user-configured publish targets). Without NATS configured there is no provider for that prefix. `http(s)://` webhooks
  still work through `WebDynamicPublisher`. Log a clear warning when such a destination is configured and no provider
  exists, if that isn't already logged.
- **NATS health check.** It disappears from `/health/readiness`. Local mode is always healthy, which is correct.

## 4. Compatibility and migration

| User setup today | After this change | Action |
|------------------|-------------------|--------|
| Single party-server, no NATS config (most evaluation and Option 1a users) | Local mode, same behaviour | None |
| Party-server(s) with their own external NATS/Kafka/PubSub | Unchanged | None |
| Several party-servers using one container's embedded NATS | Breaks: no NATS in the container | Run NATS separately; set `nats.urls` on all instances |
| Mounted config that copied `nats.urls=nats://localhost:4222` | Fails at startup (connection refused) | Remove the property. The startup error should say so (below) |
| `dont-run-nats-server` set | No-op | Can be removed |

For the copied-localhost case, catch the connection failure in the party-server. If `nats.urls` resolves only to
`localhost` / `127.0.0.1`, log one ERROR before exiting:
`nats.urls points at localhost but this image no longer contains a NATS server; remove nats.urls to use in-process events, or point it at your NATS server`.
Don't fall back to local mode silently. That could turn a multi-instance setup into several independent, stale
instances.

Release notes need a clear "breaking for multi-instance-on-embedded-NATS" entry. Make this a minor-version change and
call it out in `backend/HOW_TO_RELEASE_OS.adoc`.

**External repos to check** (not in this repo; follow-up PRs): the `featurehub-install` docker-compose examples and the
`featurehub-helm` chart, for any `nats.urls` set on party-server or reliance on the embedded NATS.

## 5. Documentation updates

- `backend/party-server/README.md` and `README.adoc`: remove "an embedded NATS server that connects them"; describe
  in-process vs external streaming and the single-instance rule; update the `nats.urls` row.
- `pipeline/build/base_party/README.md`: mark it deprecated, or delete it if option 3.5 A is taken.
- `docs/modules/ROOT/pages/installation.adoc` (Option 1a), `configuration.adoc` (Party Server and NATS sections,
  `cloudevents.local.enabled`), `troubleshooting.adoc`.
- `docs/machine-usage/cloudevents.md`: add the `local` platform to the list in "structure of projects", and explain that
  it is a loopback selected only when no external platform is enabled.
- Mentions of embedded NATS in `backend/mr/README.md`, `backend/dacha2/README.md`, `backend/dacha/README.md` and
  `backend/edge-full/README.md`: check each and reword where it refers to the party-server.

## 6. Testing

**Unit (Spock, `eventing-cloudevents`)**
- `LocalConfiguredSource`: an event on a channel reaches every in-process subscriber exactly once; `cloudEventsInclude`
  is honoured; a named `ceRegistry` (e.g. `enricher`) receives it and `common` doesn't; a channel with no subscriber
  drops without error; order is preserved for a burst of events on one subscriber.
- `CloudEventsFeature` selection: nothing enabled with `cloudevents.local.enabled=true` gives local; NATS configured plus
  the local flag gives NATS only; nothing enabled without the flag gives nothing; `publishingEnabled()` agrees in each
  case.
- Discovery with the party-server tag set against the `local` section:
  - `publish-feature-v1` reaches the Dacha2 and Edge listeners once each (no double delivery);
  - `enricher-ping-v1` reaches the `enricher` registry;
  - `feature-update-v1` reaches MR;
  - the `enriched-environment-channel` is wired only when `webhooks.features.enabled=true`.

**Startup**
- The party-server starts with no `nats.urls`. Dacha2 with `on-reconnect` resolves `EventingConnection`, and there is
  no "no destination" error at startup.
- The party-server with `nats.urls=nats://localhost:4222` and no NATS exits with the guidance message.

**Image**
- `docker run --user 999:999 -v $HOME/party:/db featurehub/party-server:<new>` writes H2 to `/db`, so the base image
  chown is correct.
- `/target/nats-server` doesn't exist, and the image is smaller (record before and after).

**End to end (`adks/e2e-sdk`)**
- Full suite against a local-mode party-server: SSE and REST, feature changes from MR reaching SDKs, Edge PUT updates
  (`feature-update-v1`), environment/service account changes, and webhooks with `webhooks.features.enabled=true`.
- Full suite against **two** party-servers plus an external `nats` container behind a load balancer. This shows
  multi-instance still works through an external stream.

## 7. Implementation plan

1. **Eventing core:** `LocalEventingFeature`, `LocalConfiguredSource`, `local: {}` in `cloud-events.yaml`, the selection
   logic and `publishingEnabled()` in `CloudEventsFeature`, and unit tests.
2. **Base image:** add the chown to `base_jdk` and publish `base_jdk:2.3`; bump the tiles.
3. **Party-server:** set the local flag and mode logging in `Application.main`; remove `nats.urls` from
   `common.properties`; delete `BatheInitializerNatsServer` and its service file; drop `app.baseimage`; add the
   localhost-NATS guidance on connection failure.
4. **Verification:** startup and image checks; e2e in local mode and with two instances on external NATS.
5. **Docs and release notes** (sections 4 and 5); deprecate `base_party`.
6. **Follow-ups (separate):** `featurehub-install` / `featurehub-helm` updates; optional single-instance heartbeat
   (3.7); optional serialisation bypass for local delivery (3.2).

## 8. Open questions

1. Should `party-server-ish` stay? Without NATS, the party-server is a strict superset (MR + Dacha2 + streaming and REST
   Edge) at a similar footprint. Keeping both doubles the images to build and document.
2. Base image option A (fix `base_jdk`) or B (`base_party:3.0` without NATS)? A is recommended.
3. Should local mode refuse readiness if a second local-mode instance is detected (needs 3.7's heartbeat), or only warn?
4. Should we publish a transitional `party-server:<version>-nats` image with the embedded NATS for one or two releases,
   for users relying on multi-instance embedded NATS? The recommendation is no: document the compose sidecar instead.
