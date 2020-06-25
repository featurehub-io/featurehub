package client

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

// GetFeature searches for a feature by key:
func (c *StreamingClient) GetFeature(key string) (*models.FeatureState, error) {
	c.mutex.Lock()
	defer c.mutex.Unlock()

	// Look for the feature:
	if feature, ok := c.features[key]; ok {
		c.logger.WithField("key", key).Trace("Found feature")
		return feature, nil
	}

	c.logger.WithField("key", key).Trace("Feature not found")
	return nil, errors.NewErrFeatureNotFound(key)
}

// GetBoolean searches for a feature by key, returns the value as a boolean:
func (c *StreamingClient) GetBoolean(key string) (bool, error) {

	// Use the existing GetFeature method:
	feature, err := c.GetFeature(key)
	if err != nil {
		return false, err
	}

	// Make sure the feature is the correct type:
	if feature.Type != models.TypeBoolean {
		return false, errors.NewErrInvalidType(string(feature.Type))
	}

	// Assert the value:
	if value, ok := feature.Value.(bool); ok {
		return value, nil
	}

	return false, errors.NewErrInvalidType("Unable to assert value as a bool")
}

// GetNumber searches for a feature by key, returns the value as a float64:
func (c *StreamingClient) GetNumber(key string) (float64, error) {

	// Use the existing GetFeature method:
	feature, err := c.GetFeature(key)
	if err != nil {
		return 0, err
	}

	// Make sure the feature is the correct type:
	if feature.Type != models.TypeNumber {
		return 0, errors.NewErrInvalidType(string(feature.Type))
	}

	// Assert the value:
	if value, ok := feature.Value.(float64); ok {
		return value, nil
	}

	return 0, errors.NewErrInvalidType("Unable to assert value as a float64")
}

// GetRawJSON searches for a feature by key, returns the value as a JSON string:
func (c *StreamingClient) GetRawJSON(key string) (string, error) {

	// Use the existing GetFeature method:
	feature, err := c.GetFeature(key)
	if err != nil {
		return "{}", err
	}

	// Make sure the feature is the correct type:
	if feature.Type != models.TypeJSON {
		return "{}", errors.NewErrInvalidType(string(feature.Type))
	}

	// Assert the value:
	if value, ok := feature.Value.(string); ok {
		return value, nil
	}

	return "{}", errors.NewErrInvalidType("Unable to assert value as a string")
}

// GetString searches for a feature by key, returns the value as a string:
func (c *StreamingClient) GetString(key string) (string, error) {

	// Use the existing GetFeature method:
	feature, err := c.GetFeature(key)
	if err != nil {
		return "", err
	}

	// Make sure the feature is the correct type:
	if feature.Type != models.TypeString {
		return "", errors.NewErrInvalidType(string(feature.Type))
	}

	// Assert the value:
	if value, ok := feature.Value.(string); ok {
		return value, nil
	}

	return "", errors.NewErrInvalidType("Unable to assert value as a string")
}
