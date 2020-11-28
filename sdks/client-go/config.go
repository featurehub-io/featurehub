package client

import (
	"fmt"
	"strings"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/sirupsen/logrus"
)

const (
	defaultNamedCache = "default"
)

// Config defines parameters for the client:
type Config struct {
	Context       *Context     // Client context metadata for server-side strategies (optional)
	LogLevel      logrus.Level // Logging level (default is "info")
	SDKKey        string       // SDK key (copied from the UI), in the format "{namedCache}/environmentID/APIKey"
	ServerAddress string       // FeatureHub API endpoint
	WaitForData   bool         // New() will block until some data has arrived
}

// featuresURL give us the full URL for receiving features:
func (c *Config) featuresURL() string {
	return fmt.Sprintf("%s/features/%s", c.ServerAddress, c.SDKKey)
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
