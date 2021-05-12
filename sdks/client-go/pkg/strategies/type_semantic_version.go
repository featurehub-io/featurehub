package strategies

import "github.com/mcuadros/go-version"

// TypeSemanticVersion is for semver values (eg 2.1.3):
const TypeSemanticVersion = "SEMANTIC_VERSION"

// SemanticVersion makes evaluations for SEMANTIC_VERSION values:
func SemanticVersion(conditional string, options []interface{}, value interface{}) bool {

	switch conditional {

	case ConditionalEquals:
		// Return true if the value is equal to any of the options:
		for _, option := range options {
			if value.(string) == option.(string) {
				return true
			}
		}
		return false

	case ConditionalGreater:
		// Return false if the value is less than or equal to any of the options
		for _, option := range options {
			if version.Compare(value.(string), option.(string), "<=") {
				return false
			}
		}
		return true

	case ConditionalGreaterEquals:
		// Return false if the value is less than any of the options:
		for _, option := range options {
			if version.Compare(value.(string), option.(string), "<") {
				return false
			}
		}
		return true

	case ConditionalLess:
		// Return false if the value is greater than or equal to any of the options:
		for _, option := range options {
			if version.Compare(value.(string), option.(string), ">=") {
				return false
			}
		}
		return true

	case ConditionalLessEquals:
		// Return false if the value is greater than any of the options:
		for _, option := range options {
			if version.Compare(value.(string), option.(string), ">") {
				return false
			}
		}
		return true

	case ConditionalNotEquals:
		// Return false if the value is equal to any of the options
		for _, option := range options {
			if value.(string) == option.(string) {
				return false
			}
		}
		return true

	default:
		return false
	}
}
