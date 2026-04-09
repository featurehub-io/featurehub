The is tracked every time the SDK reads a feature value in a FeatureStateHolder (which usually holds the FeatureState) - via the getValue call. The getValue call will normally follow this pattern having been called with two parameters `type` which is an optional FeatureValueType
and `triggerUsage` which is an optional bool:
- if the type is null/nil it will attempt to get the type from the featureState object stored in the FeatureStateHolder.
- if this fails it will return null/nil.
- if the type exists it will call the repository getting it to iterate over its FeatureValueInterceptors using the
  FeatureStateHolder key and passing the FeatureState if there is one.
- if the value is matched, if the triggerUsage is true and there is a featureState, it will call the `used` method on
  the FeatureStateHolder (passing the value and featureState), otherwise it will simply return the matched value.
- if there is no matching interceptor, it will check if the featureState is nul or the featureState's type is not the
  same type as that requested and if so, it will return nil/null/undefined.
- if next checks if there is a FeatureHub context in the FeatureStateHolder and if the `strategies` array has any values
  and if so, it 



## Integration with Context

`Context` is the context-aware feature evaluation layer. It emits a `BaseWithFeature` event on every successful feature read.

### Single-feature read (`used`)

After any `GetBoolean`, `GetNumber`, `GetString`, `GetRawJSON` call succeeds:

1. Obtain the user key from the context (`cc.UniqueKey()`).
2. Build a `FeatureHubUsageValue` from the resolved feature state and evaluated value.
3. Call `UsageProvider().NewUsageFeature(usageValue, contextAttributes, userKey)` to create a `BaseWithFeature`.
4. Emit via `RecordUsageEvent(event)` → `featureRepository.EmitUsageEvent(fillEvent(event))`.

### `fillEvent`

Before emitting, `fillEvent` enriches any event type:

1. Sets `userKey` if `cc.UniqueKey()` returns a key.
2. If the event implements `FeaturesCollection`, sets its `FeatureValues` to the full list of all evaluated features in the current context (by calling every typed getter for every known feature key).
3. If the event implements `CollectionContext`, sets its `ContextAttributes` to the full serialised context.

### `RecordUsageEvent(event)`

Calls `fillEvent` then `featureRepository.EmitUsageEvent(event)`. For use when a plugin or host code wants to emit an arbitrary event through the same pipeline.

### `GetContextUsage(ctx)` → `UsageEvent`

Builds and returns (but does **not** emit) a `BaseCollectionContext` filled with the current context and all feature values. The caller can pass this to `RecordUsageEvent` or inspect it directly.

### `RecordNamedUsage(name, additionalParams)`

Creates a `UsageNamedFeaturesCollection` with the given name and additional data, fills it via `fillEvent`, then emits it.

---

## Context Attributes in Events

The full context (`contextAttributes`) that is attached to `BaseWithFeature` and `BaseCollectionContext` events is produced by serialising the `models.Context` into a flat `ContextRecord`. This includes all standard fields (userKey, sessionKey, country, platform, device, version) and all custom attributes. The exact serialisation mirrors the format used by `Context.GenerateHeader()` (sorted, URL-encoded key=value pairs) but as a map rather than a string.

`UniqueKey()` on `models.Context` returns the most specific identifier available: `userKey` first, then `sessionKey`. If neither is set, the event's `userKey` field remains empty.

---

## Thread Safety

- `activeConvert` (the global `ConvertFunc`) is protected by a `sync.RWMutex`. Reads (during value conversion) take a read lock; `SetConvertFunc` takes a write lock.
- `usageStreams` map in `ClientFeatureHubRepository` - no mutex required. `EmitUsageEvent` iterates the map **without** acquiring the mutex — handlers change rarely and the race is accepted by design.
- `Adapter.plugins` slice is not individually mutex-guarded — plugins are registered before the adapter is active (at startup), and `RegisterPlugin` is not expected to be called concurrently from multiple goroutines after `NewAdapter` returns.

---

## Sequence Diagram: Single Feature Read

```
host code
  → fhContext.GetBoolean("my-flag")
      -> fhContext.GetFeature("my-flag")
        -> 
      → fhContext.getInternalBoolean("my-flag", true)
          → featureRepository.GetInternalBoolean("my-flag")
              → [intercept / strategy evaluation / direct value]
          ← (featureState, matched, value, err)
          → cc.used("my-flag", featureState, value)
              → UsageProvider.NewUsageFeature(usageValue, contextAttrs, userKey)
              → RecordUsageEvent(event)
                  → featureRepository.EmitUsageEvent(event)
                      → StreamHandler(event)   [for each registered stream]
                          → Adapter.dispatch(event)
                              → goroutine: plugin.Send(event)
```
