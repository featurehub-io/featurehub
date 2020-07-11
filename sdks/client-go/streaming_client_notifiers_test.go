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
		notifiers: make(notifiers),
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

	// Feature1 gets one notifier:
	var callback1called int
	callbackFunc1 := func(*models.FeatureState) {
		callback1called++
	}
	client.AddNotifierFeature("feature1", callbackFunc1)

	// Feature 2 gets 2 notifiers (1/2):
	var callback21called int
	callbackFunc21 := func(*models.FeatureState) {
		callback21called++
	}
	feature2UUID1 := client.AddNotifierFeature("feature2", callbackFunc21)

	// Feature 2 gets 2 notifiers (2/2):
	var callback22called int
	callbackFunc22 := func(*models.FeatureState) {
		callback22called++
	}
	feature2UUID2 := client.AddNotifierFeature("feature2", callbackFunc22)
	assert.Len(t, client.notifiers["feature2"], 2)
	assert.NotSame(t, feature2UUID1, feature2UUID2)

	// Feature3 gets 1 notifer, but we'll delete it before it gets called:
	var callback3called int
	callbackFunc3 := func(*models.FeatureState) {
		callback3called++
	}
	client.AddNotifierFeature("feature3", callbackFunc3)

	// Feature4 gets a notifier with a nil callback:
	feature4UUID := client.AddNotifierFeature("feature4", nil)

	// Prove that we've added the notifiers:
	assert.Len(t, client.notifiers, 4)
	assert.Contains(t, logBuffer.String(), "Added a notifier")

	// Delete some notifiers:
	assert.NoError(t, client.DeleteNotifier("feature4", feature4UUID))
	err := client.DeleteNotifier("feature5", "123")
	assert.Error(t, err)
	assert.IsType(t, &errors.ErrNotifierNotFound{}, err)

	// Add a readiness-listener:
	var readinessListenerCalled = false
	callbackReadiness := func() {
		readinessListenerCalled = true
	}
	client.ReadinessListener(callbackReadiness)

	// Start handling events:
	client.Start()

	// Check that the the correct callbacks were made:
	assert.Equal(t, 1, callback1called)
	assert.Equal(t, 1, callback21called)
	assert.Equal(t, 1, callback22called)
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

	// Check that the client triggered the readiness listener:
	assert.True(t, readinessListenerCalled)
	assert.Contains(t, logBuffer.String(), "Calling readinessListener()")
}
