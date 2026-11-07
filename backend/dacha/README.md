# FeatureHub Dacha (Dacha1 Cache)

**Dacha** (Dacha1) is FeatureHub's original "active" feature cache. It sits between the Management Repository (MR)
and Edge, and holds the environments, service accounts and features Edge needs to answer SDK requests.

When it starts, Dacha1 fills its whole cache, either from another Dacha instance or from MR. This shields MR from
miss traffic. The cost is slower start-up when you have many environments, and MR must be available during the fill.
Dacha1 **requires NATS**.

> **New installations should use [`dacha2`](https://hub.docker.com/r/featurehub/dacha2)**, the lazy, CloudEvents-based
> cache that supports NATS, Kafka and Google Pub/Sub. It has been recommended since FeatureHub 1.5.9.
> See [Architectural Choices for Dacha](https://docs.featurehub.io/featurehub/latest/architecture.html#_architectural_choices_for_dacha).

Full documentation: **https://docs.featurehub.io**

## Running

```sh
docker run --user 999:999 -e nats.urls=nats://nats:4222 featurehub/dacha:latest
```

## Ports

| Port | Purpose |
|------|---------|
| `8902` | Dacha API (`server.port`) |
| `8279` | Prometheus metrics and health checks (`monitor.port`) |

## Configuration

You can set any configuration property as an environment variable with the same name and case. You can also mount
`/etc/app-config/application.properties` and `/etc/app-config/secrets.properties`. See
[Run configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_run_configuration).

| Property | Description |
|----------|-------------|
| `nats.urls` | Comma-separated NATS servers. See [NATS](https://docs.featurehub.io/featurehub/latest/configuration.html#_nats). |
| `cache.name` | Name of the cache group (default `default`). |
| `cache.timeout` | Time in ms to find and resolve a master cache (default `5000`). |
| `cache.complete-timeout` | Time in ms to wait for data after another cache becomes master (default `15000`). |
| `cache.pool-size` | Worker thread pool size (default `10`). |

See [Dacha1 Config](https://docs.featurehub.io/featurehub/latest/configuration.html#_dacha1_config) and the
[NATS channels](https://docs.featurehub.io/featurehub/latest/configuration.html#_channels) Dacha1 uses.

## Health and metrics

- `/health/liveness` and `/health/readiness`: see [Health and Liveness checks](https://docs.featurehub.io/featurehub/latest/configuration.html#_health_and_liveness_checks)
- `/metrics`: see [Monitoring](https://docs.featurehub.io/featurehub/latest/metrics.html)

## More information

- [Architecture](https://docs.featurehub.io/featurehub/latest/architecture.html)
- [Configuration reference](https://docs.featurehub.io/featurehub/latest/configuration.html)
- [Source code](https://github.com/featurehub-io/featurehub)

License: Apache 2 with Commons Clause.
