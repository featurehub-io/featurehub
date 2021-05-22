package strategies

import (
	"fmt"
	"net"
)

// TypeIPAddress is for ip-address values (eg "1.2.3.4" or "10.0.0.0/16"):
const TypeIPAddress = "IP_ADDRESS"

// IPAddress asserts the given parameters then passes on for evaluation:
func IPAddress(conditional string, options []interface{}, value interface{}) (bool, error) {

	// Type assert the value:
	assertedValue, ok := value.(string)
	if !ok {
		return false, fmt.Errorf("Unable to assert value (%v) as string", value)
	}

	// The value should be a valid IP address:
	ip, _, err := net.ParseCIDR(assertedValue)
	if err != nil {
		return false, err
	}
	assertedValue = ip.String()

	// Type assert all of the options:
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
		// Return false if the value is equal to any of the options:
		for _, option := range options {
			if value == option {
				return false
			}
		}
		return true

	case ConditionalExcludes:

		// Parse the value IP:
		valueIP, _, err := net.ParseCIDR(value)
		if err != nil {
			return false
		}

		// Return false if the value is included by any of the options:
		for _, option := range options {

			// Parse each option address:
			_, optionNet, err := net.ParseCIDR(option)
			if err != nil {
				return false
			}

			if optionNet.Contains(valueIP) {
				return false
			}
		}
		return true

	case ConditionalIncludes:

		// Parse the value IP:
		valueIP, _, err := net.ParseCIDR(value)
		if err != nil {
			return false
		}

		// Return true if the value is included by any of the options:
		for _, option := range options {

			// Parse each option address:
			_, optionNet, err := net.ParseCIDR(option)
			if err != nil {
				return false
			}

			if optionNet.Contains(valueIP) {
				return true
			}
		}
		return false

	default:
		return false
	}
}
