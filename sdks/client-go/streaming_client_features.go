package client

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

// GetFeature searches for a feature by key:
func (c *StreamingClient) GetFeature(key string) (*models.FeatureState, error) {
	c.featuresMutex.Lock()
	defer c.featuresMutex.Unlock()

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

	return feature.AsBoolean()
}

// GetNumber searches for a feature by key, returns the value as a float64:
func (c *StreamingClient) GetNumber(key string) (float64, error) {

	// Use the existing GetFeature method:
	feature, err := c.GetFeature(key)
	if err != nil {
		return 0, err
	}

	return feature.AsNumber()
}

// GetRawJSON searches for a feature by key, returns the value as a JSON string:
func (c *StreamingClient) GetRawJSON(key string) (string, error) {

	// Use the existing GetFeature method:
	feature, err := c.GetFeature(key)
	if err != nil {
		return "{}", err
	}

	return feature.AsRawJSON()
}

// GetString searches for a feature by key, returns the value as a string:
func (c *StreamingClient) GetString(key string) (string, error) {

	// Use the existing GetFeature method:
	feature, err := c.GetFeature(key)
	if err != nil {
		return "", err
	}

	return feature.AsString()
}
