package models

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestStrategiesByPercentage(t *testing.T) {

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

func TestStrategiesByAttribute(t *testing.T) {

	// Prepare a feature state with strategies:
	featureState := &FeatureState{
		ID:    "TestFeature",
		Type:  TypeString,
		Value: "this is the default value",
		Strategies: []Strategy{
			{
				ID:    "s1",
				Name:  "country-russia",
				Value: "this is for the russians",
				Attributes: []*StrategyAttribute{
					{
						ID:          "a1",
						Conditional: strategyConditionalEquals,
						FieldName:   strategyFieldNameCountry,
						Values:      []string{"russia"},
					},
				},
			},
			{
				ID:    "s2",
				Name:  "platform-unix",
				Value: "this is for unix users",
				Attributes: []*StrategyAttribute{
					{
						ID:          "a2",
						Conditional: strategyConditionalEquals,
						FieldName:   strategyFieldNamePlatform,
						Values:      []string{string(ContextPlatformLinux), string(ContextPlatformMacos)},
					},
				},
			},
			{
				ID:    "s3",
				Name:  "device-notmobile",
				Value: "this is not for mobile users",
				Attributes: []*StrategyAttribute{
					{
						ID:          "a3",
						Conditional: strategyConditionalNotEquals,
						FieldName:   strategyFieldNameDevice,
						Values:      []string{string(ContextDeviceMobile), string(ContextDeviceWatch)},
					},
				},
			},
			{
				ID:    "s4",
				Name:  "version-less",
				Value: "version less than 15.23.4",
				Attributes: []*StrategyAttribute{
					{
						ID:          "a4",
						Conditional: strategyConditionalLess,
						FieldName:   strategyFieldNameVersion,
						Values:      []string{"15.23.4"},
						Type:        strategyTypeSemanticVersion,
					},
				},
			},
			{
				ID:    "s5",
				Name:  "version-lessequals",
				Value: "version less than or equal to 15.23.4",
				Attributes: []*StrategyAttribute{
					{
						ID:          "a5",
						Conditional: strategyConditionalLessEquals,
						FieldName:   strategyFieldNameVersion,
						Values:      []string{"15.23.4"},
						Type:        strategyTypeSemanticVersion,
					},
				},
			},
			{
				ID:    "s6",
				Name:  "version-greater",
				Value: "version greater than 16.0.0",
				Attributes: []*StrategyAttribute{
					{
						ID:          "a6",
						Conditional: strategyConditionalGreater,
						FieldName:   strategyFieldNameVersion,
						Values:      []string{"16.0.0"},
						Type:        strategyTypeSemanticVersion,
					},
				},
			},
			{
				ID:    "s7",
				Name:  "version-greaterequals",
				Value: "version greater than or equal to 16.0.0",
				Attributes: []*StrategyAttribute{
					{
						ID:          "a7",
						Conditional: strategyConditionalGreaterEquals,
						FieldName:   strategyFieldNameVersion,
						Values:      []string{"16.0.0"},
						Type:        strategyTypeSemanticVersion,
					},
				},
			},
		},
	}

	// A client context to use for our tests:
	clientContext := new(Context)

	// First make sure that we get the default value before client-context is added:
	stringValue, err := featureState.AsString()
	assert.NoError(t, err)
	assert.Equal(t, "this is the default value", stringValue)

	// See if we can match the "country-russia" attribute:
	clientContext = &Context{Country: "russia"}
	stringValue, err = featureState.WithContext(clientContext).AsString()
	assert.Equal(t, "this is for the russians", stringValue)
	assert.NoError(t, err)
	clientContext = new(Context)

	// See if we can match the "platform-unix" attribute:
	clientContext = &Context{Platform: ContextPlatformMacos}
	stringValue, err = featureState.WithContext(clientContext).AsString()
	assert.Equal(t, "this is for unix users", stringValue)
	assert.NoError(t, err)

	// See if we can match the "device-notmobile" attribute:
	clientContext = &Context{Device: ContextDeviceServer}
	stringValue, err = featureState.WithContext(clientContext).AsString()
	assert.Equal(t, "this is not for mobile users", stringValue)
	assert.NoError(t, err)

	// See if we can match the "version-less" attribute:
	clientContext = &Context{Version: "5.6.7"}
	stringValue, err = featureState.WithContext(clientContext).AsString()
	assert.Equal(t, "version less than 15.23.4", stringValue)
	assert.NoError(t, err)

	// See if we can match the "version-lessequal" attribute:
	clientContext = &Context{Version: "15.23.4"}
	stringValue, err = featureState.WithContext(clientContext).AsString()
	assert.Equal(t, "version less than or equal to 15.23.4", stringValue)
	assert.NoError(t, err)

	// See if we can match the "version-greater" attribute:
	clientContext = &Context{Version: "16.0.1"}
	stringValue, err = featureState.WithContext(clientContext).AsString()
	assert.Equal(t, "version greater than 16.0.0", stringValue)
	assert.NoError(t, err)

	// See if we can match the "version-greaterequals" attribute:
	clientContext = &Context{Version: "16.0.0"}
	stringValue, err = featureState.WithContext(clientContext).AsString()
	assert.Equal(t, "version greater than or equal to 16.0.0", stringValue)
	assert.NoError(t, err)
}
