package models

// If our value is found in the slice then we do NOT match:
func ConditionalNotEquals(strategyType string, slice []interface{}, contains interface{}) bool {

	switch strategyType {
	case strategyTypeBoolean:
		for _, value := range slice {
			if value.(bool) == contains.(bool) {
				return false
			}
		}
		return false

	case strategyTypeString, strategyTypeSemanticVersion:
		for _, value := range slice {
			if value.(string) == contains.(string) {
				return false
			}
		}
		return true

	default:
		return false
	}
}
