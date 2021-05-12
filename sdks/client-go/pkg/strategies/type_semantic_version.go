package strategies

import (
	"fmt"

	"github.com/mcuadros/go-version"
)

// TypeSemanticVersion is for semver values (eg 2.1.3):
const TypeSemanticVersion = "SEMANTIC_VERSION"

// SemanticVersion asserts the given parameters then passes on for evaluation:
func SemanticVersion(conditional string, options []interface{}, value interface{}) (bool, error) {

	assertedValue, ok := value.(string)
	if !ok {
		return false, fmt.Errorf("Unable to assert value (%v) as string", value)
	}

	var assertedOptions []string
	for _, option := range options {
		assertedOption, ok := option.(string)
		if !ok {
			return false, fmt.Errorf("Unable to assert value (%v) as string", option)
		}
		assertedOptions = append(assertedOptions, assertedOption)
	}

	return evaluateSemanticVersion(conditional, assertedOptions, assertedValue), nil
}

// evaluateSemanticVersion makes evaluations for SEMANTIC_VERSION values:
func evaluateSemanticVersion(conditional string, options []string, value string) bool {

	// Make sure we have a value:
	if len(value) == 0 {
		return false
	}

	switch conditional {

	case ConditionalEquals:
		// Return true if the value is equal to any of the options:
		for _, option := range options {
			if value == option {
				return true
			}
		}
		return false

	case ConditionalGreater:
		// Return false if the value is less than or equal to any of the options
		for _, option := range options {
			if version.Compare(value, option, "<=") {
				return false
			}
		}
		return true

	case ConditionalGreaterEquals:
		// Return false if the value is less than any of the options:
		for _, option := range options {
			if version.Compare(value, option, "<") {
				return false
			}
		}
		return true

	case ConditionalLess:
		// Return false if the value is greater than or equal to any of the options:
		for _, option := range options {
			if version.Compare(value, option, ">=") {
				return false
			}
		}
		return true

	case ConditionalLessEquals:
		// Return false if the value is greater than any of the options:
		for _, option := range options {
			if version.Compare(value, option, ">") {
				return false
			}
		}
		return true

	case ConditionalNotEquals:
		// Return false if the value is equal to any of the options
		for _, option := range options {
			if value == option {
				return false
			}
		}
		return true

	default:
		return false
	}
}
