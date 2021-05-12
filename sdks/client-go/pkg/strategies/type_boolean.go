package strategies

import "fmt"

// TypeBoolean is for true/false values:
const TypeBoolean = "BOOLEAN"

// Boolean asserts the given parameters then passes on for evaluation:
func Boolean(conditional string, options []interface{}, value interface{}) (bool, error) {

	// Type assert the value:
	assertedValue, ok := value.(bool)
	if !ok {
		return false, fmt.Errorf("Unable to assert value (%v) as bool", value)
	}

	// Type assert all of the options:
	var assertedOptions []bool
	for _, option := range options {
		assertedOption, ok := option.(bool)
		if !ok {
			return false, fmt.Errorf("Unable to assert value (%v) as bool", option)
		}
		assertedOptions = append(assertedOptions, assertedOption)
	}

	return evaluateBoolean(conditional, assertedOptions, assertedValue), nil
}

// evaluateBoolean makes evaluations for TypeBoolean values:
func evaluateBoolean(conditional string, options []bool, value bool) bool {

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

	default:
		return false
	}
}
