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

// calculate contains the logic to check each strategy and decide which one applies (if any):
func (s Strategies) calculate(ctx *Context) interface{} {

	// Handle client-side rollout strategies:
	if hashKey, ok := ctx.UniqueKey(); ok {

		// Booleans only have two states, so only one strategy:
		for _, strategy := range s {

			// And we only support percentage (currently):
			if strategy.Percentage != 0 {

				// Murmur32 sum on the key gives us a consistent number:
				hashedPercentage := float64(murmur3.Sum32([]byte(hashKey))) / maxMurmur32Hash * 1000000

				// If our calculated percentage is less than the strategy percentage then take the new value:
				if hashedPercentage <= strategy.Percentage {
					logrus.Warnf("Using percentage strategy (%s:%f = %v) for calculated percentage: %v\n", strategy.ID, strategy.Percentage, strategy.Value, hashedPercentage)
					return strategy.Value
				}
				logrus.Warnf("Not using percentage strategy (%s:%f = %v) for calculated percentage: %v\n", strategy.ID, strategy.Percentage, strategy.Value, hashedPercentage)
			}
		}
	}

	// Otherwise just return nil:
	return nil
}
