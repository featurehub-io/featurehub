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
- **H2 can't write.** `base_party` is the only thing that creates `/db` owned by UID 999. The party-server runs as
  `999:999` and writes H2 to `/db`. Any replacement base image must provide that, or evaluation installs break.
  `featurehub/base_jdk:2.2` creates `/db` but doesn't chown it.

`party-server-ish` gets away without NATS only because it has no Dacha2 and no streaming Edge. Its `EdgeGetFeature`
binds `DbDachaCacheSqlApi`, so **every** Edge REST request queries the database directly with no cache: it looks up
the service account and environment, then loads the features, feature values and feature-group strategies.

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
4. `pom.xml`: remove the `app.baseimage` override, so it inherits the pinned Temurin image from `tile-app-with-ui`
   (3.5).
5. Keep the `eventing-nats`, `eventing-kafka` and `eventing-google-pubsub` dependencies. They are Java libraries, not the
   server, and they are what lets the party-server use an external stream.
6. `DachaClientFeature` needs no change: without NATS it already binds `NoNatsBackupDachaFactory`, and the party-server
   registers the in-process `DachaApiKeyService` in `PartyInjectorStarter`.

### 3.5 Base image: pinned Eclipse Temurin, no FeatureHub base images

Retire **both** `featurehub/base_jdk` and `featurehub/base_party`. Every image, including the party-server, builds
directly from Eclipse Temurin, **pinned by OCI digest**:

```xml
<!-- tile-app/tile.xml and tile-app-with-ui/tile.xml -->
<app.baseimage>eclipse-temurin:25-jre-alpine@sha256:<index digest></app.baseimage>
```

- **Pin the multi-arch index digest**, not a per-platform manifest. Jib's `<platforms>` (amd64 and arm64, already in the
  tiles) then resolves the right image for each architecture from one pinned reference.
- `dacha2/pom.xml` already uses `eclipse-temurin:25-jre-alpine` unpinned. Remove its override so it inherits the pinned
  value. The `party-server` override goes too (3.4).
- **Patch releases bump the digest.** Updating the base image means changing one property in the two tiles, which fits
  the versioning rule in section 4. Automate the bump with Renovate or Dependabot's Docker digest pinning for that
  property, or a small scheduled workflow.
- Keep `app.baseimage.prefix` working for anyone who mirrors images, as it does today.

**What the FeatureHub base images did, and how Jib replaces it** (Jib can't run `RUN` steps):

| `base_jdk` / `base_party` did | Replacement |
|-------------------------------|-------------|
| Create user `k8suser` 999:999 | Jib `<container><user>999:999</user>`. `tile-app` already sets this; add it to `tile-app-with-ui`, which currently relies on `docker run --user 999:999`. The Temurin image has no passwd entry for 999. That's fine for the JVM, but set `-Duser.home=/tmp` (or `HOME`) so nothing tries to use `?` as a home directory. |
| `mkdir /db` owned by 999 (H2 data) | An empty `/db` added through Jib `extraDirectories`, with ownership set to `999:999` by the Jib Ownership extension (`jib-ownership-extension-maven` from GoogleContainerTools `jib-extensions`). Fallback if the extension doesn't suit Jib 3.5.1: mode `1777` on `/db` via `<permissions>`. Ownership is needed so that Docker *named* volumes, which copy the image directory's ownership on first use, are writable by 999. Bind mounts behave as today. |
| `mkdir /etc/app-config` | An empty directory via `extraDirectories`. It's a read-only mount point, so root ownership is fine. |
| `mkdir /etc/common-config` | Already created by Jib from `deploy-defaults`; root-owned and world-readable, which is fine. |
| `mkdir /target`, NATS binary and config | Dropped. |
| `EXPOSE 8085 8701` | Jib `<container><ports>`. |

Delete `pipeline/build/base_jdk` and `pipeline/build/base_party`, and remove both from
`.github/workflows/dockerhub-readme.yml`. Existing tags stay on DockerHub so older FeatureHub versions still pull.

### 3.6 External streaming with the party-server

No change in behaviour. Set `nats.urls` (or the Kafka, Pub/Sub or Kinesis properties) and that platform is used. The
routing in 2.2 already handles several party-servers on one stream: each instance has its own `dacha2` and
`edge-features` broadcast subscriptions, and `mr-notifications` is a queue group, so only one MR handles each update.

What users lose is the "free" NATS server inside the container. Anyone who pointed several party-servers at one
container's embedded NATS (`nats.urls=nats://party-1:4222`) must now run NATS themselves. Running the official
`nats` image as a container or sidecar is a few lines in compose.

### 3.7 Single-instance assumption and detecting a second instance

In local mode, a second party-server on the same database would never hear about the first one's changes. Its Dacha2
and Edge would serve stale features until restart. Two things already help:

- **H2 (the evaluation default) can't be shared.** H2 in file mode holds a lock on the database file, so a second
  process can't open `/db/party-h2.db`. Detection only matters for server databases (Postgres, MySQL, MariaDB, SQL
  Server, Oracle).
- **The startup WARN line** (3.4) states the rule, and the docs repeat it.

**Detection: a database heartbeat.** The only thing two party-servers on one database share is the database, so detect
through it:

1. New table `fh_instance` (Ebean entity plus migrations for every supported database):

   | Column | Notes |
   |--------|-------|
   | `id` | uuid PK, generated at process start |
   | `kind` | `party-local` or `party-streaming` |
   | `hostname` | from `HOSTNAME` / `InetAddress`, for the log message |
   | `when_started` | |
   | `last_seen` | always set from **database time** (`current_timestamp` in the update), so clock skew between hosts doesn't matter |

2. Every party-server upserts its own row on startup and every 30s (`party.instance.heartbeat-seconds`). On graceful
   shutdown it deletes the row. After a crash, the row simply ages out.
3. On each heartbeat it queries other rows with `last_seen > now - 3 × heartbeat` and reports a **conflict** if:
   - this instance is `party-local` and any other live instance exists, of either kind; or
   - this instance is `party-streaming` and a live `party-local` instance exists. A local-mode instance can't hear
     streamed updates, so a mixed fleet is broken too.

   Several `party-streaming` instances together are normal and never conflict.
4. **Rolling deploys** briefly overlap an old and a new instance. Report a conflict only after it has been seen on two
   consecutive heartbeats (about 60s). A normal replacement finishes within that, and the old instance deletes its row
   on shutdown.
5. **On conflict:** log ERROR naming both hostnames and how to fix it (configure an external stream, or run one
   instance). Also set a gauge metric, `party_instance_conflict`, so it can alert. Whether it should also fail readiness
   is open question 3.

The cost is one indexed UPDATE and one small SELECT every 30s per instance.

Alternatives considered:
- **Database advisory locks** (`pg_try_advisory_lock`, MySQL `GET_LOCK`, SQL Server `sp_getapplock`, Oracle `DBMS_LOCK`).
  These give an exact answer, but each database needs different SQL, the lock lives on one pinned connection outside
  the Ebean pool, and it can't see the mixed local/streaming case.
- **Network discovery** (multicast, DNS). It doesn't work across typical container networks and adds configuration.

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

**Versioning.** This is a feature change, so it ships in a **minor** release, as every feature change does. Patch
releases are reserved for dependency and base-image updates, such as bumping the pinned Temurin digest (3.5). There is
**no transitional image** with embedded NATS. Release notes need a clear "breaking for multi-instance-on-embedded-NATS"
entry, pointing at the compose NATS sidecar example. Record the patch/minor rule in `backend/HOW_TO_RELEASE_OS.adoc`.

**External repos to check** (not in this repo; follow-up PRs): the `featurehub-install` docker-compose examples and the
`featurehub-helm` chart, for any `nats.urls` set on party-server or reliance on the embedded NATS.

## 5. Documentation updates

- `backend/party-server/README.md` and `README.adoc`: remove "an embedded NATS server that connects them"; describe
  in-process vs external streaming and the single-instance rule; update the `nats.urls` row.
- `pipeline/build/base_jdk/README.md` and `pipeline/build/base_party/README.md`: delete them with their directories
  (3.5). Update any docs that tell people to build or bump those images, e.g. `backend/HOW_TO_RELEASE_OS.adoc` and
  `backend/party-server/pom.xml` comments.
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
- `docker run -v $HOME/party:/db featurehub/party-server:<new>` (bind mount) and the same with a named volume
  (`-v partydata:/db`) both write H2 to `/db` as UID 999, with and without `--user 999:999`.
- The image config shows `User: 999:999`, and the base layer digest matches the pinned Temurin index for both amd64
  and arm64.
- `/target/nats-server` doesn't exist, and the image is smaller (record before and after).

**End to end (`adks/e2e-sdk`)**
- Full suite against a local-mode party-server: SSE and REST, feature changes from MR reaching SDKs, Edge PUT updates
  (`feature-update-v1`), environment/service account changes, and webhooks with `webhooks.features.enabled=true`.
- Full suite against **two** party-servers plus an external `nats` container behind a load balancer. This shows
  multi-instance still works through an external stream, with no conflict reported.
- Instance detection on Postgres: two local-mode instances report a conflict within about 60s; a local-mode and a
  streaming instance report one; two streaming instances don't; a rolling restart of one local-mode instance
  doesn't.

## 7. Implementation plan

1. **Eventing core:** `LocalEventingFeature`, `LocalConfiguredSource`, `local: {}` in `cloud-events.yaml`, the selection
   logic and `publishingEnabled()` in `CloudEventsFeature`, and unit tests.
2. **Base image:** pin `eclipse-temurin:25-jre-alpine@sha256:…` in both tiles. Add the user, ports, `/db` (owned
   by 999) and `/etc/app-config` via Jib, and remove the `dacha2` and `party-server` overrides. Delete
   `pipeline/build/base_jdk` and `base_party`. Set up the automated digest bump.
3. **Party-server:** set the local flag and mode logging in `Application.main`; remove `nats.urls` from
   `common.properties`; delete `BatheInitializerNatsServer` and its service file; drop `app.baseimage`; add the
   localhost-NATS guidance on connection failure.
4. **Verification:** startup and image checks; e2e in local mode and with two instances on external NATS.
5. **Docs and release notes** (sections 4 and 5); deprecate `base_party`.
6. **Instance detection:** `fh_instance` table and migrations, heartbeat and conflict reporting (3.7).
7. **Retire `party-server-ish`** (section 8).
8. **Follow-ups (separate):** `featurehub-install` / `featurehub-helm` updates; optional serialisation bypass for local
   delivery (3.2).

## 8. Decisions and open questions

| Topic | Decision |
|-------|----------|
| `party-server-ish` | **Retire it.** It reads the database on every Edge request (2.3). A NATS-free party-server gives the same single-container deployment with Dacha2's cache and streaming Edge. Publish a final release noting that, and point users at `party-server`. Remove the module, its image and its docs in the following minor release. |
| Base images | Pin `eclipse-temurin` by OCI digest in the tiles; delete `base_jdk` and `base_party` (3.5) |
| Transitional NATS image | None. Feature changes are minor releases; patch releases only update dependencies and base images (section 4) |

Still open:

1. **Conflict handling.** Should a detected second instance (3.7) also fail `/health/readiness`, which stops traffic to
   the newer instance in Kubernetes, or only log and set the metric? The recommendation is log + metric by default,
   with `party.instance.fail-readiness-on-conflict=true` available for operators who want it enforced.
2. **Where the `party-server-ish` removal lands.** In the same minor release as this change, or one release later?
   The recommendation is one later, to give users a release's notice.
