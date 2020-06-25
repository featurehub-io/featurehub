package errors

import "fmt"

// ErrNotifierNotFound is returned when the user attempts to delete a non-existent notifier:
type ErrNotifierNotFound struct {
	message string
}

// NewErrNotifierNotFound returns a ErrNotifierNotFound with a user-provided message:
func NewErrNotifierNotFound(message string) *ErrNotifierNotFound {
	return &ErrNotifierNotFound{message: message}
}

func (e *ErrNotifierNotFound) Error() string {
	if e.message != "" {
		return fmt.Sprintf("Notifier not found: %s", e.message)
	}
	return "Notifier not found"
}
