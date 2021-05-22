package strategies

import (
	"fmt"
	"regexp"
	"strings"
)

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
			if strings.HasSuffix(value, option) {
				return true
			}
		}
		return false

	case ConditionalStartsWith:
		// Return true if the value starts with any of the options:
		for _, option := range options {
			if strings.HasPrefix(value, option) {
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
			if strings.Contains(value, option) {
				return false
			}
		}
		return true

	case ConditionalIncludes:
		// Return true if the value contains any of the options:
		for _, option := range options {
			if strings.Contains(value, option) {
				return true
			}
		}
		return false

	case ConditionalRegex:
		// Return true if the value matches any of the regex options:
		for _, option := range options {
			if matched, _ := regexp.MatchString(option, value); matched {
				return true
			}
		}
		return false

	default:
		return false
	}
}
