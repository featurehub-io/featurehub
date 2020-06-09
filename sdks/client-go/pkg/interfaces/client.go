package interfaces

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

// Client for FeatureHub:
type Client interface {
	GetBoolean(key string) (bool, error)
	GetFeature(key string) (*models.FeatureState, error)
	GetNumber(key string) (float64, error)
	GetRawJSON(key string) (string, error)
	GetString(key string) (string, error)
}
