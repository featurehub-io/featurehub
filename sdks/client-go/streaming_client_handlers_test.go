package client

import (
	"bytes"
	"testing"

	"github.com/donovanhide/eventsource"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

func TestStreamingClientHandlers(t *testing.T) {

	// Make a test config (with an incorrect server address):
	config := &Config{
		WaitForData: true,
	}

	// Make a logger:
	logger := logrus.New()
	logger.SetLevel(logrus.TraceLevel)
	logBuffer := new(bytes.Buffer)
	logger.SetOutput(logBuffer)

	// Use the config to make a new StreamingClient with a mock apiClient::
	client := &StreamingClient{
		apiClient: &eventsource.Stream{
			Errors: make(chan error, 100),
			Events: make(chan eventsource.Event, 100),
		},
		config:   config,
		features: make(map[string]*models.FeatureState),
		logger:   logger,
	}

	// Load the mock apiClient up with a "feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"anotherfeature","type":"BOOLEAN","value":false,"version":3}`,
		event: "feature",
	}

	// Load the mock apiClient up with a "feature" event (but with an out-of-sync version):
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"anotherfeature","type":"BOOLEAN","value":false,"version":2}`,
		event: "feature",
	}

	// Load the mock apiClient up with a "feature" event (which we'll delete):
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"featuretodelete","type":"BOOLEAN","value":true}`,
		event: "feature",
	}

	// Load the mock apiClient up with a "delete_feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"featuretodelete","type":"BOOLEAN","value":false,"version":2}`,
		event: "delete_feature",
	}

	// Start handling events:
	client.Start()

	// Make sure new features with old versions don't clobber values:
	anotherFeature, err := client.GetFeature("anotherfeature")
	assert.NoError(t, err)
	assert.Equal(t, int64(3), anotherFeature.Version)

	// Make sure features get deleted:
	deletedFeature, err := client.GetFeature("featuretodelete")
	assert.Error(t, err)
	assert.IsType(t, &errors.ErrFeatureNotFound{}, err)
	assert.Nil(t, deletedFeature)
	assert.Contains(t, logBuffer.String(), "Deleted a feature")
}
