package client

import (
	"bytes"
	"testing"
	"time"

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
		notifiers: make(map[string]notifier),
	}

	// Load the mock apiClient up with a "features" event:
	client.apiClient.Events <- &testEvent{
		data:  `[{"key":"feature1","type":"NUMBER","value":2}]`,
		event: "features",
	}

	// Load the mock apiClient up with another "features" event, same versions (zero):
	client.apiClient.Events <- &testEvent{
		data:  `[{"key":"feature1","type":"NUMBER","value":2}]`,
		event: "features",
	}

	// Load the mock apiClient up with a "feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"feature2","type":"BOOLEAN","value":true}`,
		event: "feature",
	}

	// Load the mock apiClient up with a "feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"feature4","type":"NUMBER","value":2}`,
		event: "feature",
	}

	// Set up some test notifiers:
	var callback1called, callback2called, callback3called int
	callbackFunc1 := func(*models.FeatureState) {
		callback1called++
	}
	callbackFunc2 := func(*models.FeatureState) {
		callback2called++
	}
	callbackFunc3 := func(*models.FeatureState) {
		callback3called++
	}

	// Add some notifiers:
	client.AddNotifierFeature("feature1", callbackFunc1)
	client.AddNotifierFeature("feature2", callbackFunc2)
	client.AddNotifierFeature("feature3", callbackFunc3)
	client.AddNotifierFeature("feature4", nil)
	assert.Len(t, client.notifiers, 4)
	assert.Contains(t, logBuffer.String(), "Added a notifier")

	// Delete some notifiers:
	assert.NoError(t, client.DeleteNotifier("feature4"))
	err := client.DeleteNotifier("feature5")
	assert.Error(t, err)
	assert.IsType(t, &errors.ErrNotifierNotFound{}, err)

	// Start handling events:
	client.Start()

	// Check that the the correct callbacks were made:
	assert.Equal(t, 1, callback1called)
	assert.Equal(t, 1, callback2called)
	assert.Equal(t, 0, callback3called)

	// Add a BOOLEAN callback:
	var callbackBooleanValue = false
	callbackBoolean := func(value bool) {
		callbackBooleanValue = value
	}
	client.AddNotifierBoolean("booleanfeature", callbackBoolean)

	// Load the mock apiClient up with a "feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"booleanfeature","type":"BOOLEAN","value":true}`,
		event: "feature",
	}

	// Add a JSON callback:
	var callbackJSONValue = `{}`
	callbackJSON := func(value string) {
		callbackJSONValue = value
	}
	client.AddNotifierJSON("jsonfeature", callbackJSON)

	// Load the mock apiClient up with a "feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"jsonfeature","type":"JSON","value":"{\"is_crufty\": true}"}`,
		event: "feature",
	}

	// Add a NUMBER callback:
	var callbackNumberValue float64 = 0
	callbackNumber := func(value float64) {
		callbackNumberValue = value
	}
	client.AddNotifierNumber("numberfeature", callbackNumber)

	// Load the mock apiClient up with a "feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"numberfeature","type":"NUMBER","value":123456789}`,
		event: "feature",
	}

	// Add a STRING callback:
	var callbackStringValue = `{}`
	callbackString := func(value string) {
		callbackStringValue = value
	}
	client.AddNotifierString("stringfeature", callbackString)

	// Load the mock apiClient up with a "feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"stringfeature","type":"STRING","value":"this is a string"}`,
		event: "feature",
	}

	// Give the notifiers some time to think about what they've done:
	time.Sleep(250 * time.Millisecond)

	// Check that the callback functions were all triggered (with the correct values):
	assert.Equal(t, true, callbackBooleanValue)
	assert.Equal(t, `{"is_crufty": true}`, callbackJSONValue)
	assert.Equal(t, float64(123456789), callbackNumberValue)
	assert.Equal(t, "this is a string", callbackStringValue)
}
