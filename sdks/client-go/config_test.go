package client

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestConfig(t *testing.T) {

	// Make a new config with nothing set:
	config := &Config{}

	// First thing tested is APIKey:
	assert.Error(t, config.Validate())
	assert.Contains(t, config.Validate().Error(), "APIKey is required")
	config.APIKey = "some APIKey"

	// Next is EnvironmentID:
	assert.Error(t, config.Validate())
	assert.Contains(t, config.Validate().Error(), "EnvironmentID is required")
	config.EnvironmentID = "some EnvironmentID"

	// Next is ServerAddress:
	assert.Error(t, config.Validate())
	assert.Contains(t, config.Validate().Error(), "ServerAddress is required")
	config.ServerAddress = "some ServerAddress"

	// Now try a valid config:
	assert.NoError(t, config.Validate())
}
