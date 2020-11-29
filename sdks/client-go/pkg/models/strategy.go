package models

import (
	"math"

	"github.com/sirupsen/logrus"
	"github.com/spaolacci/murmur3"
)

var (
	maxMurmur32Hash = math.Pow(2, 32)
)

// Strategies so we can attach methods:
type Strategies []Strategy

// Strategy defines model for Strategy.
type Strategy struct {
	ID         string          `json:"id"`
	Name       string          `json:"name"`
	Pairs      []*StrategyPair `json:"pairs,omitempty"`
	Percentage float64         `json:"percentage"`
	Value      interface{}     `json:"value,omitempty"` // this value is used if it is a simple attribute or percentage. If it is more complex then the pairs are passed
}

// StrategyPair defines model for StrategyPair.
type StrategyPair struct {
	Name  string `json:"name"`
	Value string `json:"value,omitempty"`
}

// Boolean determines the value of a boolean:
func (s Strategies) boolean(defaultValue bool, ctx *Context) bool {

	// Handle client-side rollout strategies:
	if hashKey, ok := ctx.UniqueKey(); ok {

		// Booleans only have two states, so only one strategy:
		if len(s) == 1 {

			// And we only support percentage (currently):
			if s[0].Percentage != 0 {

				// Murmur32 sum on the key gives us a consistent number:
				hashedPercentage := float64(murmur3.Sum32([]byte(hashKey))) / maxMurmur32Hash * 1000000

				// If our calculated percentage is less than the strategy percentage then take the new value:
				if hashedPercentage <= s[0].Percentage {
					logrus.Warnf("Using percentage strategy (%f): %v\n", s[0].Percentage, hashedPercentage)
					return s[0].Value.(bool)
				}
				logrus.Warnf("Not using percentage strategy (%f): %v\n", s[0].Percentage, hashedPercentage)
			}
		}
	}

	// Otherwise just carry on with the default value:
	return defaultValue
}
