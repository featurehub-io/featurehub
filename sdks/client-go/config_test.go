package client

import (
	"testing"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/mocks"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

func TestConfig(t *testing.T) {

	// Make sure that our fluent API for NewConfig works as expected:
	newConfig := NewConfig("myserver", "mySDKKey").WithLogLevel(logrus.WarnLevel).WithWaitForData(true)
	assert.Equal(t, "myserver", newConfig.ServerAddress)
	assert.Equal(t, "mySDKKey", newConfig.SDKKey)
	assert.Equal(t, logrus.WarnLevel, newConfig.LogLevel)
	assert.True(t, newConfig.WaitForData)

	// Try to connect (it will of course fail):
	newConfig, err := newConfig.Connect()
	assert.Error(t, err)

	// Inject a fake client:
	newConfig.client = new(mocks.FakeClient)

	// Get a context, check that it inherited the correct attributes:
	newContext := newConfig.NewContext()
	assert.Equal(t, newConfig.client, newContext.client)
	assert.Equal(t, newConfig, newContext.config)
	assert.NotNil(t, newContext.Custom)

	// Now try WithContext:
	customContext := &models.Context{
		Userkey: "customContextKey",
	}
	withContext := newConfig.WithContext(customContext)
	assert.Equal(t, newConfig.client, withContext.client)
	assert.Equal(t, newConfig, withContext.config)
	assert.Equal(t, "customContextKey", withContext.Userkey)
}

func TestConfigValidation(t *testing.T) {

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
