package client

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/interfaces"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

// ClientWithContext bundles a Context with a client:
type ClientWithContext struct {
	*models.Context
	client interfaces.Client
	config *Config
}

// GetFeature searches for a feature by key:
func (cc *ClientWithContext) GetFeature(key string) (*models.FeatureState, error) {
	return cc.client.GetFeature(key)
}

// GetBoolean searches for a feature by key, returns the value as a boolean:
func (cc *ClientWithContext) GetBoolean(key string) (bool, error) {

	// Use the existing GetFeature method:
	fs, err := cc.client.GetFeature(key)
	if err != nil {
		return false, err
	}

	// Make sure the feature is the correct type:
	if fs.Type != models.TypeBoolean {
		return false, errors.NewErrInvalidType(string(fs.Type))
	}

	// Assert the value:
	defaultValue, ok := fs.Value.(bool)
	if !ok {
		return false, errors.NewErrInvalidType("Unable to assert value as a bool")
	}

	// Figure out which value to use:
	if calculatedValue := fs.Strategies.Calculate(cc.Context); calculatedValue != nil {

		// Assert the value:
		if strategyValue, ok := calculatedValue.(bool); ok {
			return strategyValue, nil
		}
	}

	// Return the default value as a fall-back:
	return defaultValue, nil
}

// GetNumber searches for a feature by key, returns the value as a float64:
func (cc *ClientWithContext) GetNumber(key string) (float64, error) {

	// Use the existing GetFeature method:
	fs, err := cc.client.GetFeature(key)
	if err != nil {
		return 0, err
	}

	// Make sure the feature is the correct type:
	if fs.Type != models.TypeNumber {
		return 0, errors.NewErrInvalidType(string(fs.Type))
	}

	// Assert the value:
	defaultValue, ok := fs.Value.(float64)
	if !ok {
		return 0, errors.NewErrInvalidType("Unable to assert value as a float64")
	}

	// Figure out which value to use:
	if calculatedValue := fs.Strategies.Calculate(cc.Context); calculatedValue != nil {

		// Assert the value:
		if strategyValue, ok := calculatedValue.(float64); ok {
			return strategyValue, nil
		}
	}

	// Return the default value as a fall-back:
	return defaultValue, nil
}

// GetRawJSON searches for a feature by key, returns the value as a JSON string:
func (cc *ClientWithContext) GetRawJSON(key string) (string, error) {

	// Use the existing GetFeature method:
	fs, err := cc.client.GetFeature(key)
	if err != nil {
		return "{}", err
	}

	// Make sure the feature is the correct type:
	if fs.Type != models.TypeJSON {
		return "{}", errors.NewErrInvalidType(string(fs.Type))
	}

	// Assert the value:
	defaultValue, ok := fs.Value.(string)
	if !ok {
		return "{}", errors.NewErrInvalidType("Unable to assert value as a string")
	}

	// Figure out which value to use:
	if calculatedValue := fs.Strategies.Calculate(cc.Context); calculatedValue != nil {

		// Assert the value:
		if strategyValue, ok := calculatedValue.(string); ok {
			return strategyValue, nil
		}
	}

	// Return the default value as a fall-back:
	return defaultValue, nil
}

// GetString searches for a feature by key, returns the value as a string:
func (cc *ClientWithContext) GetString(key string) (string, error) {

	// Use the existing GetFeature method:
	fs, err := cc.client.GetFeature(key)
	if err != nil {
		return "", err
	}

	// Make sure the feature is the correct type:
	if fs.Type != models.TypeString {
		return "", errors.NewErrInvalidType(string(fs.Type))
	}

	// Assert the value:
	defaultValue, ok := fs.Value.(string)
	if !ok {
		return "", errors.NewErrInvalidType("Unable to assert value as a string")
	}

	// Figure out which value to use:
	if calculatedValue := fs.Strategies.Calculate(cc.Context); calculatedValue != nil {

		// Assert the value:
		if strategyValue, ok := calculatedValue.(string); ok {
			return strategyValue, nil
		}
	}

	// Return the default value as a fall-back:
	return defaultValue, nil
}
