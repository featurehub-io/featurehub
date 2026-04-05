# OTEL in SDKs 

You need to tailor the class names and projects for your SDK, otherwise the general concepts hold true and allow a pretty good implementation.
Always check it does what it is supposed to however.

## otel value interceptor
Create an OpenTelemetryFeatureInterceptor in the FeatureHubUsageOpenTelemetry project which implements the FeatureHubSDK FeatureValueInterceptor (ExtendedFeatureValueInterceptor in java) interface. 
- It should take an optional parameter indicating whether it should allow overrides of locked values, and if it isn't provided, it should check for an environment variable called FEATUREHUB_OTEL_ALLOW_LOCKED_OVERRIDE being true. It will assume false if not.
- It should use the opentelemetry baggage api, and see if there is an field
called `fhub`. If so, it will be a comma separated list of feature=url-encoded-value fields in alphabetical order. If the feature is
not provided it should return (matched=false,value=null). If overrides are not allowed and the feature is locked, it should return (matched=false,value=null).
If it matches a feature in the list, it should be converted into the type
indicated by the FeatureValueType field of featureState and returned with a (matched=true,value=converted-value) (conversion in Java is done using the Conversion.toTypedValue, otherwise
it should abort returning (matched=false,value=null). It should abort the search early if the key it is looking for is greater alphabetically that the one it is looking at in the loop. 
 
## otel - reverse of otel interceptor
Create a OpenTelemetryBaggagePlugin which implements the FeatureHubSDK UsagePlugin interface.
It should be doing the reverse of the OpenTelemetryFeatureInterceptor by setting the fhub baggage field.
If it sees an UsageEventWithFeature it should update a single feature, if it sees an UsageFeaturesCollection it should update
all of the features (the keys are stored in `fhub_keys` in the dictionary, and each key is stored in the dictionary with its value).
Use the `_raw` value of the key, so `{key}_raw` rather than straight `key`, so there is no data loss. Remember the value can be null
for non boolean features. Always make sure the keys are stored in alphabetical order when writing them to fhub in the baggage. This must be called synchronously
but the plugin system (it cannot be called async). In the OpenTelemetryBaggagePlugin, if the value being written is different to the value that is already there, it should be rejected and a warning message should be logged to indicate that an attempt was made to overwrite an existing value and that the interceptor has not been registered correctly. 

## readme doc updates

The combination of the OpenTelemetryBaggagePlugin and the OpenTelemetryFeatureInterceptor the is what we call "Full-trace Feature Value consistency support". It is intended to ensure that from the beginning of the trace when the feature values are first evaluated they will remain at the original values they were determined at, even if feature value changes come from the server or the code misconfigures a context to be inconsistent along the trace. The values move when a request moves from one microservice to another, to eventing systems and processed elsewhere, so feature value will remain consistent. As long as every application wires up these pair it will ensure consistency. Can you validate that is true? 
                              
## otel - tracking plugin, this attaches state to the trace

Create an OpenTelemetryTrackerUsagePlugin in the FeatureHubUsageOpenTelemetry project which implements the FeatureHubSDK UsagePlugin interface.
It should take in its constructor a `prefix` string (defaults to `featurehub.`) and a boolean field called `attachAsSpanEvents` which
defaults to `false`. It should store those as class level variables. When it receives the `send` method call, it should check if the
UsageEvent implements the `IUsageEventName`, and if so it should use the CopyBaseMap() method to create a new map,and get the name
of the event using the `GetName()` method (from IUsageEventName). It now needs to create OpenTelemetry API Attribute objects,
and to do so, it should then iterate through the map, using the keys as the Attribute key, and for the value it should determine
the type of the value field and use the appropriate OpenTelemetry API AttributeValue object. Values might be arrays. Once it has built up
all of the attributes, if the attachAsSpanEvents field is true, it should add a new event to the current span with the name being the
prefix from the constructor plus the name of the event. If attachAsSpanEvents is false, it should set the attributes on the current span,
renaming the key fields to include the prefix. Should be synchronous as the span context being modified should be inline with the current
request. 
