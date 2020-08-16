package interfaces

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

// Client for FeatureHub:
type Client interface {
	AddNotifierBoolean(featureKey string, callbackFunc models.CallbackFuncBoolean) (notifierUUID string) // Configure a notifier for a BOOLEAN value:
	AddNotifierFeature(featureKey string, callbackFunc models.CallbackFuncFeature) (notifierUUID string) // Configure a notifier for a generic feature:
	AddNotifierJSON(featureKey string, callbackFunc models.CallbackFuncJSON) (notifierUUID string)       // Configure a notifier for a JSON value:
	AddNotifierNumber(featureKey string, callbackFunc models.CallbackFuncNumber) (notifierUUID string)   // Configure a notifier for a NUMBER value:
	AddNotifierString(featureKey string, callbackFunc models.CallbackFuncString) (notifierUUID string)   // Configure a notifier for a STRING value:
	DeleteNotifier(featureKey, notifierUUID string) error                                                // Remove a previously configured notifier (by key and UUID, because we support more than one notifier per key)
	GetBoolean(featureKey string) (bool, error)                                                          // Retrieve a value (by key) for a BOOLEAN feature
	GetFeature(featureKey string) (*models.FeatureState, error)                                          // Retrieve a feature (by key) (value is an interface{})
	GetNumber(featureKey string) (float64, error)                                                        // Retrieve a value (by key) for a NUMBER feature
	GetRawJSON(featureKey string) (string, error)                                                        // Retrieve a value (by key) for a JSON feature
	GetString(featureKey string) (string, error)                                                         // Retrieve a value (by key) for a STRING feature
	LogAnalyticsEvent(action string, other map[string]string)                                            // Send an analytics event (non-blocking, fire and forget)
	LogAnalyticsEventSync(action string, other map[string]string) error                                  // Send an analytics event, but wait for it to complete
	ReadinessListener(callbackFunc func())                                                               // Configure the SDK with a function to call when we're ready (up and running with some data)
}
