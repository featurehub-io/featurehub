package analytics

import (
	"fmt"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	ga "github.com/jpillora/go-ogle-analytics"
)

const (
	defaultCategory = "FeatureHub Event"
)

// GoogleAnalyticsCollector implements the AnalyticsCollector interface:
type GoogleAnalyticsCollector struct {
	client       *ga.Client
	clientID     string
	trackingID   string
	userAgentKey string
}

// NewGoogleAnalyticsCollector returns a GoogleAnalyticsCollector configured with the provided metadata:
func NewGoogleAnalyticsCollector(clientID, trackingID, userAgentKey string) (*GoogleAnalyticsCollector, error) {
	client, err := ga.NewClient(trackingID)
	if err != nil {
		return nil, err
	}

	return &GoogleAnalyticsCollector{
		client:       client.ClientID(clientID),
		clientID:     clientID,
		trackingID:   trackingID,
		userAgentKey: userAgentKey,
	}, nil
}

// LogEvent generates analytics events for the given action and metadata:
func (ac *GoogleAnalyticsCollector) LogEvent(action string, other map[string]string, featureStateAtCurrentTime map[string]*models.FeatureState) error {

	// Emit an event for each feature we have:
	for _, featureState := range featureStateAtCurrentTime {
		event := ga.NewEvent(defaultCategory, action).Label(featureState.Key).Label(fmt.Sprintf("%s", featureState.Value)).Value(1)
		ac.client.Send(event)
	}
	return nil
}
