package strategies

import "fmt"

// TypeIPAddress is for ip-address values (eg "1.2.3.4" or "10.0.0.0/16"):
const TypeIPAddress = "IP_ADDRESS"

// IPAddress asserts the given parameters then passes on for evaluation:
func IPAddress(conditional string, options []interface{}, value interface{}) (bool, error) {

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

	// The string evaluations are fine for TypeIPAddress:
	return evaluateString(conditional, assertedOptions, assertedValue), nil
}

// evaluateIPAddress makes evaluations for TypeIPAddress values:
func evaluateIPAddress(conditional string, options []string, value string) bool {

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

	case ConditionalNotEquals:
		// Return false if the value is equal to any of the options
		for _, option := range options {
			if value == option {
				return false
			}
		}
		return true

	case ConditionalExcludes:
		return false

	case ConditionalIncludes:
		return false

	default:
		return false
	}
}
