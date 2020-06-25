package client

import (
	"testing"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/interfaces"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

// testEvent implements the simple "Event" interface from donovanhide/eventsource:
type testEvent struct {
	data  string
	event string
	id    string
}

func (e *testEvent) Data() string  { return e.data }
func (e *testEvent) Event() string { return e.event }
func (e *testEvent) Id() string    { return e.id }

func TestStreamingClient(t *testing.T) {

	// Make a test config (with an incorrect server address):
	config := &Config{
		LogLevel:      logrus.FatalLevel,
		SDKKey:        "default/environment-id/my-secret-api-key",
		ServerAddress: "http://streams.test:8086",
		WaitForData:   true,
	}

	// Attempt to make a new client (config has a non-existent hostname):
	client, err := NewStreamingClient(config)
	assert.Error(t, err)
	assert.Implements(t, new(interfaces.Client), client)
}
