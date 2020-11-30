package models

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestStrategies(t *testing.T) {

	// Prepare a feature state with strategies:
	featureState := &FeatureState{
		ID:    "TestFeature1",
		Type:  TypeString,
		Value: "this is the default value",
		Strategies: []Strategy{
			{
				ID:         "33",
				Name:       "33Percent",
				Percentage: 330000,
				Value:      "this is for the 33 percent",
			},
			{
				ID:         "66",
				Name:       "66Percent",
				Percentage: 660000,
				Value:      "this is for the 66 percent",
			},
		},
	}

	// A client context with userkey only:
	clientContextUserkeyOnly := &Context{
		Userkey: "1111111111", // Hashes to 27.8%
	}

	// A client context with session and userkey (should prefer session):
	clientContextSessionAndUserkey := &Context{
		Session: "4444444444", // Hashes to 47.2%
		Userkey: "1111111111", // Hashes to 27.8%
	}

	// First make sure that we get the default value before client-context is added:
	stringValue, err := featureState.AsString()
	assert.NoError(t, err)
	assert.Equal(t, "this is the default value", stringValue)

	// Add the first client-context:
	someString, err := featureState.WithContext(clientContextUserkeyOnly).AsString()
	assert.Equal(t, "this is for the 33 percent", someString)
	assert.NoError(t, err)

	// Now try with the second context:
	anotherString, err := featureState.WithContext(clientContextSessionAndUserkey).AsString()
	assert.Equal(t, "this is for the 66 percent", anotherString)
	assert.NoError(t, err)
}
