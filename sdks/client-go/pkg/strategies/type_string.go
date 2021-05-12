package strategies

import "fmt"

// TypeString is for string values (eg "something"):
const TypeString = "STRING"

// String asserts the given parameters then passes on for evaluation:
func String(conditional string, options []interface{}, value interface{}) (bool, error) {

	// Type assert the value:
	assertedValue, ok := value.(string)
	if !ok {
		return false, fmt.Errorf("Unable to assert value (%v) as string", value)
	}

	// Type assert all of the options:
	var assertedOptions []string
	for _, option := range options {
		assertedOption, ok := option.(string)
		if !ok {
			return false, fmt.Errorf("Unable to assert value (%v) as string", option)
		}
		assertedOptions = append(assertedOptions, assertedOption)
	}

	return evaluateString(conditional, assertedOptions, assertedValue), nil
}

// evaluateString makes evaluations for TypeString values:
func evaluateString(conditional string, options []string, value string) bool {

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

	case ConditionalEndsWith:
		return false

	case ConditionalStartsWith:
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
		// Return false if the value is less than or equal to any of the options
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
		return false

	case ConditionalIncludes:
		return false

	case ConditionalRegex:
		return false

	default:
		return false
	}
}
