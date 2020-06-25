package interfaces

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

// Client for FeatureHub:
type Client interface {
	AddNotifierBoolean(key string, callbackFunc models.CallbackFuncBoolean)
	AddNotifierFeature(key string, callbackFunc models.CallbackFuncFeature)
	AddNotifierJSON(key string, callbackFunc models.CallbackFuncJSON)
	AddNotifierNumber(key string, callbackFunc models.CallbackFuncNumber)
	AddNotifierString(key string, callbackFunc models.CallbackFuncString)
	DeleteNotifier(key string) error
	GetBoolean(key string) (bool, error)
	GetFeature(key string) (*models.FeatureState, error)
	GetNumber(key string) (float64, error)
	GetRawJSON(key string) (string, error)
	GetString(key string) (string, error)
}
