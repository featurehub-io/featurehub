# FeatureHub Edge (Streaming + REST)

**Edge** is the SDK-facing FeatureHub server. This image is published as `featurehub/edge`. It serves feature state
to SDKs through:

- **Streaming:** Server-Sent Events (SSE), with near real-time updates
- **REST:** GET polling, plus PUT for service accounts allowed to change features (for example in automated tests)

Edge keeps almost no state. It knows only which clients are connected and which environments they listen to. It gets
feature data from the **Dacha** cache and receives feature updates over the async layer (NATS, Kafka or Google
Pub/Sub). It never connects to the database, so you can scale it out freely. Run at least two instances in production.
See [Architecture – Edge (Streaming+REST)](https://docs.featurehub.io/featurehub/latest/architecture.html#_edge_streamingrest).

Edge is part of the scalable streaming deployment
([Option 2a](https://docs.featurehub.io/featurehub/latest/installation.html#_option_2a_scalable_deployment_streaming_rest_feature_updates)),
together with [`mr`](https://hub.docker.com/r/featurehub/mr) and [`dacha2`](https://hub.docker.com/r/featurehub/dacha2).
For REST only, backed by the database, see [`edge-rest`](https://hub.docker.com/r/featurehub/edge-rest).

Full documentation: **https://docs.featurehub.io**

## Running

With Dacha2 (recommended):

```sh
docker run -p 8553:8553 --user 999:999 \
  -e dacha1.enabled=false -e dacha2.enabled=true \
  -e dacha.url.default=http://dacha2:8902 \
  -e nats.urls=nats://nats:4222 \
  featurehub/edge:latest
```

## Ports

| Port | Purpose |
|------|---------|
| `8553` | Edge SDK API, SSE and REST (`server.port`) |
| `8701` | Prometheus metrics and health checks (`monitor.port`) |

## Configuration

You can set any configuration property as an environment variable with the same name and case. You can also mount
`/etc/app-config/application.properties` and `/etc/app-config/secrets.properties`. See
[Run configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_run_configuration).

| Property | Description |
|----------|-------------|
| `dacha1.enabled`, `dacha2.enabled` | Which cache to use. For Dacha2, set `dacha1.enabled=false` and `dacha2.enabled=true`. See [Async options](https://docs.featurehub.io/featurehub/latest/configuration.html#_async_options_for_featurehub). |
| `dacha.url.default` | URL of the Dacha2 service. **Required for Dacha2.** |
| `nats.urls` | NATS servers. For Kafka or Google Pub/Sub, see [Async options](https://docs.featurehub.io/featurehub/latest/configuration.html#_async_options_for_featurehub). |
| `jersey.cors.headers` | CORS headers allowed for browser SDKs. |
| `edge.sse.drop-after-seconds` | How long (default `30`) an SSE client stays connected before it is disconnected. `0` keeps connections open. |
| `edge.sse.heartbeat-period` | Interval for heartbeat events on SSE connections (default `0`, off). |
| `edge.dacha.delay-slots` | Random reconnect delay window when Dacha is not ready, to prevent reconnect storms. |
| `edge.cache-control.header` | Override Cache-Control on GET (polling) responses. |
| `update.pool-size`, `listen.pool-size` | Thread pools for incoming updates and incoming listen requests. |

See [Edge (all) Config](https://docs.featurehub.io/featurehub/latest/configuration.html#_edge_all_config) and
[Edge (Streaming) Config](https://docs.featurehub.io/featurehub/latest/configuration.html#_edge_streaming_config).

## Health and metrics

- `/health/liveness` and `/health/readiness`: see [Health and Liveness checks](https://docs.featurehub.io/featurehub/latest/configuration.html#_health_and_liveness_checks)
- `/metrics`: see [Edge metrics](https://docs.featurehub.io/featurehub/latest/metrics.html#_edge_metrics)

## More information

- [SDKs](https://docs.featurehub.io/featurehub/latest/sdks.html)
- [Configuration reference](https://docs.featurehub.io/featurehub/latest/configuration.html)
- [Helm chart](https://github.com/featurehub-io/featurehub-helm)
- [Source code](https://github.com/featurehub-io/featurehub)

License: Apache 2 with Commons Clause.
