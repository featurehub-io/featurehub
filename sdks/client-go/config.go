package client

import (
	"fmt"
	"strings"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/interfaces"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/sirupsen/logrus"
)

const (
	defaultLogLevel   = logrus.InfoLevel
	defaultNamedCache = "default"
)

// Config defines parameters for the client:
type Config struct {
	LogLevel      logrus.Level // Logging level (default is "info")
	SDKKey        string       // SDK key (copied from the UI), in the format "{namedCache}/environmentID/APIKey"
	ServerAddress string       // FeatureHub API endpoint
	WaitForData   bool         // New() will block until some data has arrived
	client        interfaces.Client
}

// NewConfig returns a configured Config:
func NewConfig(serverAddress, sdkKey string) *Config {
	return &Config{
		LogLevel:      defaultLogLevel,
		SDKKey:        sdkKey,
		ServerAddress: serverAddress,
	}
}

// Connect prepares a client and connects to the configured FH server:
func (c *Config) Connect() (*Config, error) {

	// Get a client:
	client, err := NewStreamingClient(c)
	if err != nil {
		return c, err
	}

	// Start the client:
	client.Start()

	c.client = client
	return c, nil
}

// NewContext returns a ClientWithContext, with default context values:
func (c *Config) NewContext() *ClientWithContext {
	return &ClientWithContext{
		Context: &models.Context{
			Custom: make(map[string]interface{}),
		},
		client: c.client,
		config: c,
	}
}

// Validate can be called to check various config options:
func (c *Config) Validate() error {

	// LogLevel shouldn't be empty:
	if c.LogLevel == 0 {
		c.LogLevel = logrus.InfoLevel
	}

	// SDKKey shouldn't be empty:
	if len(c.SDKKey) == 0 {
		return errors.NewErrBadConfig("SDKKey is required")
	}

	// SDKKey should be 3 strings delimited with a slash:
	if len(strings.Split(c.SDKKey, "/")) < 2 {
		return errors.NewErrBadConfig("Invalid SDKKey format")
	}

	// ServerAddress shouldn't be empty:
	if len(c.ServerAddress) == 0 {
		return errors.NewErrBadConfig("ServerAddress is required")
	}

	return nil
}

// WithContext returns a ClientWithContext:
func (c *Config) WithContext(context *models.Context) *ClientWithContext {
	return &ClientWithContext{
		Context: context,
		client:  c.client,
		config:  c,
	}
}

// WithLogLevel adds a logLevel to the config:
func (c *Config) WithLogLevel(logLevel logrus.Level) *Config {
	c.LogLevel = logLevel
	return c
}

// WithWaitForData adds a WaitForData config:
func (c *Config) WithWaitForData(value bool) *Config {
	c.WaitForData = value
	return c
}

// featuresURL give us the full URL for receiving features:
func (c *Config) featuresURL() string {
	return fmt.Sprintf("%s/features/%s", c.ServerAddress, c.SDKKey)
}
