package analytics

import (
	"fmt"

	ga "github.com/berdowsky/go-ogle-analytics"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
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

		// Allow for a new clientID:
		clientID := ac.clientID
		if clientIDOverride, ok := other["cid"]; ok {
			clientID = clientIDOverride
		}

		// Prepare a new event:
		event := ga.NewEvent(defaultCategory, action).
			Label(fmt.Sprintf("%s : %v", featureState.Key, featureState.Value))

		// Send the event:
		err := ac.client.
			ClientID(clientID).
			Send(event)
		if err != nil {
			return err
		}
	}
	return nil
}
