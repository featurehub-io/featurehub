# FeatureHub Party Server

The **Party Server** is the all-in-one FeatureHub deployment. A single container runs:

- the **Admin web app** (the Flutter admin console)
- the **Management Repository (MR)**: the admin API and source of truth
- **Dacha2**: the feature cache
- **Edge**: the SDK-facing API, supporting both **streaming (SSE)** and **REST (GET/PUT)** feature updates
- an embedded **NATS** server that connects them

It is the image to use for [evaluation](https://docs.featurehub.io/featurehub/latest/evaluation.html) and for
[low-volume production deployments with streaming](https://docs.featurehub.io/featurehub/latest/installation.html#_option_1a_low_volume_deployment_streaming_rest_feature_updates)
(Option 1a). If you expect higher traffic, look at the separate [`mr`](https://hub.docker.com/r/featurehub/mr),
[`dacha2`](https://hub.docker.com/r/featurehub/dacha2) and [`edge`](https://hub.docker.com/r/featurehub/edge) images
([Option 2a](https://docs.featurehub.io/featurehub/latest/installation.html#_option_2a_scalable_deployment_streaming_rest_feature_updates)).
If you only need REST, use [`party-server-ish`](https://hub.docker.com/r/featurehub/party-server-ish).

Full documentation: **https://docs.featurehub.io**

## Quick start (evaluation)

```sh
docker run -p 8085:8085 --user 999:999 -v $HOME/party:/db featurehub/party-server:latest
```

Open http://localhost:8085 and create the first (superuser) account. This setup uses an embedded H2 database
written to `/db`. Use it for evaluation only, not production.

For Docker Compose examples (Postgres, MySQL, and others), see the
[featurehub-install](https://github.com/featurehub-io/featurehub-install) repository and the
[Installation guide](https://docs.featurehub.io/featurehub/latest/installation.html).

## Ports

| Port | Purpose |
|------|---------|
| `8085` | Admin web app, MR API and Edge API (`server.port`) |
| `8701` | Prometheus metrics and health checks (`monitor.port`) |

## Configuration

You can set any configuration property as an environment variable with the same name and case (for example
`-e db.url=...`). You can also mount properties files at:

- `/etc/app-config/application.properties`
- `/etc/app-config/secrets.properties`

See [Run configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_run_configuration).

The Party Server accepts every property supported by the
[Management Repository](https://docs.featurehub.io/featurehub/latest/configuration.html#_management_repository),
[Dacha2](https://docs.featurehub.io/featurehub/latest/configuration.html#_dacha2_config) and
[Edge (Streaming)](https://docs.featurehub.io/featurehub/latest/configuration.html#_edge_streaming_config)
([Party Server config](https://docs.featurehub.io/featurehub/latest/configuration.html#_party_server)).

Commonly used properties:

| Property | Description |
|----------|-------------|
| `db.url`, `db.username`, `db.password` | JDBC connection to your database. Postgres, MySQL, MariaDB, Oracle and SQL Server are supported. Defaults to H2 at `/db`. See [Database configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_database_configuration). |
| `db.minConnections`, `db.maxConnections` | Database connection pool sizing. |
| `nats.urls` | Comma-separated NATS servers. Only needed when you run several Party Servers or an external NATS. See [NATS](https://docs.featurehub.io/featurehub/latest/configuration.html#_nats). |
| `webhooks.encryption.password` | Key used to encrypt Slack tokens and webhook secrets at rest. Required for [Slack integration](https://docs.featurehub.io/featurehub/latest/slack-integration.html). See [Encryption](https://docs.featurehub.io/featurehub/latest/configuration.html#_encryption). |
| `featurehub.url-path` | Serve FeatureHub under a context path other than `/`. See [Common to all servers](https://docs.featurehub.io/featurehub/latest/configuration.html#_common_to_all_servers). |
| `portfolio.admin.group.suffix` | Suffix for the auto-created portfolio admin group (default `Administrators`). |
| `edge.sse.drop-after-seconds`, `edge.sse.heartbeat-period` | Lifetime and heartbeat behaviour of SSE connections. |
| `monitor.port` | Port for metrics and health checks. Set to `0` to serve them on the main port. |

You can configure authentication through OAuth2 (Google, GitHub, Azure, Keycloak, generic) or SAML. See
[Identity](https://docs.featurehub.io/featurehub/latest/identity.html).

To use Kafka or Google Pub/Sub instead of the embedded NATS, see
[Async options](https://docs.featurehub.io/featurehub/latest/configuration.html#_async_options_for_featurehub).

## Health and metrics

- `/health/liveness` and `/health/readiness`: see [Health and Liveness checks](https://docs.featurehub.io/featurehub/latest/configuration.html#_health_and_liveness_checks)
- `/metrics`: Prometheus metrics, described in [Monitoring](https://docs.featurehub.io/featurehub/latest/metrics.html)

## More information

- [Architecture](https://docs.featurehub.io/featurehub/latest/architecture.html)
- [Configuration reference](https://docs.featurehub.io/featurehub/latest/configuration.html)
- [Helm chart](https://github.com/featurehub-io/featurehub-helm)
- [Source code](https://github.com/featurehub-io/featurehub)

License: Apache 2 with Commons Clause.
