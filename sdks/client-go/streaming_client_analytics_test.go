package client

import (
	"bytes"
	"testing"

	"github.com/donovanhide/eventsource"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/analytics"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

func TestStreamingClientAnalytics(t *testing.T) {

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
		analyticsCollector: analytics.NewLoggingAnalyticsCollector(logger),
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
		data:  `{"key":"feature1","type":"STRING","value":"value1"}`,
		event: "feature",
	}

	// Load the mock apiClient up with a "feature" event:
	client.apiClient.Events <- &testEvent{
		data:  `{"key":"feature2","type":"number","value":2}`,
		event: "feature",
	}

	// Start handling events:
	client.Start()

	// Some test attributes to submit:
	testAttributes := map[string]string{
		"testing": "true",
		"feature": "hub",
	}

	// Log an event:
	client.LogAnalyticsEvent("testing", testAttributes)

	// Check that the right things were logged:
	assert.Contains(t, logBuffer.String(), "Analytics event")
	assert.Contains(t, logBuffer.String(), "feature_key=feature1")
	assert.Contains(t, logBuffer.String(), "feature_value=value1")
	assert.Contains(t, logBuffer.String(), "feature_key=feature2")
	assert.Contains(t, logBuffer.String(), "feature_value=2")
	assert.Contains(t, logBuffer.String(), "map[feature:hub testing:true]")

	// Check that the client knew not to trigger the readiness listener (because there was none):
	assert.Contains(t, logBuffer.String(), "No registered readinessListener() to call")
}
