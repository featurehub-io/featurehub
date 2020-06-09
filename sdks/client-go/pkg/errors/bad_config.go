package errors

import "fmt"

// ErrBadConfig is returned when bad config is detected:
type ErrBadConfig struct {
	message string
}

// NewErrBadConfig returns a ErrBadConfig with a user-provided message:
func NewErrBadConfig(message string) *ErrBadConfig {
	return &ErrBadConfig{message: message}
}

func (e *ErrBadConfig) Error() string {
	if e.message != "" {
		return fmt.Sprintf("Invalid config: %s", e.message)
	}
	return "Invalid config"
}
