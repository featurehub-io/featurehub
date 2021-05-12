package strategies

import (
	"fmt"
	"time"
)

// TypeDate is for date values (eg "YYYY-MM-DD"):
const TypeDate = "DATE"

// Date asserts the given parameters then passes on for evaluation:
func Date(conditional string, options []interface{}, value interface{}) (bool, error) {

	// Type assert the value:
	assertedValue, ok := value.(string)
	if !ok {
		return false, fmt.Errorf("Unable to assert value (%v) as string", value)
	}

	// The user context can specify "now" as a date string:
	if assertedValue == "now" {
		assertedValue = time.Now().Format("2006-01-02")
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

	// The string evaluations are fine for TypeDate:
	return evaluateString(conditional, assertedOptions, assertedValue), nil
}
