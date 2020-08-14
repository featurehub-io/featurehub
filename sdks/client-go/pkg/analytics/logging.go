package analytics

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/sirupsen/logrus"
)

// LoggingAnalyticsCollector implements the AnalyticsCollector interface:
type LoggingAnalyticsCollector struct {
	logger *logrus.Logger
}

// NewLoggingAnalyticsCollector returns an LoggingAnalyticsCollector configured with the provided logger:
func NewLoggingAnalyticsCollector(logger *logrus.Logger) *LoggingAnalyticsCollector {
	return &LoggingAnalyticsCollector{
		logger: logger,
	}
}

// LogEvent generates analytics events for the given action and metadata:
func (ac *LoggingAnalyticsCollector) LogEvent(action string, other map[string]string, featureStateAtCurrentTime map[string]*models.FeatureState) error {

	// Emit an event for each feature we have:
	for _, featureState := range featureStateAtCurrentTime {
		ac.logger.
			WithField("action", action).
			WithField("feature_key", featureState.Key).
			WithField("feature_value", featureState.Value).
			WithField("other", other).
			Debug("Analytics event")
	}
	return nil
}
