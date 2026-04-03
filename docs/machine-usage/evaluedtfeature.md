into the core project, add a new class called EvaluatedFeature.

It should take define 3 fields that will not change and should be private on construction:

- `value` which is a ContextAttribute,
- `strategyId` which is a `string | undefined`,
- `featureState` which is `FeatureState|undefined`.

- It needs to have several creation patterns and a private constructor. These patterns should be enforced by types so
  incorrect usage will fail at compile time.
- if the featurestate, value and strategy id are passed, the featurestate must not be undefined
- if the feature state and value are passed but not the strategyid, then the featurestate can be undefined
- if just the value is passed, we assume an undefined featurestate and strategyid
- if just the featurestate is passed, it must not be undefined and we take the value from the "value" field of the feature state (and the strategyid is undefined)

It should have getters for all fields to make field access simple.

It should have a toString() method which returns the string values of the value (if any), strategyId and featureState key (if not undefined).

It should have a convenience hasValue() method which returns true only if the value field is not undefined. 

