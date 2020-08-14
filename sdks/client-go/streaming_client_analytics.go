package client

// LogAnalyticsEvent submits analytics events using the client's configured AnalyticsCollector:
func (c *StreamingClient) LogAnalyticsEvent(action string, other map[string]string) error {

	return c.analyticsCollector.LogEvent(action, other, c.features)
}
