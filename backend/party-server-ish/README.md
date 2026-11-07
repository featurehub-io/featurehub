# FeatureHub Party Server-ish

**Party Server-ish** is the all-in-one, **REST-only** FeatureHub deployment. A single container runs:

- the **Admin web app** (the Flutter admin console)
- the **Management Repository (MR)**: the admin API and source of truth
- **Edge-REST**: the SDK-facing API for **GET (polling) and PUT** requests, which reads straight from the database

It includes no NATS, no Dacha cache and no streaming (SSE). Because of this it is simple and cheap to run on platforms
such as Google Cloud Run, Azure Container Instances or ECS. It suits mobile and web clients that poll for features.
See [Option 1b – Low Volume Deployment (REST only)](https://docs.featurehub.io/featurehub/latest/installation.html#_option_1b_low_volume_deployment_rest_feature_updates_only).

If you need streaming updates, use [`party-server`](https://hub.docker.com/r/featurehub/party-server). To scale REST
traffic across read replicas, split into [`mr`](https://hub.docker.com/r/featurehub/mr) +
[`edge-rest`](https://hub.docker.com/r/featurehub/edge-rest)
([Option 2b](https://docs.featurehub.io/featurehub/latest/installation.html#_option_2b_scalable_deployment_rest_feature_updates_only)).

Full documentation: **https://docs.featurehub.io**

## Quick start (evaluation)

```sh
docker run -p 8085:8085 --user 999:999 -v $HOME/party:/db featurehub/party-server-ish:latest
```

Open http://localhost:8085. This uses an embedded H2 database written to `/db`, which is for evaluation only.
For a Postgres example, see `docker-compose-options/party-server-ish-postgres` in
[featurehub-install](https://github.com/featurehub-io/featurehub-install). For a worked example, see
[Deploying on Google Cloud Run](https://docs.featurehub.io/featurehub/latest/installation.html#_deploying_featurehub_on_google_cloud_run_party_server_ish_example).

## Ports

| Port | Purpose |
|------|---------|
| `8085` | Admin web app, MR API and Edge REST API (`server.port`) |
| `8701` | Prometheus metrics and health checks (`monitor.port`) |

## Configuration

You can set any configuration property as an environment variable with the same name and case. You can also mount
`/etc/app-config/application.properties` and `/etc/app-config/secrets.properties`. See
[Run configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_run_configuration).

Party Server-ish accepts every property supported by the
[Management Repository](https://docs.featurehub.io/featurehub/latest/configuration.html#_management_repository) and
[Edge (REST only)](https://docs.featurehub.io/featurehub/latest/configuration.html#_edge_rest_only_config)
([Party-Server-ish config](https://docs.featurehub.io/featurehub/latest/configuration.html#_party_server_ish)).

Commonly used properties:

| Property | Description |
|----------|-------------|
| `db.url`, `db.username`, `db.password` | JDBC connection to your database (defaults to H2 at `/db`). See [Database configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_database_configuration). |
| `db-replica.*` | Optional read replica. Edge requests go to it by default. See [Database Read Replicas](https://docs.featurehub.io/featurehub/latest/configuration.html#_database_read_replicas). |
| `webhooks.encryption.password` | Key for encrypting Slack tokens and webhook secrets. See [Encryption](https://docs.featurehub.io/featurehub/latest/configuration.html#_encryption). |
| `edge.cache-control.header` | Override the Cache-Control header on Edge GET responses. See [Edge (all) Config](https://docs.featurehub.io/featurehub/latest/configuration.html#_edge_all_config). |
| `featurehub.url-path` | Serve FeatureHub under a context path other than `/`. |
| `monitor.port` | Port for metrics and health checks. Set to `0` to serve them on the main port (useful on Cloud Run or ECS). |

You can configure authentication through OAuth2 or SAML. See [Identity](https://docs.featurehub.io/featurehub/latest/identity.html).

## Health and metrics

- `/health/liveness` and `/health/readiness`: see [Health and Liveness checks](https://docs.featurehub.io/featurehub/latest/configuration.html#_health_and_liveness_checks)
- `/metrics`: Prometheus metrics, described in [Monitoring](https://docs.featurehub.io/featurehub/latest/metrics.html)

## More information

- [Architecture](https://docs.featurehub.io/featurehub/latest/architecture.html)
- [Configuration reference](https://docs.featurehub.io/featurehub/latest/configuration.html)
- [Source code](https://github.com/featurehub-io/featurehub)

License: Apache 2 with Commons Clause.
