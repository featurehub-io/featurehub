package models

// If our value is found in the slice then we do NOT match:
func ConditionalNotEquals(strategyType string, slice []string, contains string) bool {
	for _, value := range slice {
		if value == contains {
			return false
		}
	}
	return true
}
