package models

import "github.com/mcuadros/go-version"

// If our value >= any value in the slice then we do NOT match:
func ConditionalLess(strategyType string, slice []interface{}, contains interface{}) bool {

	switch strategyType {
	case strategyTypeString:
		for _, value := range slice {
			if contains.(string) >= value.(string) {
				return false
			}
		}
		return true

	case strategyTypeSemanticVersion:
		for _, value := range slice {
			if version.Compare(contains.(string), value.(string), ">=") {
				return false
			}
		}
		return true

	default:
		return false
	}
}
