package interfaces

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

// AnalyticsCollector allows the user to generate analytics events:
type AnalyticsCollector interface {
	LogEvent(action string, other map[string]string, featureStateAtCurrentTime []*models.FeatureState)
}
