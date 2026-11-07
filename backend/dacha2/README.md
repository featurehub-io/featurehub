# FeatureHub Dacha2 (Cache)

**Dacha2** is FeatureHub's lazy feature cache and the recommended cache since 1.5.9. It sits between the
Management Repository (MR) and Edge. It holds the environments, service accounts, features and pre-calculated etags
that Edge needs to answer SDK requests, so SDK traffic never reaches MR or the database.

- **Lazy loading:** the first time it sees an environment/API key, it asks MR for the data, then keeps it up to date from the event stream.
- **Caches misses too:** repeated requests for unknown keys never reach MR.
- **CloudEvents based:** works with NATS, Kafka or Google Pub/Sub.

See [Architecture – Dacha](https://docs.featurehub.io/featurehub/latest/architecture.html#_architectural_choices_for_dacha).

Dacha2 is used in the scalable streaming deployment
([Option 2a](https://docs.featurehub.io/featurehub/latest/installation.html#_option_2a_scalable_deployment_streaming_rest_feature_updates))
together with [`mr`](https://hub.docker.com/r/featurehub/mr) and [`edge`](https://hub.docker.com/r/featurehub/edge).
Dacha2 should not be exposed outside your network; only Edge needs to reach it.

Full documentation: **https://docs.featurehub.io**

## Running

```sh
docker run --user 999:999 \
  -e management-repository.url=http://mr:8085 \
  -e nats.urls=nats://nats:4222 \
  featurehub/dacha2:latest
```

MR must run with `dacha1.enabled=false` and `dacha2.enabled=true`. Edge needs `dacha.url.default` pointed at this
service. See [Async options](https://docs.featurehub.io/featurehub/latest/configuration.html#_async_options_for_featurehub).

## Ports

| Port | Purpose |
|------|---------|
| `8902` | Dacha2 API used by Edge (`server.port`) |
| `8279` | Prometheus metrics and health checks (`monitor.port`) |

## Configuration

You can set any configuration property as an environment variable with the same name and case. You can also mount
`/etc/app-config/application.properties` and `/etc/app-config/secrets.properties`. See
[Run configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_run_configuration).

| Property | Description |
|----------|-------------|
| `management-repository.url` | URL of MR, including the port. Dacha2 uses it to fill cache misses. |
| `dacha2.cache.api-key` | Key sent to MR when MR protects its Dacha2 endpoint with `mr.dacha2.api-keys`. Use it when Dacha2 runs in a different cluster from MR. |
| `dacha2.streaming.disconnect-behaviour` | What to do when the streaming layer disconnects: `on-reconnect` (default), `use-passthrough` or `standard`. |
| `nats.urls` | NATS servers. For Kafka or Google Pub/Sub, see [Async options](https://docs.featurehub.io/featurehub/latest/configuration.html#_async_options_for_featurehub). |

These and further options are described in
[Dacha2 Config](https://docs.featurehub.io/featurehub/latest/configuration.html#_dacha2_config).

## Health and metrics

- `/health/liveness` and `/health/readiness`: see [Health and Liveness checks](https://docs.featurehub.io/featurehub/latest/configuration.html#_health_and_liveness_checks)
- `/metrics`: see [Dacha2 metrics](https://docs.featurehub.io/featurehub/latest/metrics.html#_dacha2_metrics)

## More information

- [Installation](https://docs.featurehub.io/featurehub/latest/installation.html)
- [Configuration reference](https://docs.featurehub.io/featurehub/latest/configuration.html)
- [Helm chart](https://github.com/featurehub-io/featurehub-helm)
- [Source code](https://github.com/featurehub-io/featurehub)

License: Apache 2 with Commons Clause.
