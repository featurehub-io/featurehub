mocks:
	@mkdir -p pkg/mocks
	@counterfeiter -o pkg/mocks/client.go pkg/interfaces Client
	@counterfeiter -o pkg/mocks/analytics_collector.go pkg/interfaces AnalyticsCollector

test:
	@go test ./... -cover
