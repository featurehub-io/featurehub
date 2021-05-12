package strategies

const TypeBoolean = "BOOLEAN"

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
