package models

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestFeatureStateTest(t *testing.T) {

	// Boolean:
	featureStateBool := &FeatureState{
		ID:    "featureStateBool",
		Type:  TypeBoolean,
		Value: true,
	}
	boolValue, err := featureStateBool.AsBoolean()
	assert.NoError(t, err)
	assert.Equal(t, true, boolValue)

	// Raw JSON:
	featureStateJSON := &FeatureState{
		ID:    "featureStateJSON",
		Type:  TypeJSON,
		Value: `{"is_testy":true}`,
	}
	rawJSONValue, err := featureStateJSON.AsRawJSON()
	assert.NoError(t, err)
	assert.Equal(t, `{"is_testy":true}`, rawJSONValue)

	// Number:
	featureStateNumber := &FeatureState{
		ID:    "featureStateNumber",
		Type:  TypeNumber,
		Value: 3.0,
	}
	numberValue, err := featureStateNumber.AsNumber()
	assert.NoError(t, err)
	assert.Equal(t, 3.0, numberValue)

	// String:
	featureStateString := &FeatureState{
		ID:    "featureStateString",
		Type:  TypeString,
		Value: "hello, i am a string",
	}
	stringValue, err := featureStateString.AsString()
	assert.NoError(t, err)
	assert.Equal(t, "hello, i am a string", stringValue)

	// WithContext (make sure we can add client context to a FeatureState):
	clientContext := &Context{
		Userkey: "some-key",
	}
	featureStateStringWithContext := featureStateString.WithContext(clientContext)
	assert.Equal(t, clientContext, featureStateStringWithContext.clientContext)
}
