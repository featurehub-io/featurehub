package analytics

import (
	"bytes"
	"testing"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

func TestLoggingAnalyticsCollector(t *testing.T) {

	// Make a logger:
	logger := logrus.New()
	logger.SetLevel(logrus.TraceLevel)
	logBuffer := new(bytes.Buffer)
	logger.SetOutput(logBuffer)

	// Use the config to make a new StreamingClient with a mock apiClient::
	analyticsCollector := NewLoggingAnalyticsCollector(logger)

	// Some test attributes to submit:
	testAttributes := map[string]string{
		"testing": "true",
		"feature": "hub",
	}

	// Some test features to submit:
	testFeatures := map[string]*models.FeatureState{
		"one": {
			Key:   "feature1",
			Value: "value1",
		},
		"two": {
			Key:   "feature2",
			Value: 2,
		},
	}

	// Log an event:
	analyticsCollector.LogEvent("testing", testAttributes, testFeatures)

	// Check that the right things were logged:
	assert.Contains(t, logBuffer.String(), "Analytics event")
	assert.Contains(t, logBuffer.String(), "feature_key=feature1")
	assert.Contains(t, logBuffer.String(), "feature_value=value1")
	assert.Contains(t, logBuffer.String(), "feature_key=feature2")
	assert.Contains(t, logBuffer.String(), "feature_value=2")
	assert.Contains(t, logBuffer.String(), "map[feature:hub testing:true]")
}
