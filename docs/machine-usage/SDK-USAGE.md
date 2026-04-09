# Usage Subsystem Specification

This document is a language-agnostic specification of the `usage` subsystem - currently this is isolated and 
further prompts will elicit integration steps.

NOTE: `Config` may refer to `EdgeFeatureHubConfig` depending on the SDK. 

---

## Overview

The usage subsystem provides a way to observe when feature flags are evaluated. 

These events are dispatched to registered **plugins** that can forward them to analytics, audit, or telemetry backends.

The system has four layers:

1. **Event types** — structured objects describing what happened (single feature read, collection of features, named collection).
2. **StreamableRepository** — the repository emits events to zero or more registered handlers.
3. **Adapter** — subscribes to the repository stream and fans events out to registered plugins.
4. **Plugin** — receives events and sends them to an external system.

---

## Core Types

### `ContextAttribute`

```
type ContextAttribute = string|number|boolean|undefined
```

### `ContextRecord`

```
type ContextRecord = map[string, ContextAttribute]
```

A free-form key→value map used for additional event data, context attributes, and plugin configuration. Keys and values are arbitrary. Used throughout as the serialisation target for events. The value can be stored as an arbitrary object or
interface if the language does not support union types.

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

| Field | Type | Description                                         |
|---|---|-----------------------------------------------------|
| `ID` | string | Immutable feature identifier (server-assigned)      |
| `Key` | string | Human-readable feature key (may change)             |
| `EnvironmentID` | string | The environment the feature belongs to              |
| `Value` | string | The RawValue converted to string via `ConvertFunc`  |
| `ValueType` | FeatureValueType | `boolean`, `string`, `number`, or `json`            |
| `RawValue` | any | The unconverted value as returned by the repository |

**Construction:**

by constructors in languages that support them, by New methods if they don't. 

- `NewUsageValueFromFeature(feature: FeatureState, RawValue)` — convenience wrapper that reads all fields from a `FeatureState`.

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

```
interface UsageEventWithFeature extends UsageEvent {
    GetFeature() → FeatureHubUsageValue
    SetFeature(feature: FeatureHubUsageValue)
    SetContextAttributes(contextAttributes: ContextRecord)
}
```

### `BaseWithFeature` (event name: `"feature"`) - implements `UsageEventWithFeature`

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
    
constructor or method call in a language with no constructors:
```
NewUsageEventWithFeature(feature: FeatureHubUsageValue, contextAttributes: ContextRecord, userKey: string) → BaseWithFeature
```

---

```
interface UsageEventFeaturesCollection extends UsageEvent {
   GetFeatureValues() -> Array<FeatureHubUsageValue>
   SetFeatureValues(featureValues: Array<FeatureHubUsageValue>) 
}
```

### `BaseFeaturesCollection` (event name: `"feature-collection"`) - implements `UsageEventFeaturesCollection`

Holds an array of `FeatureHubUsageValue`s representing a snapshot of all evaluated features. Extends `BaseUsageEvent`.

| Field | Description |
|---|---|
| `FeatureValues` | `[]FeatureHubUsageValue` |

`CollectUsageRecord()` merges:
1. `additionalData`
2. adds each feature as `featureKey → convertedValue`
3. adds fhub_keys string field as the all of the keys in features from FeatureValues joined by a comma
4. adds each feature as `featureKey_raw` -> rawValue

**Construction:**

as constructor or 
```
NewUsageFeaturesCollection() → BaseFeaturesCollection
```
---
has an interface `UsageEventFeaturesContext` that adds `SetContextAttributes(contextAttributes: ContextRecord)`
to `UsageEventFeaturesCollection`

### `BaseCollectionContext` (event name: `"feature-collection-context"`) implements interface  `UsageEventCollectionContext`

Extends `BaseFeaturesCollection` with context attributes. Used when you want both all feature values and the full context in one event.

| Field | Description |
|---|---|
| `ContextAttributes` | `ContextRecord` — the serialised context |

`CollectUsageRecord()` merges `BaseFeaturesCollection.CollectUsageRecord()` then overlays `ContextAttributes`.

**Construction:**
            
constructor or
```
NewUsageFeaturesCollectionContext(userKey: string, additionalData: ContextRecord) → BaseCollectionContext
```

---

has an interface `UsageEventNamedFeaturesContext` extends `UsageEventFeaturesContext` but adds  
`SetEventName(name: string)`.

### `UsageNamedFeaturesCollection` (event name: custom) implements `UsageEventNamedFeaturesContext`

Extends `BaseCollectionContext` with a caller-supplied event name. Use this when you want to tag a collection event with a semantic label (e.g. `"page-view"`, `"checkout"`).

| Field | Description |
|---|---|
| `name` | The custom event name returned by `EventName()` |

**Construction:**
                 
constructor or:
```
NewUsageNamedFeaturesCollection(name: string, userKey: string, additionalData: ContextRecord) → UsageNamedFeaturesCollection
```

---

## Plugin Interface

```
interface Plugin {
    DefaultPluginAttributes() → ContextRecord
    CanSendAsync -> boolean
    Send(event: UsageEvent)    
    Close()
}
```
                           
- `CanSendAsync` - this returns false if the UsageAdapter should wait for Send to complete. If it returns true, it
be dispatched in the background using a promise, goroutine, task or whatever the language supports for background
concurrent operations.
- `DefaultPluginAttributes()` — returns default attributes the plugin wants merged into events it receives. (Currently informational; the adapter does not call this automatically — plugins must apply their own defaults inside `Send`.)
- `Send(event)` — called with each usage event. Must return the (possibly enriched) context. Must not panic; the adapter catches panics and logs them, but a panicking plugin is still a bug.
- `Close()` - called by the UsageAdapter when Close is called on it (by the EdgeFeatureHubConfig)

Each plugin's `Send` call is dispatched in its own goroutine by the `Adapter`. Plugins must be safe for concurrent execution if they hold state.

---

## ProviderFactory / Provider

`ProviderFactory` is an injectable interface for constructing usage objects. The default implementation (`DefaultProvider`) simply delegates to the package-level constructors above.

```
interface ProviderFactory {
    NewUsageValueFromFeature(feature: FeatureState, value: ContextRecord) → FeatureHubUsageValue
    NewUsageFeature(feature, contextAttributes, userKey) → UsageEventWithFeature
    NewUsageEventFeaturesCollection() → UsageEventFeaturesCollection
    NewUsageEventFeaturesContext(userKey) → UsageEventFeaturesContext
    NewUsageEventNamedFeaturesContext(name, additionalData) → UsageEventNamedFeaturesContext
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

`ClientFeatureHubRepository` implements this interface. It stores handlers in a map keyed by integer ID. IDs are monotonically increasing. `EmitUsageEvent(event)` iterates the map and calls each handler synchronously (without holding any lock — streams change rarely and the SDK accepts the theoretical TOCTOU race for performance reasons).

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
- **`dispatch(event)`** — called by the repository for each event. Iterates the plugin list and, for each plugin, if CanSendAsync is true, will call `Send(event)` asynchronously and not wait for a response, and if `CanSendAsync` is false,
it waits for the `Send` call to finish. All calls however are wrapped in exception handling logic so failures do not
interrupt the flow of the SDK.

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

A built-in plugin registered automatically by `Config.SetRepository`. When the edge type is `PassiveRest` and a usage event of type `UsageEventWithFeature` is emitted, it calls `client.Poll()` to trigger a fresh feature fetch. This keeps the feature cache current without requiring the host to manually poll.

`DefaultPluginAttributes()` returns nil. `Send` calls `Poll()` and returns the context unchanged.

---

## Extending the Subsystem

### Registering a custom Plugin

```go
config.RegisterUsagePlugin(myPlugin)
```

This appends the plugin to the adapter's list. All future usage events will be dispatched to it. This is ignored
if the `Config` is closed.

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
