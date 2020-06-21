package client

import (
	"encoding/json"
	"sync"
	"time"

	"github.com/donovanhide/eventsource"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/sirupsen/logrus"
)

// StreamingClient implements the client interface by by subscribing to server-side events:
type StreamingClient struct {
	apiClient   *eventsource.Stream
	config      *Config
	features    map[string]*models.FeatureState
	featuresURL string
	hasData     bool
	logger      *logrus.Logger
	mutex       sync.Mutex
}

// New wraps NewStreamingClient (as the default / only implementation):
func New(config *Config) (*StreamingClient, error) {
	return NewStreamingClient(config)
}

// NewStreamingClient prepares a new StreamingClient with given config:
func NewStreamingClient(config *Config) (*StreamingClient, error) {

	// Check for nil config:
	if config == nil {
		return nil, errors.NewErrBadConfig("Nil config provided")
	}

	// Get the config to self-validate:
	if err := config.Validate(); err != nil {
		return nil, err
	}

	// Make a logger:
	logger := logrus.New()
	logger.SetLevel(config.LogLevel)

	// Put this into a new StreamingClient:
	client := &StreamingClient{
		config: config,
		logger: logger,
	}

	// Report that we're starting:
	logger.WithField("server_address", client.config.ServerAddress).Info("Subscribing to FeatureHub server")

	// Prepare an API client:
	apiClient, err := eventsource.Subscribe(config.featuresURL(), "")
	if err != nil {
		client.logger.WithError(err).Error("Error subscribing to server")
		return nil, err
	}
	client.apiClient = apiClient

	return client, nil
}

// Start begins handling events from the streamer:
func (c *StreamingClient) Start() {

	// Handle incoming events:
	go c.handleEvents()
	go c.handleErrors()

	// Block until we have some data:
	for !c.hasData {
		time.Sleep(time.Second)
	}

}

// handleErrors deals with incoming server-side errors:
func (c *StreamingClient) handleErrors() {

	// Run forever (blocks on receiving events from the client channel):
	for {
		event := <-c.apiClient.Errors
		c.logger.WithError(event).Trace("Error from API client")
	}
}

// handleEvents deals with incoming server-side events:
func (c *StreamingClient) handleEvents() {

	// Run forever (blocks on receiving events from the client channel):
	for {
		event := <-c.apiClient.Events

		// Handle the different types of events that can be received on this channel:
		switch models.Event(event.Event()) {

		// Control messages:
		case models.SSEAck, models.SSEBye:
			c.logger.WithField("event", event.Event()).Trace("Received SSE control event")

		// Errors (from the SSE client):
		case models.SSEError:

			// If we're already running then just log an error, otherwise panic:
			if c.hasData {
				c.logger.WithError(&errors.ErrFromAPI{}).WithField("event", event.Event()).WithField("message", event.Data()).Error("Error from API client")
			} else {
				c.logger.WithError(&errors.ErrFromAPI{}).WithField("event", event.Event()).WithField("message", event.Data()).Fatal("Error from API client")
			}

		// Failures (from the FeatureHub server):
		case models.FHFailure:
			c.logger.WithError(&errors.ErrFromAPI{}).WithField("event", event.Event()).WithField("message", event.Data()).Fatal("Failure from FeatureHub server")

		// An entire feature set (replaces what we currently have):
		case models.FHFeatures:
			// Unmarshal the event payload:
			features := []*models.FeatureState{}
			if err := json.Unmarshal([]byte(event.Data()), &features); err != nil {
				c.logger.WithError(err).WithField("event", "features").Error("Error unmarshaling SSE payload")
			}

			// Create a new map of features:
			newFeatures := make(map[string]*models.FeatureState)
			for _, feature := range features {
				newFeatures[feature.Key] = feature
			}

			// Take the new features:
			c.mutex.Lock()
			c.features = newFeatures
			c.hasData = true
			c.mutex.Unlock()

			c.logger.Debugf("Received %d features from server", len(features))

		// One specific feature (replaces the previous version):
		case models.FHFeature:
			// Unmarshal the event payload:
			feature := &models.FeatureState{}
			if err := json.Unmarshal([]byte(event.Data()), feature); err != nil {
				c.logger.WithError(err).WithField("event", "feature").Error("Error unmarshaling SSE payload")
			}

			// Take the new features:
			c.mutex.Lock()
			c.features[feature.Key] = feature
			c.mutex.Unlock()

			c.logger.WithField("key", feature.Key).Debugf("Received a new feature from server")

		// Everything else just gets logged:
		default:
			c.logger.WithField("event", event.Event()).Trace("Received SSE event")
		}
	}
}

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
