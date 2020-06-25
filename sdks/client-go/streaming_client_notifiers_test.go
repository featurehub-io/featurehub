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

func TestStreamingClientNotifiers(t *testing.T) {

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
		config:    config,
		features:  make(map[string]*models.FeatureState),
		logger:    logger,
		notifiers: make(map[string]func()),
	}

	// Load the mock apiClient up with a "feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"feature1","type":"BOOLEAN","value":1}`,
		event: "feature",
	}

	// Load the mock apiClient up with a "feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"feature3","type":"NUMBER","value":2}`,
		event: "feature",
	}

	// Set up some test notifiers:
	var callback1called, callback2called bool
	callback1 := func() {
		callback1called = true
	}
	callback2 := func() {
		callback2called = true
	}

	// Add some notifiers:
	client.AddNotifier("feature1", callback1)
	client.AddNotifier("feature2", callback2)
	client.AddNotifier("feature3", nil)
	assert.Len(t, client.notifiers, 3)
	assert.Contains(t, logBuffer.String(), "Added a notifier")

	// Delete some notifiers:
	assert.NoError(t, client.DeleteNotifier("feature3"))
	err := client.DeleteNotifier("feature4")
	assert.Error(t, err)
	assert.IsType(t, &errors.ErrNotifierNotFound{}, err)

	// Start handling events:
	client.Start()

	// Check that the the correct callbacks were made:
	assert.True(t, callback1called)
	assert.False(t, callback2called)
}
