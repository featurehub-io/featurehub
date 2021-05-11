package models

import "github.com/mcuadros/go-version"

// If our value > any value in the slice then we do NOT match:
func ConditionalLessEquals(strategyType string, slice []string, contains string) bool {

	switch strategyType {
	case strategyTypeSemanticVersion:
		for _, value := range slice {
			if version.Compare(contains, value, ">") {
				return false
			}
		}
		return true

	default:
		for _, value := range slice {
			if contains > value {
				return false
			}
		}
		return true
	}
}
