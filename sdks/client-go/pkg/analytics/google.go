package analytics

import (
	"context"
	"fmt"

	"github.com/AlekSi/pointer"
	gamp "github.com/blablapolicja/go-gamp"
	"github.com/blablapolicja/go-gamp/client/gampops"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

const (
	defaultCategory = "FeatureHub Event"
)

// GoogleAnalyticsCollector implements the AnalyticsCollector interface:
type GoogleAnalyticsCollector struct {
	client       *gampops.Client
	clientID     string
	trackingID   string
	userAgentKey string
}

// NewGoogleAnalyticsCollector returns a GoogleAnalyticsCollector configured with the provided metadata:
func NewGoogleAnalyticsCollector(clientID, trackingID, userAgentKey string) (*GoogleAnalyticsCollector, error) {
	return &GoogleAnalyticsCollector{
		client:       gamp.New(context.Background(), trackingID),
		clientID:     clientID,
		trackingID:   trackingID,
		userAgentKey: userAgentKey,
	}, nil
}

// LogEvent generates analytics events for the given action and metadata:
func (ac *GoogleAnalyticsCollector) LogEvent(action string, other map[string]string, featureStateAtCurrentTime map[string]*models.FeatureState) error {

	// Emit an event for each feature we have:
	for _, featureState := range featureStateAtCurrentTime {
		// event := ga.NewEvent(defaultCategory, action).Label(fmt.Sprintf("%s: %s", featureState.Key, featureState.Value)).Value(1)
		clientID := ac.clientID
		if clientIDOverride, ok := other["cid"]; ok {
			clientID = clientIDOverride
		}

		err := ac.client.Collect(
			gampops.NewCollectParams().
				WithCid(pointer.ToString(clientID)).
				WithT("event").
				WithEc(pointer.ToString(defaultCategory)).
				WithEa(pointer.ToString(action)).
				WithEl(pointer.ToString(fmt.Sprintf("%s: %v", featureState.Key, featureState.Value))).
				WithEv(pointer.ToInt64(1)),
		)
		// ok, err := ac.client.DebugCollect(
		// 	gampops.NewDebugCollectParams().
		// 		WithCid(pointer.ToString(clientID)).
		// 		WithT("event").
		// 		WithEc(pointer.ToString(defaultCategory)).
		// 		WithEa(pointer.ToString(action)).
		// 		WithEl(pointer.ToString(fmt.Sprintf("%s: %v", featureState.Key, featureState.Value))).
		// 		WithEv(pointer.ToInt64(1)),
		// )
		// fmt.Printf("DebugOK: %v\n", ok)
		if err != nil {
			return err
		}

	}
	return nil
}
