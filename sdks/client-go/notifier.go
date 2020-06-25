package client

import (
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/errors"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

// Notifier ties together a feature, type and callback function:
type notifier struct {
	callbackFuncFeature models.CallbackFuncFeature
	callbackFuncBoolean models.CallbackFuncBoolean
	callbackFuncJSON    models.CallbackFuncJSON
	callbackFuncNumber  models.CallbackFuncNumber
	callbackFuncString  models.CallbackFuncString
	featureKey          string
	featureValueType    models.FeatureValueType
}

// notify triggers the appropriate callback function for this notifier type:
func (n *notifier) notify(feature *models.FeatureState) error {

	// Switch on the stored type:
	switch n.featureValueType {

	case models.TypeBoolean:
		assertedValue, ok := feature.Value.(bool)
		if !ok {
			return errors.NewErrInvalidType("Unable to assert as bool")
		}
		n.callbackFuncBoolean(assertedValue)

	case models.TypeFeature:
		n.callbackFuncFeature(feature)

	case models.TypeJSON:
		assertedValue, ok := feature.Value.(string)
		if !ok {
			return errors.NewErrInvalidType("Unable to assert as string")
		}
		n.callbackFuncJSON(assertedValue)

	case models.TypeNumber:
		assertedValue, ok := feature.Value.(float64)
		if !ok {
			return errors.NewErrInvalidType("Unable to assert as int64")
		}
		n.callbackFuncNumber(assertedValue)

	case models.TypeString:
		assertedValue, ok := feature.Value.(string)
		if !ok {
			return errors.NewErrInvalidType("Unable to assert as string")
		}
		n.callbackFuncString(assertedValue)

	default:
		return errors.NewErrInvalidType(string(n.featureValueType))
	}

	return nil
}
