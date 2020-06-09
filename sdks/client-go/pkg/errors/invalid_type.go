package errors

import "fmt"

// ErrInvalidType is returned when a feature does not match the expected type:
type ErrInvalidType struct {
	message string
}

// NewErrInvalidType returns a ErrInvalidType with a user-provided message:
func NewErrInvalidType(message string) *ErrInvalidType {
	return &ErrInvalidType{message: message}
}

func (e *ErrInvalidType) Error() string {
	if e.message != "" {
		return fmt.Sprintf("Invalid type: %s", e.message)
	}
	return "Invalid type"
}
