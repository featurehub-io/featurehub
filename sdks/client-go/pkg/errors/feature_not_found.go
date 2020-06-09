package errors

import "fmt"

// ErrFeatureNotFound is returned when a requested feature is not found:
type ErrFeatureNotFound struct {
	message string
}

// NewErrFeatureNotFound returns a ErrFeatureNotFound with a user-provided message:
func NewErrFeatureNotFound(message string) *ErrFeatureNotFound {
	return &ErrFeatureNotFound{message: message}
}

func (e *ErrFeatureNotFound) Error() string {
	if e.message != "" {
		return fmt.Sprintf("Feature not found: %s", e.message)
	}
	return "Feature not found"
}
