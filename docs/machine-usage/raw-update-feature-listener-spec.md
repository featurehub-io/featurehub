# RawUpdateFeatureListener: porting specification

This document describes the `RawUpdateFeatureListener` mechanism in the FeatureHub JavaScript SDK
so that it can be reimplemented in another FeatureHub SDK (Go, C#, Python, Java, Dart) with the
same behaviour. It is written for an implementer (human or AI) who has the target SDK open but
not this codebase.

Reference sources in this repo:

| Concern                          | File                                                                            |
| -------------------------------- | ------------------------------------------------------------------------------- |
| Interface definition             | `packages/core/src/featurehub_repository.ts`                                    |
| Registration + fan-out           | `packages/core/src/client_feature_repository.ts`                                |
| Config-level registration        | `packages/core/src/edge_featurehub_config.ts`, `feature_hub_config.ts`          |
| Upstream producers (`notify`)    | `packages/core/src/network/featurehub_eventsource.ts`, `network/polling_sdk.ts` |
| Reference listener (simple)      | `plugins/stores/featurehub-store-localstorage/src/local_session_store.ts`       |
| Reference listener (distributed) | `plugins/stores/featurehub-store-redis/src/redis_session_store.ts`              |

---

## 1. Purpose

The repository receives **raw `FeatureState` objects** from upstream: the SSE streaming
connection or the REST poller. A `RawUpdateFeatureListener` receives a copy of those raw updates
_as they arrive_, along with a `source` string saying where they came from.

The main use is a **backing store** (Redis, browser storage, a file, a database):

1. **Write-through.** Every upstream change is persisted to the store.
2. **Warm start / failover.** On startup, or when upstream is unavailable, the store pushes its saved
   state back _into_ the repository. It uses the same `notify` entry point as the network layer,
   but with its own `source` tag.
3. **Echo suppression.** Because the store's own injected updates are broadcast back to every raw
   listener, including the store itself, each listener ignores events whose `source` equals its own tag.

This makes the repository a hub. Upstream transports and stores are all **producers** calling
`notify(state, data, source)`. Every registered raw listener is a **consumer**. A store is both.

The listener deals in **raw wire data**: `FeatureState` objects with `id`, `key`, `version`,
`type`, `value`, `strategies`, and so on. It does **not** deal in evaluated values, contexts or
`FeatureStateHolder` objects. It is not the same as the per-feature value listeners or the
usage/analytics stream.

---

## 2. Data model it carries

`FeatureState` (the generated API model, already present in every FeatureHub SDK):

| Field           | Type                         | Notes                                                                         |
| --------------- | ---------------------------- | ----------------------------------------------------------------------------- |
| `id`            | string (UUID)                | **Stable identity. Stores must key by `id`, not `key`.**                      |
| `key`           | string                       | User-facing name. **Can change** for the same `id`.                           |
| `version`       | int, optional                | Monotonic per feature. Used for "newer wins" comparisons. Treat missing as 0. |
| `type`          | enum                         | BOOLEAN / STRING / NUMBER / JSON                                              |
| `value`         | any, nullable                |                                                                               |
| `l`             | bool, optional               | locked                                                                        |
| `v`             | string, nullable             | strategy id applied (server-evaluated keys only)                              |
| `fp`            | map<string,string>, nullable | feature properties                                                            |
| `strategies`    | list, nullable               | rollout strategies (client-evaluated keys only)                               |
| `environmentId` | string, nullable             | **Filled in client-side** by the transport before `notify` (see §5).          |

Stores should serialise the whole object as the API's JSON so the data can be read back and passed
straight to `notify`. The Redis layout (§8.2) is shared across SDK languages, so in that case the
serialisation must match the wire JSON field names exactly.

`SSEResultState` enum (wire event names): `ack`, `bye`, `failure`, `features`, `feature`,
`delete_feature`, `config`, `error`.

---

## 3. The interface

TypeScript original:

```ts
export interface RawUpdateFeatureListener {
  // Delete an individual feature. Use feature.id where possible.
  deleteFeature(feature: FeatureState, source: string): void;

  // true if this store is connected and operational
  get connected(): boolean;

  // Full replacement: this is the complete current feature set for the environment.
  processUpdates(features: Array<FeatureState>, source: string): void;

  // Upsert of a single feature. Use feature.id; a feature can change its key.
  processUpdate(feature: FeatureState, source: string): void;

  // Release resources (timers, connections). Called by the repository on close.
  close(): void;

  // Some config may have changed; re-check anything derived from it.
  configChanged(): void;
}
```

### Method semantics

| Method           | Called when                                                     | Listener contract                                                                                                                                |
| ---------------- | --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `processUpdates` | A **full** feature list arrives (`features` event / poll)       | Treat as the **complete** set for the environment and **replace** stored state. Any feature missing from the list has been deleted.              |
| `processUpdate`  | A **single** feature change arrives (`feature` event)           | Upsert by `id`. Apply only if `version` is newer than what is stored (the Redis store checks this; the local store skips it).                    |
| `deleteFeature`  | A `delete_feature` event arrives                                | Remove by `id`. The payload may be partial; `key` and `id` are always present.                                                                   |
| `connected`      | Polled by consumers                                             | Return true only when the backing store is usable.                                                                                               |
| `close`          | Repository is closing                                           | Stop timers and release connections. Must be idempotent and must not throw. May try to deregister itself (the repository allows this, see §4.3). |
| `configChanged`  | Declared hook (see §9, it is **not currently invoked** by core) | Recompute anything derived from config (e.g. a storage key) and reload if it changed.                                                            |

Every data method receives `source`. A listener that also injects data **must** return early when
`source == its own tag`.

### Language mapping suggestions

| Language | Shape                                                                                                                                                                                                                                       |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Go       | `type RawUpdateFeatureListener interface { DeleteFeature(fs *FeatureState, source string); ProcessUpdates(fs []*FeatureState, source string); ProcessUpdate(fs *FeatureState, source string); Connected() bool; ConfigChanged(); Close() }` |
| C#       | `public interface IRawUpdateFeatureListener : IDisposable`-style, with `bool Connected { get; }` and `void Close()` (or let `Dispose` call `Close`).                                                                                        |
| Java     | `public interface RawUpdateFeatureListener { void deleteFeature(FeatureState f, String source); ... boolean isConnected(); void close(); void configChanged(); }` (consider `extends AutoCloseable`)                                        |
| Python   | `typing.Protocol` or ABC with `delete_feature`, `process_updates`, `process_update`, a `connected` `@property`, `close`, `config_changed`.                                                                                                  |
| Dart     | `abstract class RawUpdateFeatureListener` (or `abstract interface class`) with `bool get connected;`.                                                                                                                                       |

Keep the methods **synchronous / fire-and-forget** in the interface (`void`, not `Future`/`Task`).
The repository never waits for listeners. A listener that does I/O starts its own async work and
handles its own errors (see §6).

---

## 4. Registration

### 4.1 Repository level

```ts
registerRawUpdateFeatureListener(listener): number   // returns a handle
removeRawUpdateFeatureListener(handle: number): void
```

Implementation in `ClientFeatureRepository`:

- Listeners are stored in an **ordered map `handle -> listener`** (`Map<number, RawUpdateFeatureListener>`).
- The handle comes from a **single repository-wide counter** (`_listenerCounter++`, starting at 1)
  that is also used for usage streams. Handles are unique per repository and never reused.
- Registering the same object twice gives two entries, and the listener gets every event twice. There is no dedupe.
- Removing an unknown handle does nothing.
- Registration does **not** replay current state to the new listener. A listener that needs the
  current state reads it itself or waits for the next full update.

### 4.2 Config level

`FeatureHubConfig.registerRawUpdateFeatureListener(listener): void` delegates to
`repository().registerRawUpdateFeatureListener(listener)`. If the config is already closed it does nothing.

Note that the config-level method **drops the handle** (returns `void`). A port should return the
handle at both levels so callers can deregister.

### 4.3 Close

When the repository closes (`ClientFeatureRepository.close()`, called from `EdgeFeatureHubConfig.forceClose()`):

```ts
[...this._rawUpdateListeners.values()].forEach((l) => void Promise.resolve().then(() => l.close()));
this._rawUpdateListeners.clear();
```

- Take a **snapshot** of the listeners first, because a listener's `close()` may call
  `removeRawUpdateFeatureListener` and change the map while it is being iterated.
- Call `close()` on each listener **asynchronously** (deferred), then clear the map at once.
- In order, the config's `forceClose` closes the edge (transport) services, then the usage adapter,
  then the repository (which closes the raw listeners).

---

## 5. Producers: who calls `notify`

All updates enter through one method on the internal repository:

```ts
notify(state: SSEResultState, data: unknown, source: string): void
```

| Producer              | `source` tag            | Events sent                                                      | Pre-processing                                                                                                                                      |
| --------------------- | ----------------------- | ---------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| SSE client            | `"streaming-service"`   | `features`, `feature`, `delete_feature`, `ack`, `bye`, `failure` | Sets `fs.environmentId = config.environmentId` on every FeatureState first. `config` events are handled by the SSE client and never reach `notify`. |
| REST poller           | `"polling-service"`     | `features` (flattened over all environments), `failure`          | Sets `f.environmentId = environment.id` on each feature.                                                                                            |
| Redis store           | `"redis-store"`         | `features`                                                       | Loads from Redis at init and when the SHA changes.                                                                                                  |
| Browser storage store | `"local-session-store"` | `features`                                                       | Loads from storage at construction and on `configChanged`.                                                                                          |

A port should keep these exact source strings. They are not shared across processes, but keeping
them the same makes logs and cross-SDK debugging consistent. New stores should pick a unique,
stable tag and define it as a constant.

---

## 6. Fan-out: what listeners receive and when

This is the core logic, from `ClientFeatureRepository.notify`. **The repository applies the update
to its own state first, then dispatches to raw listeners.** Each listener call is **scheduled
asynchronously** (`Promise.resolve().then(...)`, a microtask), so:

- the transport thread / event loop turn returns without waiting for listener I/O,
- an exception or slow listener cannot break the repository or other listeners,
- by the time a listener runs, the repository already holds the new state.

| Event (`state`)          | Repository action                                                                                                                                                                                                                                                                                                          | Raw listener dispatch                                                         |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `ack`, `bye`             | none                                                                                                                                                                                                                                                                                                                       | **none**                                                                      |
| `failure`                | readiness → Failed, broadcast (unless catch-and-release)                                                                                                                                                                                                                                                                   | **none**                                                                      |
| `delete_feature`         | `deleteFeature(fs)` (version-guarded, see below)                                                                                                                                                                                                                                                                           | `deleteFeature(fs, source)` to every listener                                 |
| `feature`                | catch-and-release: buffer it. Otherwise: `featureUpdate(fs)`, then fire "new state available" if it changed.                                                                                                                                                                                                               | `processUpdate(fs, source)` to every listener                                 |
| `features`               | Filter out entries with no `key`. Then **(a)** if the initial state was already received and catch-and-release is on: buffer the list. **(b)** Otherwise: upsert each, delete any held feature missing from the list, set Ready, broadcast readiness on the first time, or fire "new state available" if anything changed. | `processUpdates(filteredList, source)` to every listener in **both** branches |
| `config`, `error`, other | none                                                                                                                                                                                                                                                                                                                       | **none**                                                                      |

Important properties to keep:

1. **Raw means raw.** Listeners get the upstream data even when the repository did _not_ apply it
   (catch-and-release buffering, or a version older than the one held). The listener does its
   own version checks. This lets a store keep up to date while the in-memory repository is
   holding changes for a release.
2. **Implicit deletes are not sent individually.** When a full `features` list omits a feature the
   repository held, the repository deletes it internally but does **not** call
   `deleteFeature` on listeners. Listeners must treat `processUpdates` as full replacement.
3. **Shared payload.** Every listener gets the same list/object instances. Listeners must
   **not mutate** them. Copy before changing.
4. **Every source goes to every listener.** A `features` call injected by store A goes to store B
   (so stores sync with each other) and back to store A (which ignores it by tag).
5. **Injected state counts as real state.** If a store calls `notify(features, ..., tag)` before
   upstream connects, the repository becomes **Ready** and readiness listeners fire with
   `firstTimeReady = true`. This is the warm-start behaviour.

Repository-side version guards (useful context, not part of the listener contract):

- `featureUpdate`: ignore if `incoming.version < held.version`.
- `deleteFeature`: apply if `incoming.version` is missing, `== 0`, or `>= held.version`. Version 0
  means a real delete, or a feature created already retired.

### Async dispatch in other languages

The JS microtask fan-out gives "don't block the producer, isolate failures". Pick the
equivalent for the target language and **wrap every listener call in try/catch and log the error**
(in JS, a synchronous throw inside `.then` becomes an unhandled rejection, so do better than that):

| Language | Suggested dispatch                                                                                                                                                                          |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Go       | `go func(l RawUpdateFeatureListener) { defer recover-and-log; l.ProcessUpdate(fs, source) }(l)`, or a single dispatcher goroutine with a buffered channel if per-listener ordering matters. |
| C#       | `Task.Run(() => { try { l.ProcessUpdate(fs, source); } catch (Exception e) { log } })`                                                                                                      |
| Java     | A shared single-thread or small `ExecutorService` owned by the repository, shut down in `close()`.                                                                                          |
| Python   | Threaded SDK: `ThreadPoolExecutor.submit`. asyncio SDK: `loop.call_soon` / `asyncio.create_task` wrapping a sync call.                                                                      |
| Dart     | `scheduleMicrotask(() { try {...} catch (e) {...} })` or `Future.microtask`.                                                                                                                |

**Ordering:** in JS, microtasks run in FIFO order on one thread, so each listener sees events in
arrival order. A thread pool does not guarantee this. If the target SDK dispatches on several
threads, use **one ordered queue per listener** (or one single-threaded dispatcher) so
a `processUpdates` followed by a `processUpdate` is not seen in reverse order.

**Concurrency:** take a snapshot of the listener collection before iterating (or use a
concurrent/copy-on-write collection). Registration and removal can happen from other threads or from
inside a callback.

---

## 7. Lifecycle of a store listener

```
construct store(config, options)
   │
   ├─ (optional guard) refuse if config uses server-evaluated keys     ← Redis does this
   ├─ connect to backing store
   ├─ repository.registerRawUpdateFeatureListener(this)
   ├─ load saved state → repository.notify(FEATURES, saved, MY_TAG)    ← warm start
   ├─ start a refresh timer (shared stores only)
   │
config.init() / upstream connects
   │
   ├─ upstream notify(..., "streaming-service"|"polling-service")
   │      └─ repository applies → dispatches → store persists
   ├─ timer: if backing store changed externally → notify(FEATURES, data, MY_TAG)
   │      └─ repository applies → dispatches → store ignores its own tag; other stores persist
   │
config.close() → repository.close() → store.close()   (stop timer, mark inactive)
```

Register **before** `config.init()` so stored state is in the repository before upstream data
arrives and before the first readiness callbacks fire.

---

## 8. Reference listener implementations

### 8.1 Simple: per-client cache (`LocalSessionStore`)

- Tag `"local-session-store"`. Storage key = `config.featureUrl()`.
- In memory: `map[id] -> FeatureState`.
- `processUpdates`: if own tag, return. Otherwise replace the map with `{f.id: f}` and persist the whole map as JSON.
- `processUpdate`: if own tag, return. Otherwise `map[f.id] = f` and persist. (No version check.)
- `deleteFeature`: **no-op**. The next full `processUpdates` corrects the store. Removing by `id` is also fine.
- `connected`: always true.
- `configChanged`: if `featureUrl()` changed, switch key, clear the map, reload from the new key, and `notify`.
- `close`: no-op.
- Parse errors on load are ignored.

### 8.2 Distributed: shared cache across processes (`RedisSessionStore*`)

Cross-SDK contract: **every SDK language must use the same key layout and JSON format** so that a
Go, Java and Node service can share one Redis.

| Key                            | Value                                           |
| ------------------------------ | ----------------------------------------------- |
| `{prefix}_{environmentId}`     | JSON array of `FeatureState` (wire field names) |
| `{prefix}_{environmentId}_sha` | hex SHA-256 fingerprint of that array           |

- `prefix` defaults to `"featurehub"`. `environmentId` comes from the API key (`config.environmentId`).
- **Fingerprint:** sort features by `id` (ordinal string compare), map each to `"{id}:{version ?? 0}"`,
  join with `"|"`, SHA-256, lowercase hex. This must match byte for byte across languages.

Options (defaults): `prefix="featurehub"`, `backoffTimeout=500ms`, `retryUpdateCount=10`,
`refreshTimeout=300s`, `delayInit=false` (if false, the constructor starts `init()` in the background).

`init()`:

1. If there are API keys and they are **server-evaluated** (no `*`), log an error and **do not
   register**. Shared state would leak one context's evaluation into another.
2. Connect if not already open.
3. Register as raw listener.
4. `lastSha = GET sha`. If present, load the features and `notify(FEATURES, features, "redis-store")`.
5. Start the interval timer: `GET sha`. If it is non-null and `!= lastSha`, set `lastSha` and reload with `notify`.
6. `active = true`.

`connected` = `active && client.isOpen`. `close()` = `active=false`, stop the timer. `configChanged()` is a no-op.

Callbacks (each: return if own tag. Otherwise start async work, catch and log errors, never throw):

- `processUpdates(features)`: compute the SHA. If it equals the Redis SHA, skip. If Redis has data
  and **no** incoming feature is newer than its Redis copy (newer = `incoming.version >
stored.version`, where a feature missing from Redis counts as version -1), skip. Otherwise store.
- `processUpdate(f)`: read Redis. If it has the same `id` with `version >= f.version`, skip.
  Otherwise write `redis - {id} + f`.
- `deleteFeature(f)`: read Redis. If the `id` is absent, skip. Otherwise write `redis - {id}`.

Write (`storeFeatures`) with optimistic locking (single node):

```
for attempt in 0..retryCount:
    WATCH sha
    current = GET sha
    if current == shaToWrite: UNWATCH; lastSha = shaToWrite; return
    if current != null and current != lastSha:          # someone else wrote
        redisFeatures = read
        if redisFeatures != null:
            if no incoming feature is newer: UNWATCH; return    # we are stale
            toWrite = merge(toWrite, redisFeatures)              # per id, higher version wins
            recompute sha + json
    result = MULTI; SET sha; SET features; EXEC
    if result != null: lastSha = sha; return
    # null / WatchError → retry
    sleep(backoffTimeout)
log error "exhausted retries"
```

Any non-watch error: log and stop. **Cluster mode:** no WATCH/MULTI, just `SET sha` then
`SET features` (not atomic), then update `lastSha`.

---

## 9. Known gaps in the JS implementation (decide deliberately when porting)

These are tracked for the JS SDK in [featurehub-javascript-sdk#270](https://github.com/featurehub-io/featurehub-javascript-sdk/issues/270).

| Gap                                                                                                                 | Recommendation for ports                                                                                                |
| ------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `configChanged()` is declared but **nothing in core calls it**.                                                     | Either call it on all raw listeners when the API key / URL changes, or leave it out until it is needed. Document which. |
| `FeatureHubConfig.registerRawUpdateFeatureListener` returns `void`, losing the handle.                              | Return the handle (or a disposable/cancel function).                                                                    |
| Neither store calls `removeRawUpdateFeatureListener` in its own `close()`, although the READMEs say it deregisters. | Keep the handle and deregister in `close()`, as well as stopping timers.                                                |
| Listener exceptions inside the microtask are not caught by the repository.                                          | Wrap each dispatched call in try/catch with logging.                                                                    |
| Implicit deletes (missing from a full list) do not produce `deleteFeature` calls.                                   | Keep this (full-replacement semantics) but document it in the interface comments.                                       |
| The browser store's `deleteFeature` is a no-op.                                                                     | Removing by `id` is cheap and more correct, so prefer that.                                                             |

---

## 10. Port checklist

- [ ] `RawUpdateFeatureListener` interface with the six members and `source` on every data method.
- [ ] Repository holds an ordered `handle -> listener` collection, with a monotonic handle counter shared with other listener registries.
- [ ] `register…` returns a handle and `remove…` takes it. Expose both on the config as well.
- [ ] `notify` applies to repository state **first**, then fans out:
      `feature → processUpdate`, `features → processUpdates` (key-filtered list, including the catch-and-release branch),
      `delete_feature → deleteFeature`. Nothing for ack/bye/failure/config/error.
- [ ] Fan-out is non-blocking, isolates exceptions, keeps per-listener order, and iterates over a snapshot.
- [ ] Transports set `environmentId` on each FeatureState before `notify` and use the tags `"streaming-service"` / `"polling-service"`.
- [ ] Repository `close()` snapshots the listeners, calls `close()` on each without blocking, and clears the collection.
- [ ] Stores key by `id`, compare `version` (missing = 0), ignore their own tag, and inject with `notify(FEATURES, list, OWN_TAG)`.
- [ ] Shared stores refuse server-evaluated keys and use the Redis key/fingerprint format in §8.2 exactly.
- [ ] Tests: echo suppression; repository Ready after a store injects; listener gets raw updates in catch-and-release mode;
      older-version `processUpdate` ignored by the store; a throwing listener does not affect others; close calls listener close.
