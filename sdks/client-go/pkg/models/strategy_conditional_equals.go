package models

// If our value is found in the slice then we match:
func ConditionalEquals(strategyType string, slice []string, contains string) bool {
	for _, value := range slice {
		if value == contains {
			return true
		}
	}
	return false
}
