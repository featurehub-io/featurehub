package client

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestConfig(t *testing.T) {

	// Make a new config with nothing set:
	config := &Config{}

	// First thing tested is SDKKey:
	assert.Error(t, config.Validate())
	assert.Contains(t, config.Validate().Error(), "SDKKey is required")
	config.SDKKey = "some SDKKey"

	// Next is the format of the SDKKey:
	assert.Error(t, config.Validate())
	assert.Contains(t, config.Validate().Error(), "Invalid SDKKey format")
	config.SDKKey = "default/environment-id/my-secret-api-key"

	// Next is ServerAddress:
	assert.Error(t, config.Validate())
	assert.Contains(t, config.Validate().Error(), "ServerAddress is required")
	config.ServerAddress = "http://streams.test:8086"

	// Check that we can build the correct Features URL:
	featuresURL := config.featuresURL()
	assert.Equal(t, "http://streams.test:8086/features/default/environment-id/my-secret-api-key", featuresURL)

	// Now try a valid config:
	assert.NoError(t, config.Validate())
}
