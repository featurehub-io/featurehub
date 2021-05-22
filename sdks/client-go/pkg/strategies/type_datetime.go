package strategies

import (
	"fmt"
	"time"
)

// TypeDateTime is for DATETIME values (eg "YYYY-MM-DDTHH:MM:SSZ"):
const TypeDateTime = "DATETIME"

// DateTime asserts the given parameters then passes on for evaluation:
func DateTime(conditional string, options []interface{}, value interface{}) (bool, error) {

	// Type assert the value:
	assertedValue, ok := value.(string)
	if !ok {
		return false, fmt.Errorf("Unable to assert value (%v) as string", value)
	}

	// The user context can specify "now" as a datetime string:
	if assertedValue == "now" {
		assertedValue = time.Now().Format("2006-01-02T15:04:05Z")
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

	// The string evaluations are fine for TypeDateTime:
	return evaluateString(conditional, assertedOptions, assertedValue), nil
}
