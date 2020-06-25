package client

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

// AddNotifierBoolean adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifierBoolean(key string, callbackFunc models.CallbackFuncBoolean) {
	c.notifiersMutex.Lock()
	defer c.notifiersMutex.Unlock()
	c.notifiers[key] = notifier{
		callbackFuncBoolean: callbackFunc,
		featureKey:          key,
		featureValueType:    models.TypeBoolean,
	}
	c.logger.WithField("key", key).Debug("Added a notifier")
}

// AddNotifierFeature adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifierFeature(key string, callbackFunc models.CallbackFuncFeature) {
	c.notifiersMutex.Lock()
	defer c.notifiersMutex.Unlock()
	c.notifiers[key] = notifier{
		callbackFuncFeature: callbackFunc,
		featureKey:          key,
		featureValueType:    models.TypeFeature,
	}
	c.logger.WithField("key", key).Debug("Added a notifier")
}

// AddNotifierJSON adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifierJSON(key string, callbackFunc models.CallbackFuncJSON) {
	c.notifiersMutex.Lock()
	defer c.notifiersMutex.Unlock()
	c.notifiers[key] = notifier{
		callbackFuncJSON: callbackFunc,
		featureKey:       key,
		featureValueType: models.TypeJSON,
	}
	c.logger.WithField("key", key).Debug("Added a notifier")
}

// AddNotifierNumber adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifierNumber(key string, callbackFunc models.CallbackFuncNumber) {
	c.notifiersMutex.Lock()
	defer c.notifiersMutex.Unlock()
	c.notifiers[key] = notifier{
		callbackFuncNumber: callbackFunc,
		featureKey:         key,
		featureValueType:   models.TypeNumber,
	}
	c.logger.WithField("key", key).Debug("Added a notifier")
}

// AddNotifierString adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifierString(key string, callbackFunc models.CallbackFuncString) {
	c.notifiersMutex.Lock()
	defer c.notifiersMutex.Unlock()
	c.notifiers[key] = notifier{
		callbackFuncString: callbackFunc,
		featureKey:         key,
		featureValueType:   models.TypeString,
	}
	c.logger.WithField("key", key).Debug("Added a notifier")
}

// DeleteNotifier removes a notifier callback:
func (c *StreamingClient) DeleteNotifier(key string) error {
	c.notifiersMutex.Lock()
	defer c.notifiersMutex.Unlock()
	if _, ok := c.notifiers[key]; !ok {
		err := errors.NewErrNotifierNotFound(key)
		c.logger.WithError(err).WithField("key", key).Error("Attempt to delete a notifier that doesn't exist")
		return err
	}

	delete(c.notifiers, key)
	c.logger.WithField("key", key).Debug("Deleted a notifier")
	return nil
}

// notify triggers a callback:
func (c *StreamingClient) notify(feature *models.FeatureState) error {
	c.notifiersMutex.Lock()
	defer c.notifiersMutex.Unlock()
	notifier, ok := c.notifiers[feature.Key]
	if !ok {
		err := errors.NewErrNotifierNotFound(feature.Key)
		c.logger.WithError(err).WithField("key", feature.Key).Trace("Attempt to call a notifier that doesn't exist")
		return err
	}

	// Trigger the callback:
	go notifier.notify(feature)
	c.logger.WithField("key", feature.Key).Debug("Triggered a notifier")
	return nil
}
