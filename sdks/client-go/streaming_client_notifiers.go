package client

import (
	"fmt"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/google/uuid"
)

// AddNotifierBoolean adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifierBoolean(featureKey string, callbackFunc models.CallbackFuncBoolean) string {
	return c.addNotifier(notifier{
		callbackFuncBoolean: callbackFunc,
		featureKey:          featureKey,
		featureValueType:    models.TypeBoolean,
	})
}

// AddNotifierFeature adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifierFeature(featureKey string, callbackFunc models.CallbackFuncFeature) string {
	return c.addNotifier(notifier{
		callbackFuncFeature: callbackFunc,
		featureKey:          featureKey,
		featureValueType:    models.TypeFeature,
	})
}

// AddNotifierJSON adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifierJSON(featureKey string, callbackFunc models.CallbackFuncJSON) string {
	return c.addNotifier(notifier{
		callbackFuncJSON: callbackFunc,
		featureKey:       featureKey,
		featureValueType: models.TypeJSON,
	})
}

// AddNotifierNumber adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifierNumber(featureKey string, callbackFunc models.CallbackFuncNumber) string {
	return c.addNotifier(notifier{
		callbackFuncNumber: callbackFunc,
		featureKey:         featureKey,
		featureValueType:   models.TypeNumber,
	})
}

// AddNotifierString adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) AddNotifierString(featureKey string, callbackFunc models.CallbackFuncString) string {
	return c.addNotifier(notifier{
		callbackFuncString: callbackFunc,
		featureKey:         featureKey,
		featureValueType:   models.TypeString,
	})
}

// addNotifier adds a notifier callback function which will be executed any time the feature with the given key is updated:
func (c *StreamingClient) addNotifier(newNotifier notifier) string {
	c.notifiersMutex.Lock()
	defer c.notifiersMutex.Unlock()

	// Since we allow multiple notifiers for each feature key we need some way to distinguish them from each other:
	notifierUUID := uuid.New()
	newNotifier.uuid = notifierUUID.String()

	// Add the notifier:
	c.notifiers.add(newNotifier)
	c.logger.WithField("key", newNotifier.featureKey).WithField("uuid", newNotifier.uuid).Debug("Added a notifier")
	return newNotifier.uuid
}

// DeleteNotifier removes a notifier callback:
func (c *StreamingClient) DeleteNotifier(featureKey, notifierUUID string) error {
	c.notifiersMutex.Lock()
	defer c.notifiersMutex.Unlock()

	// First check that the given featureKey has any notifiers at all:
	featureKeyNotifiers, featureKeyExists := c.notifiers[featureKey]
	if !featureKeyExists {
		err := errors.NewErrNotifierNotFound(featureKey)
		c.logger.WithError(err).WithField("key", featureKey).Error("Attempt to delete a notifier that doesn't exist")
		return err
	}

	// Now check if a notifier for this UUID exists:
	if _, notifierExists := featureKeyNotifiers[notifierUUID]; !notifierExists {
		err := errors.NewErrNotifierNotFound(fmt.Sprintf("%s/%s", featureKey, notifierUUID))
		c.logger.WithError(err).WithField("key", featureKey).Error("Attempt to delete a notifier that doesn't exist")
		return err
	}

	// Now we can delete it:
	delete(c.notifiers[featureKey], notifierUUID)
	c.logger.WithField("key", featureKey).WithField("uuid", notifierUUID).Debug("Deleted a notifier")
	return nil
}

// notify triggers a callback:
func (c *StreamingClient) notify(feature *models.FeatureState) error {
	c.notifiersMutex.Lock()
	defer c.notifiersMutex.Unlock()

	// First check that the given featureKey has any notifiers at all:
	featureKeyNotifiers, featureKeyExists := c.notifiers[feature.Key]
	if !featureKeyExists {
		err := errors.NewErrNotifierNotFound(feature.Key)
		c.logger.WithError(err).WithField("key", feature.Key).Trace("Attempt to call a notifier that doesn't exist")
		return err
	}

	// Now we just trigger them all:
	for _, notifier := range featureKeyNotifiers {
		notifier.notify(feature)
		c.logger.WithField("key", feature.Key).WithField("uuid", notifier.uuid).Debug("Triggered a notifier")
	}

	return nil
}
