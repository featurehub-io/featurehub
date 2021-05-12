package strategies

// TypeBoolean is for true/false values:
const TypeBoolean = "BOOLEAN"

// Boolean makes evaluations for BOOLEAN values:
func Boolean(conditional string, options []interface{}, value interface{}) bool {

	switch conditional {

	case ConditionalEquals:
		// Return true if the value is equal to any of the options:
		for _, option := range options {
			if value.(bool) == option.(bool) {
				return true
			}
		}
		return false

	case ConditionalNotEquals:
		// Return false if the value is equal to any of the options
		for _, option := range options {
			if value.(bool) == option.(bool) {
				return false
			}
		}
		return true

	default:
		return false
	}
}
