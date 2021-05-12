package strategies

import (
	"fmt"
	"strings"
)

// TypeNumber is for numerical values:
const TypeNumber = "NUMBER"

// Number asserts the given parameters then passes on for evaluation:
func Number(conditional string, options []interface{}, value interface{}) (bool, error) {

	// Type assert the value:
	assertedValue, ok := value.(float64)
	if !ok {
		return false, fmt.Errorf("Unable to assert value (%v) as float64", value)
	}

	// Type assert all of the options:
	var assertedOptions []float64
	for _, option := range options {
		assertedOption, ok := option.(float64)
		if !ok {
			return false, fmt.Errorf("Unable to assert value (%v) as float64", option)
		}
		assertedOptions = append(assertedOptions, assertedOption)
	}

	return evaluateNumber(conditional, assertedOptions, assertedValue), nil
}

// evaluateNumber makes evaluations for TypeNumber values:
func evaluateNumber(conditional string, options []float64, value float64) bool {

	switch conditional {

	case ConditionalEquals:
		// Return true if the value is equal to any of the options:
		for _, option := range options {
			if value == option {
				return true
			}
		}
		return false

	case ConditionalNotEquals:
		// Return false if the value is equal to any of the options:
		for _, option := range options {
			if value == option {
				return false
			}
		}
		return true

	case ConditionalEndsWith:
		// Return true if the value ends with any of the options:
		for _, option := range options {
			if strings.HasSuffix(fmt.Sprintf("%f", value), fmt.Sprintf("%f", option)) {
				return true
			}
		}
		return false

	case ConditionalStartsWith:
		// Return true if the value starts with any of the options:
		for _, option := range options {
			if strings.HasPrefix(fmt.Sprintf("%f", value), fmt.Sprintf("%f", option)) {
				return true
			}
		}
		return false

	case ConditionalLess:
		// Return false if the value is greater than or equal to any of the options:
		for _, option := range options {
			if value >= option {
				return false
			}
		}
		return true

	case ConditionalLessEquals:
		// Return false if the value is greater than any of the options:
		for _, option := range options {
			if value > option {
				return false
			}
		}
		return true

	case ConditionalGreater:
		// Return false if the value is less than or equal to any of the options:
		for _, option := range options {
			if value <= option {
				return false
			}
		}
		return true

	case ConditionalGreaterEquals:
		// Return false if the value is less than any of the options:
		for _, option := range options {
			if value < option {
				return false
			}
		}
		return true

	case ConditionalExcludes:
		// Return false if the value contains any of the options:
		for _, option := range options {
			if strings.Contains(fmt.Sprintf("%f", value), fmt.Sprintf("%f", option)) {
				return false
			}
		}
		return true

	case ConditionalIncludes:
		// Return true if the value contains any of the options:
		for _, option := range options {
			if strings.Contains(fmt.Sprintf("%f", value), fmt.Sprintf("%f", option)) {
				return true
			}
		}
		return false

	default:
		return false
	}
}
