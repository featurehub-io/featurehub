package strategies

import (
	"math"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/sirupsen/logrus"
	"github.com/spaolacci/murmur3"
)

var (
	maxMurmur32Hash = math.Pow(2, 32)
)

// Decider makes decisions based on strategies and client context:
type Decider struct {
	logger *logrus.Logger
}

// New returns a configured strategy decider:
func New(logger *logrus.Logger) *Decider {
	return &Decider{
		logger: logger,
	}
}

// Boolean determines the value of a boolean:
func (d *Decider) Boolean(defaultValue bool, fs *models.FeatureState, ctx *models.Context) bool {

	// Handle client-side rollout strategies:
	if hashKey, ok := ctx.UniqueKey(); ok {

		// Booleans only have two states, so only one strategy:
		if len(fs.Strategies) == 1 {

			// And we only support percentage (currently):
			if fs.Strategies[0].Percentage != 0 {

				// Murmur32 sum on the key gives us a consistent number:
				hashedPercentage := float64(murmur3.Sum32([]byte(hashKey))) / maxMurmur32Hash

				d.logger.Tracef("Using percentage strategy (%d): %v\n", fs.Strategies[0].Percentage, hashedPercentage)
			}
		}
	}

	// Otherwise just carry on with the default value:
	return defaultValue
}
