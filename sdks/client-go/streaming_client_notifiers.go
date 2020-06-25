package client

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
)

// AddNotifier adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifier(key string, callback func()) {
	c.mutex.Lock()
	defer c.mutex.Unlock()
	c.notifiers[key] = callback
}

// DeleteNotifier removes a notifier callback:
func (c *StreamingClient) DeleteNotifier(key string) error {
	c.mutex.Lock()
	defer c.mutex.Unlock()
	if _, ok := c.notifiers[key]; !ok {
		err := errors.NewErrNotifierNotFound(key)
		c.logger.WithError(err).WithField("key", key).Error("Attempt to delete a notifier that doesn't exist")
		return err
	}

	delete(c.notifiers, key)
	c.logger.WithField("key", key).Debugf("Deleted a notifier")
	return nil
}
