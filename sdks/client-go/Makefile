mocks:
	@mkdir -p pkg/mocks
	@counterfeiter -o pkg/mocks/client.go pkg/interfaces Client

test:
	@go test -cover
