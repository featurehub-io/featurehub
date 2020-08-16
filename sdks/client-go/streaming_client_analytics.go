package client

// LogAnalyticsEvent submits analytics events using the client's configured AnalyticsCollector (as a background GoRoutine):
func (c *StreamingClient) LogAnalyticsEvent(action string, other map[string]string) {

	// Make a copy of our list of features (in case it changes underneath us):
	copyOfFeatures := c.features

	go c.analyticsCollector.LogEvent(action, other, copyOfFeatures)
}

// LogAnalyticsEventSync submits analytics events using the client's configured AnalyticsCollector (blocking until it is complete):
func (c *StreamingClient) LogAnalyticsEventSync(action string, other map[string]string) error {

	// Make a copy of our list of features (in case it changes underneath us):
	copyOfFeatures := c.features

	return c.analyticsCollector.LogEvent(action, other, copyOfFeatures)
}
