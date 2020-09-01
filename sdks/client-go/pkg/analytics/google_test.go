package analytics

import (
	"testing"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
)

func TestGoogleAnalyticsCollector(t *testing.T) {

	// Use the config to make a new StreamingClient with a mock apiClient::
	analyticsCollector, err := NewGoogleAnalyticsCollector(
		uuid.New().String(),
		"UA-12345678-9",
		"fh-analytics-test",
	)
	assert.NoError(t, err)

	// Some test attributes to submit:
	testAttributes := map[string]string{
		"testing": "true",
		"feature": "hub",
	}

	// Some test features to submit:
	testFeatures := map[string]*models.FeatureState{
		"one": {
			Key:   "FEATURE_TANYA",
			Value: "orange",
		},
		"two": {
			Key:   "SUBMIT_COLOR_BUTTON",
			Value: "orange",
		},
	}

	// Log an event:
	assert.NoError(t, analyticsCollector.LogEvent("todo-add", testAttributes, testFeatures))
}
