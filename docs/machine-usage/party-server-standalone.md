# Party Server without embedded NATS

Status: implemented (except section 7, steps 6 and 7)
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
- **Channel conditionals are ignored, as NATS ignores them.** `CloudEventConfig.enabled` (the channel-level
  `conditional`) isn't checked by any platform. This matters for `enriched-environment-channel`: its conditional
  `webhooks.features.enabled` defaults to `false` in `cloud-events.yaml`, but `WebhookFeature.enabled` defaults to
  `true`. If local mode honoured the conditional, webhooks would stop working by default. The local platform wires the
  channel unconditionally, the same as NATS.
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
  It keeps its name: it has one caller in this repo, and the name is public API.
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
| Create user `k8suser` 999:999 | Use the image's built-in **`nobody`** user (`65534:65534`; home `/`, shell `/sbin/nologin`) with Jib `<container><user>65534:65534</user>` in **both** tiles. `tile-app` currently sets `999:999`; `tile-app-with-ui` sets nothing and relies on `docker run --user 999:999`. Set the user **numerically**, not as `nobody`: Kubernetes `runAsNonRoot` rejects an image whose user isn't numeric because it can't verify it. Because `nobody` has a passwd entry, the JVM gets a sensible `user.home`. |
| `mkdir /db` owned by 999 (H2 data) | An empty `/db` added through Jib `extraDirectories`, with ownership set to `65534:65534` by the Jib Ownership extension (`jib-ownership-extension-maven` from GoogleContainerTools `jib-extensions`). Fallback if the extension doesn't suit Jib 3.5.1: mode `1777` on `/db` via `<permissions>`. Ownership is needed so that Docker *named* volumes, which copy the image directory's ownership on first use, are writable by `nobody`. |
| `mkdir /etc/app-config` | An empty directory via `extraDirectories`. It's a read-only mount point, so root ownership is fine. |
| `mkdir /etc/common-config` | Already created by Jib from `deploy-defaults`; root-owned and world-readable, which is fine. |
| `mkdir /target`, NATS binary and config | Dropped. |
| `EXPOSE 8085 8701` | Jib `<container><ports>`. |

Delete `pipeline/build/base_jdk` and `pipeline/build/base_party`, and remove both from
`.github/workflows/dockerhub-readme.yml`. Existing tags stay on DockerHub so older FeatureHub versions still pull.

#### Alpine (musl) compatibility

Alpine uses musl libc instead of glibc. Any dependency that bundles a native library built against glibc can fail to
load there. The party-server runtime classpath (329 jars from `mvn dependency:tree`) has seven jars with Linux native
libraries. Each was loaded inside `eclipse-temurin` Alpine on arm64 and amd64:

| Library | Pulled in by | Used when | Result on Alpine |
|---|---|---|---|
| Bouncy Castle LTS (`bcprov-lts8on`) | `jnats` | NATS with NKey / credentials auth | **arm64: fails hard.** `CryptoServicesRegistrar` can't initialise (its probe library needs `ld-linux-aarch64.so.1`), so NKey signing throws `NoClassDefFoundError`. amd64 works. |
| `snappy-java` 1.1.10.5 | `kafka-clients` 3.9.2 | Kafka messages compressed with snappy | **amd64: fails** (needs `ld-linux-x86-64.so.2`). arm64 works. |
| gRPC's BoringSSL TLS library (`netty-tcnative`, inside `grpc-netty-shaded`) | Google Pub/Sub | Pub/Sub TLS | Works: native on arm64, falls back to JDK TLS (ALPN `h2`) on amd64 |
| Netty epoll (inside `grpc-netty-shaded`) | Google Pub/Sub | Pub/Sub transport | Works on both |
| Conscrypt | Google libraries | Optional TLS provider | Unavailable; optional, nothing depends on it |
| `zstd-jni`, `lz4-java` | `kafka-clients` | Kafka compression | Work (lz4 uses pure Java on arm64) |
| `jffi` (Linux build), Oracle TCP fast-open | Cloud SQL socket factory, `ojdbc11` | Unix-socket Cloud SQL, Oracle | The libraries load (`ldd` only, not exercised at runtime) |

**Alpine is usable, with two required fixes** (both verified on arm64 and amd64):

1. **Bouncy Castle: force pure Java.** Add `-Dorg.bouncycastle.native.cpu_variant=java` to the Jib `<jvmFlags>` in
   `tile-app` and `tile-app-with-ui`. With it, Bouncy Castle reports `UNSUPPORTED` for native services, uses its Java
   implementations, and Ed25519 / NATS NKey create, sign and verify pass on both architectures. Without it, NATS NKey
   auth is broken on ARM hosts.
2. **snappy: use a musl-aware version.** Pin `org.xerial.snappy:snappy-java` to `1.1.10.12` (or later) in dependency
   management. That version ships `Linux/x86_64-musl`, and compress/uncompress round-trips pass on both architectures.
   FeatureHub's Kafka module sets no compression, so its own producers use Kafka's default of none. A consumer still
   needs snappy if any other producer on the topic compresses with it.

Neither affects the default party-server (local events with H2 or a server database). Each needs an optional transport
on a particular architecture, which makes it easy to miss without a test.

**Keep it tested.** Add a small musl smoke test that runs in CI against the pinned image on both architectures:

- Use the pinned `eclipse-temurin:25-jdk-alpine` digest, so a Java source file can run directly.
- Put the runtime jars on the classpath.
- Exercise each native-backed library through its public API:
  - gRPC `OpenSsl.isAvailable()` and `GrpcSslContexts.forClient()`;
  - gRPC `Epoll.isAvailable()`;
  - Bouncy Castle native status plus an Ed25519 sign/verify;
  - `jnats` `NKey.createUser` / `sign`;
  - zstd and snappy round-trips;
  - `LZ4Factory.fastestInstance()`.

This catches a dependency bump that adds a glibc-only native library, which is the main risk of staying on Alpine. The
amd64 results above were taken under emulation on Apple silicon, so the CI run on real amd64 confirms them.

If a future dependency has no musl build and no Java fallback, the pinned base can move to the Ubuntu-based
`eclipse-temurin:25-jre` (Temurin's non-Alpine images are Ubuntu Noble, not Debian) by changing the same tile
property.

### 3.6 External streaming with the party-server

No change in behaviour. Set `nats.urls` (or the Kafka, Pub/Sub or Kinesis properties) and that platform is used. The
routing in 2.2 already handles several party-servers on one stream: each instance has its own `dacha2` and
`edge-features` broadcast subscriptions, and `mr-notifications` is a queue group, so only one MR handles each update.

What users lose is the "free" NATS server inside the container. Anyone who pointed several party-servers at one
container's embedded NATS (`nats.urls=nats://party-1:4222`) must now run NATS themselves. Running the official
`nats` image as a container or sidecar is a few lines in compose.

### 3.7 Single-instance assumption

In local mode, a second party-server on the same database never hears about the first one's changes, so its Dacha2 and
Edge serve stale features until restart. FeatureHub **does not try to detect this**. Running one instance, or
configuring an external stream for several, is the operator's responsibility. The party-server:

- logs the WARN line at startup (3.4), saying which mode is active and that local mode means a single instance;
- documents the rule in the party-server README, `installation.adoc` and `configuration.adoc`.

H2 in file mode (the evaluation default) can't be shared anyway: it locks the database file, so a second process can't
open it.

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
| Existing `/db` data, volume or PVC owned by UID 999 (`--user 999:999`, the old `k8suser`) | The new default user `nobody` (65534) can't write it, so H2 fails to open | One-off `chown -R 65534:65534` of the data; or keep running as 999 with `--user 999:999` / `runAsUser: 999`; or in Kubernetes set `fsGroup: 65534` |
| `docker run --user 999:999 …` from the docs | Still works on a fresh `/db` bind mount the user owns; the docs move to the default user | Drop `--user`, or use `65534:65534` |

For the copied-localhost case, catch the connection failure in the party-server. If `nats.urls` resolves only to
`localhost` / `127.0.0.1`, log one ERROR before exiting:
`nats.urls points at localhost but this image no longer contains a NATS server; remove nats.urls to use in-process events, or point it at your NATS server`.
Don't fall back to local mode silently. That could turn a multi-instance setup into several independent, stale
instances.

**Versioning.** This is a feature change, so it ships in a **minor** release, as every feature change does. Patch
releases are reserved for dependency and base-image updates, such as bumping the pinned Temurin digest (3.5). There is
**no transitional image** with embedded NATS. Release notes need a clear "breaking for multi-instance-on-embedded-NATS"
entry, pointing at the compose NATS sidecar example. Record the patch/minor rule in `backend/HOW_TO_RELEASE_OS.adoc`.

**External repos to update** (not in this repo; tracked as follow-up PRs):

- **`featurehub-helm`** (required): change `securityContext.runAsUser` / `runAsGroup` from 999 to 65534 and set
  `fsGroup: 65534` for every FeatureHub workload. Document the PVC ownership migration for upgrades. Remove any
  reliance on the embedded NATS in the party-server option, and drop any `nats.urls` pointing at it.
- **`featurehub-install`**: in the docker-compose examples, remove `user: 999:999` (or switch to 65534), remove
  `nats.urls` set on party-server, and add a NATS sidecar example for multi-instance setups.

## 5. Documentation updates

- `backend/party-server/README.md` and `README.adoc`: remove "an embedded NATS server that connects them"; describe
  in-process vs external streaming and the single-instance rule; update the `nats.urls` row.
- `pipeline/build/base_jdk/README.md` and `pipeline/build/base_party/README.md`: delete them with their directories
  (3.5). Update any docs that tell people to build or bump those images, e.g. `backend/HOW_TO_RELEASE_OS.adoc` and
  `backend/party-server/pom.xml` comments.
- `docs/modules/ROOT/pages/installation.adoc` (Option 1a), `configuration.adoc` (Party Server and NATS sections,
  `cloudevents.local.enabled`), `troubleshooting.adoc`.
- **User 999 → `nobody` (65534):** update every `--user 999:999` / `k8suser` reference to use the default user or
  `65534:65534`, and add the data-ownership migration note:
  - `readme.adoc`, `CLAUDE.md` (the evaluation `docker run` line);
  - `docs/modules/ROOT/pages/evaluation.adoc`, `maintenance.adoc`;
  - the image READMEs in `backend/` (`party-server`, `mr`, `dacha2`, `dacha`, `edge-full`, `edge-rest`,
    `maintenance-responder`);
  - `pipeline/build/party-server-test-db/Dockerfile.part`, which bakes a test H2 database into `/db` and runs
    `chown -R 999:999 /db`. Change it to `65534:65534`, or the e2e test-db image won't start under the new user.
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
  - `enriched-feature-v1` reaches Edge, whatever the channel conditional says (see 3.2).

**Startup**
- The party-server starts with no `nats.urls`. Dacha2 with `on-reconnect` resolves `EventingConnection`, and there is
  no "no destination" error at startup.
- The party-server with `nats.urls=nats://localhost:4222` and no NATS exits with the guidance message.

**Image**
- A named volume (`-v partydata:/db`) writes H2 to `/db` as `nobody` (65534) with no `--user` flag.
- A bind mount owned by 65534 works with no `--user` flag; a bind mount owned by 999 works with `--user 999:999`
  (the compatibility path in section 4).
- In Kubernetes, `runAsNonRoot: true` is accepted, which needs the numeric image user.
- The image config shows `User: 65534:65534`, and the base layer digest matches the pinned Temurin index for both
  amd64 and arm64.
- `/target/nats-server` doesn't exist, and the image is smaller (record before and after).
- The musl smoke test (3.5) passes on arm64 and amd64.
- The container's JVM flags include `-Dorg.bouncycastle.native.cpu_variant=java`, and the resolved `snappy-java` is
  `1.1.10.12` or later (`mvn dependency:tree`).

**End to end (`adks/e2e-sdk`)**
- Full suite against a local-mode party-server: SSE and REST, feature changes from MR reaching SDKs, Edge PUT updates
  (`feature-update-v1`), environment/service account changes, and webhooks with `webhooks.features.enabled=true`.
- Full suite against **two** party-servers plus an external `nats` container behind a load balancer. This shows
  multi-instance still works through an external stream.

## 7. Implementation plan

1. **Eventing core:** `LocalEventingFeature`, `LocalConfiguredSource`, `local: {}` in `cloud-events.yaml`, the selection
   logic and `publishingEnabled()` in `CloudEventsFeature`, and unit tests.
2. **Base image:** pin `eclipse-temurin:25-jre-alpine@sha256:…` in both tiles. Run as `nobody` (`65534:65534`)
   and add ports, `/db` (owned by 65534) and `/etc/app-config` via Jib. Remove the `dacha2` and `party-server`
   overrides. Delete `pipeline/build/base_jdk` and `base_party`. Set up the automated digest bump. Add the musl
   fixes, `-Dorg.bouncycastle.native.cpu_variant=java` in both tiles and the `snappy-java` 1.1.10.12 pin, plus the
   musl smoke test in CI (3.5).
3. **Party-server:** set the local flag and mode logging in `Application.main`; remove `nats.urls` from
   `common.properties`; delete `BatheInitializerNatsServer` and its service file; drop `app.baseimage`; add the
   localhost-NATS guidance on connection failure.
4. **Verification:** startup and image checks; e2e in local mode and with two instances on external NATS.
5. **Docs and release notes** (sections 4 and 5); deprecate `base_party`.
6. **Retire `party-server-ish`**, in the minor release *after* this one (section 8).
7. **Follow-ups (separate repos and PRs):**
   - `featurehub-helm`: user 65534, `fsGroup`, PVC migration notes, no embedded NATS (section 4);
   - `featurehub-install`: compose updates (section 4);
   - optional serialisation bypass for local delivery (3.2).

## 8. Decisions

| Topic | Decision |
|-------|----------|
| `party-server-ish` | **Retire it, one minor release after this change.** It reads the database on every Edge request (2.3). A NATS-free party-server gives the same single-container deployment with Dacha2's cache and streaming Edge. This change's release notes announce the deprecation; the next minor release removes the module, its image and its docs. |
| Base images | Pin `eclipse-temurin` by OCI digest in the tiles; delete `base_jdk` and `base_party` (3.5) |
| Alpine vs Ubuntu | Alpine (`25-jre-alpine`), with the Bouncy Castle flag, the `snappy-java` pin and a CI musl smoke test (3.5). Ubuntu stays available as a one-property fallback. |
| Container user | The image's built-in `nobody`, set numerically as `65534:65534`, replacing 999 (3.5). `featurehub-helm` must be updated to match (section 4) |
| Second-instance detection | None. Running a single local-mode instance is the operator's responsibility; the startup log and docs say so (3.7) |
| Transitional NATS image | None. Feature changes are minor releases; patch releases only update dependencies and base images (section 4) |

## 9. Verification (implementation)

- Unit: `CloudEventsFeatureSpec` (platform selection) and `LocalConfiguredSourceSpec` (routing with the party-server
  tags, registries, no compression, ordering, failures, shutdown) in `eventing-cloudevents`; `PartyEventingModeSpec`
  in `party-server`.
- Image (`featurehub/party-server` built locally with Jib): `User` is `65534:65534`, ports `8085` and `8701` are exposed,
  `/db` is owned by `65534:65534` (Jib Ownership extension 0.1.0 works with Jib 3.5.1), `/target` and `nats.urls` are
  gone, and the `-Dorg.bouncycastle.native.cpu_variant=java` flag is set. A named volume on `/db` with no `--user`
  writes H2.
- Startup: local mode logs the WARN line. `nats.urls=nats://localhost:4222` with nothing listening exits with the
  guidance message. `cloudevents.local.enabled=false` with no stream exits with an error.
- e2e (`adks/e2e-sdk`, `not @saasflags`) against one local-mode party-server: 40/41 scenarios pass. The remaining
  failure calls `/mr-api/testing/slack-testing`, a test-only endpoint the production image doesn't have.
- e2e against two party-servers on Postgres plus an external `nats` container, with the MR API on instance A and Edge
  on instance B (so every change has to cross NATS): 39/41 pass. One failure is the same Slack test-only endpoint. The
  other: `webhooks.feature` receives each enriched webhook **twice**, from two separate enrichment runs (the event
  ids differ). Each instance's Dacha2 enriches the broadcast cache update. This is on the NATS path, which this change
  doesn't touch, so it is most likely existing multi-instance behaviour. It hasn't been checked against `main`.
- musl smoke test (`pipeline/build/musl-smoke`): passes on arm64 natively and on amd64 under emulation. CI runs it on
  native runners of both architectures (`.github/workflows/musl-smoke.yml`).
