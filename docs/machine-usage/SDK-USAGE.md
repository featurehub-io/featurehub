# Usage Subsystem Specification

This document is a language-agnostic specification of the `pkg/usage` subsystem and how it integrates with the rest of the FeatureHub SDK. It is intended to serve as a reference for reimplementing the subsystem in any language.

---

## Overview

The usage subsystem provides a way to observe when feature flags are evaluated. Every time the SDK reads a feature value through a context-aware API, it emits a **usage event** describing what was read and by whom. These events are dispatched to registered **plugins** that can forward them to analytics, audit, or telemetry backends.

The system has four layers:

1. **Event types** — structured objects describing what happened (single feature read, collection of features, named collection).
2. **StreamableRepository** — the repository emits events to zero or more registered handlers.
3. **Adapter** — subscribes to the repository stream and fans events out to registered plugins.
4. **Plugin** — receives events and sends them to an external system.

---

## Core Types

### `ContextRecord`

```
type ContextRecord = map[string, any]
```

A free-form key→value map used for additional event data, context attributes, and plugin configuration. Keys and values are arbitrary. Used throughout as the serialisation target for events.

---

### `ConvertFunc`

```
type ConvertFunc = func(value: any, valueType: FeatureValueType) → string
```

A global, replaceable function that converts a raw feature value to its string representation for inclusion in usage records. The default implementation:

- `boolean` → `"on"` (true) or `"off"` (false)
- `string` → the value unchanged
- `number` → formatted with the minimum decimal digits needed (e.g. `"1"`, `"3.14"`)
- `nil` / unknown type → `""` (empty string; the field is omitted from the record)

**`SetConvertFunc(fn)`** — replaces the global converter. Pass `nil` to restore the default. The replacement is protected by a read-write lock so it is safe to swap at runtime.

---

### `FeatureHubUsageValue`

Holds everything the SDK knows about a single feature at the moment of evaluation.

| Field | Type | Description |
|---|---|---|
| `ID` | string | Immutable feature identifier (server-assigned) |
| `Key` | string | Human-readable feature key (may change) |
| `EnvironmentID` | string | The environment the feature belongs to |
| `Value` | string | The feature value converted to string via `ConvertFunc` |
| `ValueType` | FeatureValueType | `boolean`, `string`, `number`, or `json` |
| `RawValue` | any | The unconverted value as returned by the repository |

**Construction:**

- `NewUsageValue(id, key, environmentID, value, valueType)` — builds from individual fields, applying the active `ConvertFunc`.
- `NewUsageValueFromFeature(feature: FeatureState)` — convenience wrapper that reads all fields from a `FeatureState`.

---

## Event Types

All event types implement the `UsageEvent` interface:

```
interface UsageEvent {
    UserKey() → string
    SetUserKey(userKey: string)
    CollectUsageRecord() → ContextRecord
}
```

`CollectUsageRecord()` returns a flat `ContextRecord` representing all data in the event, suitable for sending to an external system.

---

### `BaseUsageEvent`

Base type embedded by all concrete event types.

| Field | Description |
|---|---|
| `userKey` | The unique user/session identifier (may be empty) |
| `additionalData` | Optional free-form `ContextRecord` merged into `CollectUsageRecord()` |

`CollectUsageRecord()` returns a shallow copy of `additionalData`.

`SetAdditionalData(data)` replaces `additionalData`; a nil argument is normalised to an empty map.

---

### `BaseWithFeature` (event name: `"feature"`)

Emitted every time a single feature is read through a context-aware API. Extends `BaseUsageEvent`.

| Field | Description |
|---|---|
| `contextAttributes` | The full serialised context at the time of the read (`ContextRecord`) |
| `feature` | The `FeatureHubUsageValue` for the feature that was read |

`CollectUsageRecord()` merges:
1. `additionalData` (from `BaseUsageEvent`)
2. `contextAttributes` (all keys from the context)
3. `"feature"` → `feature.Key`
4. `"value"` → `feature.Value` (converted string)
5. `"id"` → `feature.ID`
6. `"environmentId"` → `feature.EnvironmentID` (only if non-empty)

**Construction:**

```
NewUsageEventWithFeature(feature: FeatureHubUsageValue, contextAttributes: ContextRecord, userKey: string) → BaseWithFeature
```

---

### `BaseFeaturesCollection` (event name: `"feature-collection"`)

Holds an array of `FeatureHubUsageValue`s representing a snapshot of all evaluated features. Extends `BaseUsageEvent`.

| Field | Description |
|---|---|
| `FeatureValues` | `[]FeatureHubUsageValue` |

`CollectUsageRecord()` merges `additionalData` then adds each feature as `featureKey → convertedValue`.

**Construction:**

```
NewUsageFeaturesCollection() → BaseFeaturesCollection
```

---

### `BaseCollectionContext` (event name: `"feature-collection-context"`)

Extends `BaseFeaturesCollection` with context attributes. Used when you want both all feature values and the full context in one event.

| Field | Description |
|---|---|
| `ContextAttributes` | `ContextRecord` — the serialised context |

`CollectUsageRecord()` merges `BaseFeaturesCollection.CollectUsageRecord()` then overlays `ContextAttributes`.

**Construction:**

```
NewUsageFeaturesCollectionContext(userKey: string, additionalData: ContextRecord) → BaseCollectionContext
```

---

### `UsageNamedFeaturesCollection` (event name: custom)

Extends `BaseCollectionContext` with a caller-supplied event name. Use this when you want to tag a collection event with a semantic label (e.g. `"page-view"`, `"checkout"`).

| Field | Description |
|---|---|
| `name` | The custom event name returned by `EventName()` |

**Construction:**

```
NewUsageNamedFeaturesCollection(name: string, userKey: string, additionalData: ContextRecord) → UsageNamedFeaturesCollection
```

---

## Plugin Interface

```
interface Plugin {
    DefaultPluginAttributes() → ContextRecord
    Send(ctx: Context, event: UsageEvent) → Context
}
```

- `DefaultPluginAttributes()` — returns default attributes the plugin wants merged into events it receives. (Currently informational; the adapter does not call this automatically — plugins must apply their own defaults inside `Send`.)
- `Send(ctx, event)` — called with each usage event. Must return the (possibly enriched) context. Must not panic; the adapter catches panics and logs them, but a panicking plugin is still a bug.

Each plugin's `Send` call is dispatched in its own goroutine by the `Adapter`. Plugins must be safe for concurrent execution if they hold state.

---

## ProviderFactory / Provider

`ProviderFactory` is an injectable interface for constructing usage objects. The default implementation (`DefaultProvider`) simply delegates to the package-level constructors above.

```
interface ProviderFactory {
    NewUsageValue(id, key, environmentID, value, valueType) → FeatureHubUsageValue
    NewUsageValueFromFeature(feature: FeatureState) → FeatureHubUsageValue
    NewUsageFeature(feature, contextAttributes, userKey) → BaseWithFeature
    NewUsageCollectionEvent() → BaseFeaturesCollection
    NewUsageContextCollectionEvent(userKey) → BaseCollectionContext
    NewNamedUsageCollection(name, additionalData) → UsageNamedFeaturesCollection
}
```

The factory is held by `ClientFeatureHubRepository` and accessed via `UsageProvider()`. Replacing the factory (e.g. by setting a different `ProviderFactory` on the repository) allows tests or alternative implementations to control what objects are created.

---

## StreamableRepository

```
interface StreamableRepository {
    RegisterUsageStream(handler: StreamHandler) → int   // returns a numeric handler ID
    RemoveUsageStream(id: int)
}
```

`StreamHandler` is:
```
type StreamHandler = func(ctx: Context, event: UsageEvent)
```

`ClientFeatureHubRepository` implements this interface. It stores handlers in a map keyed by integer ID. IDs are monotonically increasing. `EmitUsageEvent(ctx, event)` iterates the map and calls each handler synchronously (without holding any lock — streams change rarely and the SDK accepts the theoretical TOCTOU race for performance reasons).

---

## Adapter

The `Adapter` bridges the repository's stream interface and the plugin list.

```
class Adapter {
    constructor(repository: StreamableRepository, logger: Logger)
    RegisterPlugin(plugin: Plugin)
    Close()
}
```

- **Construction:** subscribes to the repository by calling `RegisterUsageStream(dispatch)`, storing the returned handler ID.
- **`RegisterPlugin`** — adds a plugin. Plugins are stored in an ordered list; all plugins receive every event.
- **`Close`** — unregisters the adapter from the repository via `RemoveUsageStream(handlerID)`.
- **`dispatch(ctx, event)`** — called by the repository for each event. Iterates the plugin list and, for each plugin, spawns a goroutine that calls `plugin.Send(ctx, event)`. Each goroutine has a `recover()` that logs panics without propagating them.

**Ownership:** `Config.SetRepository(repo)` creates a new `Adapter` for the repository and closes any previous one. The config itself registers a built-in `passiveRestPollPlugin` on every adapter it creates.

---

## Integration with Config and Repository

### `Config.SetRepository`

When a new repository is installed on `Config`:
1. The previous `Adapter` is closed (unsubscribes from the old repository).
2. A new `Adapter` is created for the new repository.
3. The built-in `passiveRestPollPlugin` is registered on the new adapter.

```
config.SetRepository(repo)
config.RegisterUsagePlugin(myPlugin)   // adds to the adapter
```

### `passiveRestPollPlugin`

A built-in plugin registered automatically by `Config.SetRepository`. When the edge type is `EdgePassiveRest` and a usage event is emitted, it calls `client.Poll()` to trigger a fresh feature fetch. This keeps the feature cache current without requiring the host to manually poll.

`DefaultPluginAttributes()` returns nil. `Send` calls `Poll()` and returns the context unchanged.

---

## Integration with ClientWithContext

`ClientWithContext` is the context-aware feature evaluation layer. It emits a `BaseWithFeature` event on every successful feature read.

### Single-feature read (`used`)

After any `GetBoolean`, `GetNumber`, `GetString`, `GetRawJSON` call succeeds:

1. Obtain the user key from the context (`cc.UniqueKey()`).
2. Build a `FeatureHubUsageValue` from the resolved feature state and evaluated value.
3. Call `UsageProvider().NewUsageFeature(usageValue, contextAttributes, userKey)` to create a `BaseWithFeature`.
4. Emit via `RecordUsageEvent(ctx, event)` → `featureRepository.EmitUsageEvent(ctx, fillEvent(ctx, event))`.

### `fillEvent`

Before emitting, `fillEvent` enriches any event type:

1. Sets `userKey` if `cc.UniqueKey()` returns a key.
2. If the event implements `FeaturesCollection`, sets its `FeatureValues` to the full list of all evaluated features in the current context (by calling every typed getter for every known feature key).
3. If the event implements `CollectionContext`, sets its `ContextAttributes` to the full serialised context.

### `RecordUsageEvent(ctx, event)`

Calls `fillEvent` then `featureRepository.EmitUsageEvent(ctx, event)`. For use when a plugin or host code wants to emit an arbitrary event through the same pipeline.

### `GetContextUsage(ctx)` → `UsageEvent`

Builds and returns (but does **not** emit) a `BaseCollectionContext` filled with the current context and all feature values. The caller can pass this to `RecordUsageEvent` or inspect it directly.

### `RecordNamedUsage(ctx, name, additionalParams)`

Creates a `UsageNamedFeaturesCollection` with the given name and additional data, fills it via `fillEvent`, then emits it.

---

## Context Attributes in Events

The full context (`contextAttributes`) that is attached to `BaseWithFeature` and `BaseCollectionContext` events is produced by serialising the `models.Context` into a flat `ContextRecord`. This includes all standard fields (userKey, sessionKey, country, platform, device, version) and all custom attributes. The exact serialisation mirrors the format used by `Context.GenerateHeader()` (sorted, URL-encoded key=value pairs) but as a map rather than a string.

`UniqueKey()` on `models.Context` returns the most specific identifier available: `userKey` first, then `sessionKey`. If neither is set, the event's `userKey` field remains empty.

---

## Thread Safety

- `activeConvert` (the global `ConvertFunc`) is protected by a `sync.RWMutex`. Reads (during value conversion) take a read lock; `SetConvertFunc` takes a write lock.
- `usageStreams` map in `ClientFeatureHubRepository` is guarded by `usageStreamsMu` for registration and removal. `EmitUsageEvent` iterates the map **without** acquiring the mutex — handlers change rarely and the race is accepted by design.
- `Adapter.plugins` slice is not individually mutex-guarded — plugins are registered before the adapter is active (at startup), and `RegisterPlugin` is not expected to be called concurrently from multiple goroutines after `NewAdapter` returns.

---

## Sequence Diagram: Single Feature Read

```
host code
  → fhContext.GetBoolean(ctx, "my-flag")
      → ClientWithContext.GetBoolean(ctx, "my-flag")
          → featureRepository.GetInternalBoolean(ctx, "my-flag")
              → [intercept / strategy evaluation / direct value]
          ← (featureState, matched, value, err)
          → cc.used(ctx, "my-flag", featureState, value)
              → UsageProvider.NewUsageFeature(usageValue, contextAttrs, userKey)
              → RecordUsageEvent(ctx, event)
                  → featureRepository.EmitUsageEvent(ctx, event)
                      → StreamHandler(ctx, event)   [for each registered stream]
                          → Adapter.dispatch(ctx, event)
                              → goroutine: plugin.Send(ctx, event)
```

---

## Extending the Subsystem

### Registering a custom Plugin

```go
config.RegisterUsagePlugin(myPlugin)
```

This appends the plugin to the adapter's list. All future usage events will be dispatched to it.

### Replacing the value converter

```go
usage.SetConvertFunc(func(value interface{}, t models.FeatureValueType) string {
    // custom conversion
})
```

Call with `nil` to restore the default.

### Replacing the ProviderFactory

Inject a custom `ProviderFactory` into `ClientFeatureHubRepository.usageProvider`. This allows full control over what event objects are created.

### Emitting custom events

```go
// From a context:
ctx.RecordNamedUsage(goCtx, "checkout", usage.ContextRecord{"cart_size": 3})

// Or build and emit directly:
event := provider.NewUsageContextCollectionEvent(userKey)
ctx.RecordUsageEvent(goCtx, event)
```
