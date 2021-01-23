package models

import (
	"math"

	"github.com/mcuadros/go-version"
	"github.com/spaolacci/murmur3"
)

var (
	maxMurmur32Hash = math.Pow(2, 32)
)

const (
	strategyConditionalEquals        = "EQUALS"
	strategyConditionalGreater       = "GREATER"
	strategyConditionalGreaterEquals = "GREATER_EQUALS"
	strategyConditionalLess          = "LESS"
	strategyConditionalLessEquals    = "LESS_EQUALS"
	strategyConditionalNotEquals     = "NOT_EQUALS"
	strategyFieldNameCountry         = "country"
	strategyFieldNameDevice          = "device"
	strategyFieldNamePlatform        = "platform"
	strategyFieldNameVersion         = "version"
	strategyTypeSemanticVersion      = "SEMANTIC_VERSION"
)

// Strategies so we can attach methods:
type Strategies []Strategy

// Strategy defines model for Strategy.
type Strategy struct {
	Attributes []*StrategyAttribute `json:"attributes"`
	ID         string               `json:"id"`
	Name       string               `json:"name"`
	Percentage float64              `json:"percentage"`
	Value      interface{}          `json:"value,omitempty"` // this value is used if it is a simple attribute or percentage. If it is more complex then the pairs are passed
}

// StrategyAttribute defines a more complex strategy than simple percentages:
type StrategyAttribute struct {
	ID          string   `json:"id"`
	Conditional string   `json:"conditional"`
	FieldName   string   `json:"fieldName"`
	Values      []string `json:"values"`
	Type        string   `json:"type"`
}

// calculate contains the logic to check each strategy and decide which one applies (if any):
func (ss Strategies) calculate(clientContext *Context) interface{} {

	// Pre-calculate our hashKey:
	hashKey, _ := clientContext.UniqueKey()

	// Go through the available strategies:
	for _, strategy := range ss {
		logger.Tracef("Checking strategy (%s)", strategy.ID)

		// Check if we match any percentage-based rule:
		if !strategy.proceedWithPercentage(hashKey) {
			logger.Tracef("Failed strategy (%s) percentage - trying next strategy", strategy.ID)
			continue
		}

		// Check if we match the attribute-based rules:
		if !strategy.proceedWithAttributes(clientContext) {
			logger.Tracef("Failed strategy (%s) attributes - trying next strategy", strategy.ID)
			continue
		}

		// If we got this far then we matched this strategy, so we return its value:
		logger.Debugf("Matched strategy (%s:%s)", strategy.ID, strategy.Name)
		return strategy.Value
	}

	// Otherwise just return nil:
	return nil
}

// proceedWithPercentage contains the logic to match percentage-based rules on a user-key / session-key hash:
func (s Strategy) proceedWithPercentage(hashKey string) bool {

	// Make sure we have a percentage rule:
	if s.Percentage == 0 {
		return true
	}

	// If we do have a rule, but don't have a hash-key then we can't continue with this strategy:
	if len(hashKey) == 0 {
		return false
	}

	// Murmur32 sum on the key gives us a consistent number:
	hashedPercentage := float64(murmur3.Sum32([]byte(hashKey))) / maxMurmur32Hash * 1000000

	// If our calculated percentage is less than the strategy percentage then we matched!
	if hashedPercentage <= s.Percentage {
		logger.Tracef("Matched percentage strategy (%s:%f = %v) for calculated percentage: %v\n", s.ID, s.Percentage, s.Value, hashedPercentage)
		return true
	}

	logger.Debugf("Didn't match percentage strategy (%s:%f = %v) for calculated percentage: %v\n", s.ID, s.Percentage, s.Value, hashedPercentage)
	return false
}

// proceedWithPercentage contains the logic to match attribute-based rules on the rest of the client context:
func (s Strategy) proceedWithAttributes(clientContext *Context) bool {

	// We can't continue without a clientContext:
	if clientContext == nil {
		logger.Trace("proceedWithAttributes() Received nil clientContext")
		return false
	}

	for _, sa := range s.Attributes {

		// Handle each different client-context attribute:
		switch sa.FieldName {

		// Match by country name:
		case strategyFieldNameCountry:
			if len(clientContext.Country) > 0 && sa.matchConditional(sa.Values, string(clientContext.Country)) {
				continue
			}
			logger.Tracef("Didn't match attribute strategy (%s:%s = %v) for country: %v\n", sa.ID, sa.FieldName, sa.Values, clientContext.Country)
			return false

		// Match by device type:
		case strategyFieldNameDevice:
			if len(clientContext.Device) > 0 && sa.matchConditional(sa.Values, string(clientContext.Device)) {
				continue
			}
			logger.Tracef("Didn't match attribute strategy (%s:%s = %v) for device: %v\n", sa.ID, sa.FieldName, sa.Values, clientContext.Device)
			return false

		// Match by platform:
		case strategyFieldNamePlatform:
			if len(clientContext.Platform) > 0 && sa.matchConditional(sa.Values, string(clientContext.Platform)) {
				continue
			}
			logger.Tracef("Didn't match attribute strategy (%s:%s = %v) for platform: %v\n", sa.ID, sa.FieldName, sa.Values, clientContext.Platform)
			return false

		// Match by version:
		case strategyFieldNameVersion:
			logger.Trace("Trying version")
			if len(clientContext.Version) > 0 && sa.matchConditional(sa.Values, string(clientContext.Version)) {
				continue
			}
			logger.Tracef("Didn't match attribute strategy (%s:%s = %v) for version: %v\n", sa.ID, sa.FieldName, sa.Values, clientContext.Version)
			return false

		// Some other (unsupported) field:
		default:
			logger.Infof("Unsupported strategy field (%s)", sa.FieldName)
			return false
		}
	}

	return true
}

// matchConditional checks the given string against the given slice of strings with the attribute's conditional logic:
func (sa *StrategyAttribute) matchConditional(slice []string, contains string) bool {

	// Handle the different conditionals available to us:
	switch {

	// If our value is found in the slice then we match:
	case sa.Conditional == strategyConditionalEquals:
		for _, value := range slice {
			if value == contains {
				return true
			}
		}
		return false

	// If our value is found in the slice then we do NOT match:
	case sa.Conditional == strategyConditionalNotEquals:
		for _, value := range slice {
			if value == contains {
				return false
			}
		}
		return true

	// If our value <= any value in the slice then we do NOT match:
	case sa.Conditional == strategyConditionalGreater && sa.Type == strategyTypeSemanticVersion:
		for _, value := range slice {
			if version.Compare(contains, value, "<=") {
				return false
			}
		}
		return true

	// // If our value <= any value in the slice then we do NOT match:
	// case sa.Conditional == strategyConditionalGreater && sa.Type != strategyTypeSemanticVersion:
	// 	for _, value := range slice {
	// 		if contains <= value {
	// 			return false
	// 		}
	// 	}
	// 	return true

	// If our value < any value in the slice then we do NOT match:
	case sa.Conditional == strategyConditionalGreaterEquals && sa.Type == strategyTypeSemanticVersion:
		for _, value := range slice {
			if version.Compare(contains, value, "<") {
				return false
			}
		}
		return true

	// // If our value < any value in the slice then we do NOT match:
	// case sa.Conditional == strategyConditionalGreaterEquals && sa.Type != strategyTypeSemanticVersion:
	// 	for _, value := range slice {
	// 		if contains < value {
	// 			return false
	// 		}
	// 	}
	// 	return true

	// If our value >= any value in the slice then we do NOT match:
	case sa.Conditional == strategyConditionalLess && sa.Type == strategyTypeSemanticVersion:
		for _, value := range slice {
			if version.Compare(contains, value, ">=") {
				return false
			}
		}
		return true

	// // If our value >= any value in the slice then we do NOT match:
	// case sa.Conditional == strategyConditionalLess && sa.Type != strategyTypeSemanticVersion:
	// 	for _, value := range slice {
	// 		if contains >= value {
	// 			return false
	// 		}
	// 	}
	// 	return true

	// If our value > any value in the slice then we do NOT match:
	case sa.Conditional == strategyConditionalLessEquals && sa.Type == strategyTypeSemanticVersion:
		for _, value := range slice {
			if version.Compare(contains, value, ">") {
				return false
			}
		}
		return true

		// // If our value > any value in the slice then we do NOT match:
		// case sa.Conditional == strategyConditionalLessEquals && sa.Type != strategyTypeSemanticVersion:
		// 	for _, value := range slice {
		// 		if contains > value {
		// 			return false
		// 		}
		// 	}
		// 	return true
	}

	// We didn't find it:
	return false
}
