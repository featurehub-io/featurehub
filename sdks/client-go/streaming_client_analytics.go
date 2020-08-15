package client

// LogAnalyticsEvent submits analytics events using the client's configured AnalyticsCollector:
func (c *StreamingClient) LogAnalyticsEvent(action string, other map[string]string) error {

	// Make a copy of our list of features (in case it changes underneath us):
	copyOfFeatures := c.features

	return c.analyticsCollector.LogEvent(action, other, copyOfFeatures)
}
