package client

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/sirupsen/logrus"
)

const (
	defaultNamedCache = "default"
)

// Config defines parameters for the client:
type Config struct {
	APIKey        string       // FeatureHub API key
	EnvironmentID string       // FeatureHub EnvironmentID
	LogLevel      logrus.Level // Logging level (default is "info")
	NamedCache    string       // FeatureHub NamedCache (default is "default")
	ServerAddress string       // FeatureHub API endpoint
	WaitForData   bool         // New() will block until some data has arrived
}

// Validate can be called to check various config options:
func (c *Config) Validate() error {

	// APIKey shouldn't be empty:
	if len(c.APIKey) == 0 {
		return errors.NewErrBadConfig("APIKey is required")
	}

	// EnvironmentID shouldn't be empty:
	if len(c.EnvironmentID) == 0 {
		return errors.NewErrBadConfig("EnvironmentID is required")
	}

	// LogLevel shouldn't be empty:
	if c.LogLevel == 0 {
		c.LogLevel = logrus.InfoLevel
	}

	// NamedCache shouldn't be empty:
	if len(c.NamedCache) == 0 {
		c.NamedCache = defaultNamedCache
	}

	// ServerAddress shouldn't be empty:
	if len(c.ServerAddress) == 0 {
		return errors.NewErrBadConfig("ServerAddress is required")
	}

	return nil
}
