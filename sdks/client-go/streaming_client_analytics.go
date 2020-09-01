package client

import (
	"reflect"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/interfaces"
)

// AddAnalyticsCollector configures the client with a new analytics collector:
func (c *StreamingClient) AddAnalyticsCollector(newAnalyticsCollector interfaces.AnalyticsCollector) {
	c.analyticsMutex.Lock()
	defer c.analyticsMutex.Unlock()

	c.analyticsCollectors = append(c.analyticsCollectors, newAnalyticsCollector)
}

// LogAnalyticsEvent submits analytics events using the client's configured AnalyticsCollector (as a background GoRoutine):
func (c *StreamingClient) LogAnalyticsEvent(action string, other map[string]string) {
	c.analyticsMutex.Lock()
	defer c.analyticsMutex.Unlock()

	// Make a copy of our list of features (in case it changes underneath us):
	copyOfFeatures := c.features

	// Submit events for each collector:
	for _, analyticsCollector := range c.analyticsCollectors {
		c.logger.WithField("analytics_collector", reflect.TypeOf(analyticsCollector)).Debug("Submitting analytics event")
		go analyticsCollector.LogEvent(action, other, copyOfFeatures)
	}
}

// LogAnalyticsEventSync submits analytics events using the client's configured AnalyticsCollector (blocking until it is complete):
func (c *StreamingClient) LogAnalyticsEventSync(action string, other map[string]string) error {
	c.analyticsMutex.Lock()
	defer c.analyticsMutex.Unlock()

	// Make a copy of our list of features (in case it changes underneath us):
	copyOfFeatures := c.features

	// Submit events for each collector:
	for _, analyticsCollector := range c.analyticsCollectors {
		c.logger.WithField("analytics_collector", reflect.TypeOf(analyticsCollector)).Debug("Submitting analytics event")
		if err := analyticsCollector.LogEvent(action, other, copyOfFeatures); err != nil {
			return err
		}
	}

	return nil
}
