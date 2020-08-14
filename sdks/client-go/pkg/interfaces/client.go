package interfaces

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

// Client for FeatureHub:
type Client interface {
	AddNotifierBoolean(featureKey string, callbackFunc models.CallbackFuncBoolean) (notifierUUID string)
	AddNotifierFeature(featureKey string, callbackFunc models.CallbackFuncFeature) (notifierUUID string)
	AddNotifierJSON(featureKey string, callbackFunc models.CallbackFuncJSON) (notifierUUID string)
	AddNotifierNumber(featureKey string, callbackFunc models.CallbackFuncNumber) (notifierUUID string)
	AddNotifierString(featureKey string, callbackFunc models.CallbackFuncString) (notifierUUID string)
	DeleteNotifier(featureKey, notifierUUID string) error
	GetBoolean(featureKey string) (bool, error)
	GetFeature(featureKey string) (*models.FeatureState, error)
	GetNumber(featureKey string) (float64, error)
	GetRawJSON(featureKey string) (string, error)
	GetString(featureKey string) (string, error)
	LogAnalyticsEvent(action string, other map[string]string) error
	ReadinessListener(callbackFunc func())
}
