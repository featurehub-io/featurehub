package errors

import "fmt"

// ErrFromAPI is returned when we have trouble making API requests:
type ErrFromAPI struct {
	message string
}

// NewErrFromAPI returns a ErrFromAPI with a user-provided message:
func NewErrFromAPI(message string) *ErrFromAPI {
	return &ErrFromAPI{message: message}
}

func (e *ErrFromAPI) Error() string {
	if e.message != "" {
		return fmt.Sprintf("API error: %s", e.message)
	}
	return "API error"
}
