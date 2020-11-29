package models

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
)

// FeatureState defines model for FeatureState.
type FeatureState struct {
	ID            string           `json:"id,omitempty"`         // ID
	Key           string           `json:"key,omitempty"`        // Name of the feature
	Strategies    Strategies       `json:"strategies,omitempty"` // Rollout strategy
	Type          FeatureValueType `json:"type,omitempty"`       // Data type
	Value         interface{}      `json:"value,omitempty"`      // the current value
	Version       int64            `json:"version,omitempty"`    // Version
	clientContext *Context         // ClientContext to apply to this FeatureState
}

// WithContext adds a client context to a featurestate:
func (fs *FeatureState) WithContext(context *Context) *FeatureState {
	fs.clientContext = context
	return fs
}

// AsBoolean returns a boolean value for this feature:
func (fs *FeatureState) AsBoolean() (bool, error) {

	// Make sure the feature is the correct type:
	if fs.Type != TypeBoolean {
		return false, errors.NewErrInvalidType(string(fs.Type))
	}

	// Assert the value:
	value, ok := fs.Value.(bool)
	if !ok {
		return false, errors.NewErrInvalidType("Unable to assert value as a bool")
	}

	// Return the default value through the boolean rollout strategy:
	return fs.Strategies.boolean(value, fs.clientContext), nil
}

// AsNumber returns a number value for this feature:
func (fs *FeatureState) AsNumber() (float64, error) {

	// Make sure the feature is the correct type:
	if fs.Type != TypeNumber {
		return 0, errors.NewErrInvalidType(string(fs.Type))
	}

	// Assert the value:
	value, ok := fs.Value.(float64)
	if !ok {
		return 0, errors.NewErrInvalidType("Unable to assert value as a float64")
	}

	// Handle client-side rollout strategies:
	if fs.clientContext != nil {
	}

	return value, nil
}

// AsRawJSON returns a raw JSON value for this feature:
func (fs *FeatureState) AsRawJSON() (string, error) {

	// Make sure the feature is the correct type:
	if fs.Type != TypeJSON {
		return "{}", errors.NewErrInvalidType(string(fs.Type))
	}

	// Assert the value:
	value, ok := fs.Value.(string)
	if !ok {
		return "{}", errors.NewErrInvalidType("Unable to assert value as a string")
	}

	// Handle client-side rollout strategies:
	if fs.clientContext != nil {
	}

	return value, nil
}

// AsString returns a string value for this feature:
func (fs *FeatureState) AsString() (string, error) {

	// Make sure the feature is the correct type:
	if fs.Type != TypeString {
		return "", errors.NewErrInvalidType(string(fs.Type))
	}

	// Assert the value:
	value, ok := fs.Value.(string)
	if !ok {
		return "", errors.NewErrInvalidType("Unable to assert value as a string")
	}

	// Handle client-side rollout strategies:
	if fs.clientContext != nil {
	}

	return value, nil
}
