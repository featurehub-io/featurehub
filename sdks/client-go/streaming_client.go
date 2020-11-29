package client

import (
	"net/http"
	"sync"
	"time"

	"github.com/donovanhide/eventsource"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/interfaces"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/sirupsen/logrus"
)

const (
	// ClientContextHeaderName allows the FeatureHub server to provide client-specific rollout strategies:
	ClientContextHeaderName = "x-featurehub"
)

// StreamingClient implements the client interface by by subscribing to server-side events:
type StreamingClient struct {
	analyticsCollectors []interfaces.AnalyticsCollector
	analyticsMutex      sync.Mutex
	apiClient           *eventsource.Stream
	config              *Config
	features            map[string]*models.FeatureState
	featuresMutex       sync.Mutex
	featuresURL         string
	hasData             bool
	logger              *logrus.Logger
	notifiers           notifiers
	notifiersMutex      sync.Mutex
	readinessListener   func()
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
		config:    config,
		logger:    logger,
		notifiers: make(notifiers),
	}

	// Report that we're starting:
	logger.WithField("server_address", client.config.ServerAddress).Info("Subscribing to FeatureHub server")

	// Prepare a custom HTTP request:
	req, err := http.NewRequest("GET", config.featuresURL(), nil)
	if err != nil {
		client.logger.WithError(err).Error("Error preparing request")
		return nil, err
	}

	// Add the client context header (if we have it):
	if config.Context != nil {
		req.Header.Add(ClientContextHeaderName, config.Context.String())
	}

	// Prepare an API client:
	apiClient, err := eventsource.SubscribeWithRequest("", req)
	if err != nil {
		client.logger.WithError(err).Error("Error subscribing to server")
		return nil, err
	}
	client.apiClient = apiClient

	return client, nil
}

// ReadinessListener defines a callback function which will be triggered once the client has received data for the first time:
func (c *StreamingClient) ReadinessListener(callbackFunc func()) {
	c.readinessListener = callbackFunc
}

// Start begins handling events from the streamer:
func (c *StreamingClient) Start() {

	// Handle incoming events:
	go c.handleEvents()
	go c.handleErrors()

	// Block until we have some data:
	if c.config.WaitForData {
		for !c.hasData {
			time.Sleep(time.Second)
		}
	}
}

// isReady triggers various notifications that the client is ready to serve data:
func (c *StreamingClient) isReady() {

	// If we're not already flagged as ready:
	if !c.hasData {

		// Flag us as ready:
		c.hasData = true

		// Trigger the registered readinessListener:
		if c.readinessListener != nil {
			c.logger.Trace("Calling readinessListener()")
			c.readinessListener()
		} else {
			c.logger.Trace("No registered readinessListener() to call")
		}
	}
}
