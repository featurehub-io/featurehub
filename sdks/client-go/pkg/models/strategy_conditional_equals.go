package models

// If our value is found in the slice then we match:
func ConditionalEquals(strategyType string, slice []interface{}, contains interface{}) bool {

	switch strategyType {
	case strategyTypeString, strategyTypeSemanticVersion:
		for _, value := range slice {
			if value.(string) == contains.(string) {
				return true
			}
		}
		return false

	default:
		return false
	}
}
