package client

import (
	"bytes"
	"encoding/json"
	"testing"

	"github.com/donovanhide/eventsource"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/strategies"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

var TestFeature1States = []*models.FeatureState{
	{
		ID:    "TestFeature1",
		Key:   "TestFeature1",
		Type:  models.TypeString,
		Value: "this is the default value",
		Strategies: []models.Strategy{
			{
				ID:    "s1",
				Name:  "country-russia",
				Value: "this is for the russians",
				Attributes: []*models.StrategyAttribute{
					{
						ID:          "a1",
						Conditional: strategies.ConditionalEquals,
						FieldName:   strategies.FieldNameCountry,
						Values:      []interface{}{"russia"},
						Type:        strategies.TypeString,
					},
				},
			},
			{
				ID:    "s2",
				Name:  "platform-unix",
				Value: "this is for unix users",
				Attributes: []*models.StrategyAttribute{
					{
						ID:          "a2",
						Conditional: strategies.ConditionalEquals,
						FieldName:   strategies.FieldNamePlatform,
						Values:      []interface{}{string(models.ContextPlatformLinux), string(models.ContextPlatformMacos)},
						Type:        strategies.TypeString,
					},
				},
			},
			{
				ID:    "s3",
				Name:  "device-notmobile",
				Value: "this is not for mobile users",
				Attributes: []*models.StrategyAttribute{
					{
						ID:          "a3",
						Conditional: strategies.ConditionalNotEquals,
						FieldName:   strategies.FieldNameDevice,
						Values:      []interface{}{string(models.ContextDeviceMobile), string(models.ContextDeviceWatch)},
						Type:        strategies.TypeString,
					},
				},
			},
			{
				ID:    "s4",
				Name:  "version-less",
				Value: "version less than 15.23.4",
				Attributes: []*models.StrategyAttribute{
					{
						ID:          "a4",
						Conditional: strategies.ConditionalLess,
						FieldName:   strategies.FieldNameVersion,
						Values:      []interface{}{"15.23.4"},
						Type:        strategies.TypeSemanticVersion,
					},
				},
			},
			{
				ID:    "s5",
				Name:  "version-lessequals",
				Value: "version less than or equal to 15.23.4",
				Attributes: []*models.StrategyAttribute{
					{
						ID:          "a5",
						Conditional: strategies.ConditionalLessEquals,
						FieldName:   strategies.FieldNameVersion,
						Values:      []interface{}{"15.23.4"},
						Type:        strategies.TypeSemanticVersion,
					},
				},
			},
			{
				ID:    "s6",
				Name:  "version-greater",
				Value: "version greater than 16.0.0",
				Attributes: []*models.StrategyAttribute{
					{
						ID:          "a6",
						Conditional: strategies.ConditionalGreater,
						FieldName:   strategies.FieldNameVersion,
						Values:      []interface{}{"16.0.0"},
						Type:        strategies.TypeSemanticVersion,
					},
				},
			},
			{
				ID:    "s7",
				Name:  "version-greaterequals",
				Value: "version greater than or equal to 16.0.0",
				Attributes: []*models.StrategyAttribute{
					{
						ID:          "a7",
						Conditional: strategies.ConditionalGreaterEquals,
						FieldName:   strategies.FieldNameVersion,
						Values:      []interface{}{"16.0.0"},
						Type:        strategies.TypeSemanticVersion,
					},
				},
			},
		},
	},
	{
		ID:    "TestFeature2",
		Key:   "TestFeature2",
		Type:  models.TypeString,
		Value: "this is the default value",
		Strategies: []models.Strategy{
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
	},
}

func TestStreamingClientWithContext(t *testing.T) {

	// Make a test config:
	config := &Config{
		WaitForData: true,
	}

	// Make a logger:
	logger := logrus.New()
	logger.SetLevel(logrus.TraceLevel)
	logBuffer := new(bytes.Buffer)
	logger.SetOutput(logBuffer)

	// Use the config to make a new StreamingClient with a mock apiClient::
	testClient := &StreamingClient{
		apiClient: &eventsource.Stream{
			Errors: make(chan error, 100),
			Events: make(chan eventsource.Event, 100),
		},
		config:   config,
		features: make(map[string]*models.FeatureState),
		logger:   logger,
	}

	// Make a client context:
	testContext := &models.Context{
		Userkey: "TestStreamingClientWithContext",
	}

	// Make sure our client and context are present:
	streamingClientWithContext := testClient.WithContext(testContext)
	assert.Equal(t, testClient, streamingClientWithContext.client)
	assert.Equal(t, testContext, streamingClientWithContext.context)

	// Marshal the TestFeature1States to JSON:
	TestFeature1StatesJSON, err := json.Marshal(TestFeature1States)
	assert.NoError(t, err)

	// Load the mock apiClient up with a "features" event:
	testClient.apiClient.Events <- &testEvent{
		data:  string(TestFeature1StatesJSON),
		event: "features",
	}

	// Start handling events:
	testClient.Start()

	// First make sure that we get the default value before client-context is added:
	stringValue, err := testClient.GetString("TestFeature1")
	assert.NoError(t, err)
	assert.Equal(t, "this is the default value", stringValue)

	// See if we can match the "country-russia" attribute:
	stringValue, err = testClient.
		WithContext(&models.Context{Country: models.ContextCountryRussia}).
		GetString("TestFeature1")
	assert.Equal(t, "this is for the russians", stringValue)
	assert.NoError(t, err)

	// See if we can match the "platform-unix" attribute:
	stringValue, err = testClient.
		WithContext(&models.Context{Platform: models.ContextPlatformMacos}).
		GetString("TestFeature1")
	assert.Equal(t, "this is for unix users", stringValue)
	assert.NoError(t, err)

	// See if we can match the "device-notmobile" attribute:
	stringValue, err = testClient.
		WithContext(&models.Context{Device: models.ContextDeviceServer}).
		GetString("TestFeature1")
	assert.Equal(t, "this is not for mobile users", stringValue)
	assert.NoError(t, err)

	// See if we can match the "version-less" attribute:
	stringValue, err = testClient.
		WithContext(&models.Context{Version: "5.6.7"}).
		GetString("TestFeature1")
	assert.Equal(t, "version less than 15.23.4", stringValue)
	assert.NoError(t, err)

	// See if we can match the "version-lessequal" attribute:
	stringValue, err = testClient.
		WithContext(&models.Context{Version: "15.23.4"}).
		GetString("TestFeature1")
	assert.Equal(t, "version less than or equal to 15.23.4", stringValue)
	assert.NoError(t, err)

	// See if we can match the "version-greater" attribute:
	stringValue, err = testClient.
		WithContext(&models.Context{Version: "16.0.1"}).
		GetString("TestFeature1")
	assert.Equal(t, "version greater than 16.0.0", stringValue)
	assert.NoError(t, err)

	// See if we can match the "version-greaterequals" attribute:
	stringValue, err = testClient.
		WithContext(&models.Context{Version: "16.0.0"}).
		GetString("TestFeature1")
	assert.Equal(t, "version greater than or equal to 16.0.0", stringValue)
	assert.NoError(t, err)

	// Look for a 33% rule (based on a pre-calculated hash):
	stringValue, err = testClient.
		WithContext(&models.Context{Userkey: "1111111111"}).
		GetString("TestFeature2")
	assert.Equal(t, "this is for the 33 percent", stringValue)
	assert.NoError(t, err)

	// Look for a 66% rule (based on a pre-calculated hash):
	stringValue, err = testClient.
		WithContext(&models.Context{
			Userkey: "1111111111",
			Session: "4444444444",
		}).
		GetString("TestFeature2")
	assert.Equal(t, "this is for the 66 percent", stringValue)
	assert.NoError(t, err)
}
