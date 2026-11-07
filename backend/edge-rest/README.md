# FeatureHub Edge-REST

**Edge-REST** is a REST-only version of the FeatureHub SDK-facing server. It supports:

- **GET:** SDKs poll for feature state
- **PUT:** service accounts with write permission can change features (for example during automated tests)

It has no streaming (SSE) support. It reads **directly from the database**, so it needs no NATS and no Dacha cache.
Because its traffic is almost all reads, it works well against one or more **read replicas**. See
[Architecture – Edge (REST only)](https://docs.featurehub.io/featurehub/latest/architecture.html#_edge_rest_only).

Edge-REST is deployed alongside [`mr`](https://hub.docker.com/r/featurehub/mr) in
[Option 2b – Scalable Deployment (REST only)](https://docs.featurehub.io/featurehub/latest/installation.html#_option_2b_scalable_deployment_rest_feature_updates_only).
It is also bundled inside [`party-server-ish`](https://hub.docker.com/r/featurehub/party-server-ish).

Full documentation: **https://docs.featurehub.io**

## Running

```sh
docker run -p 8553:8553 --user 999:999 \
  -e db.url=jdbc:postgresql://db:5432/featurehub -e db.username=featurehub -e db.password=secret \
  -e db-replica.url=jdbc:postgresql://db-replica:5432/featurehub -e db-replica.username=featurehub -e db-replica.password=secret \
  featurehub/edge-rest:latest
```

A Docker Compose example is in `docker-compose-options/mr-edge-rest-postgres` in
[featurehub-install](https://github.com/featurehub-io/featurehub-install).

## Ports

| Port | Purpose |
|------|---------|
| `8553` | Edge REST API (`server.port`) |
| `8701` | Prometheus metrics and health checks (`monitor.port`) |

## Configuration

You can set any configuration property as an environment variable with the same name and case. You can also mount
`/etc/app-config/application.properties` and `/etc/app-config/secrets.properties`. See
[Run configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_run_configuration).

| Property | Description |
|----------|-------------|
| `db.url`, `db.username`, `db.password`, `db.minConnections`, `db.maxConnections` | Primary database connection. See [Database configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_database_configuration). |
| `db-replica.url`, `db-replica.username`, `db-replica.password` | Read replica used for GET requests. See [Database Read Replicas](https://docs.featurehub.io/featurehub/latest/configuration.html#_database_read_replicas). |
| `jersey.cors.headers` | CORS headers allowed for browser SDKs. |
| `edge.cache-control.header` | Override Cache-Control on GET (polling) responses to limit how often clients poll. |

See [Edge (all) Config](https://docs.featurehub.io/featurehub/latest/configuration.html#_edge_all_config) and
[Edge (REST only) Config](https://docs.featurehub.io/featurehub/latest/configuration.html#_edge_rest_only_config).

## Health and metrics

- `/health/liveness` and `/health/readiness`: see [Health and Liveness checks](https://docs.featurehub.io/featurehub/latest/configuration.html#_health_and_liveness_checks)
- `/metrics`: see [Edge metrics](https://docs.featurehub.io/featurehub/latest/metrics.html#_edge_metrics)

## More information

- [SDKs](https://docs.featurehub.io/featurehub/latest/sdks.html)
- [Configuration reference](https://docs.featurehub.io/featurehub/latest/configuration.html)
- [Source code](https://github.com/featurehub-io/featurehub)

License: Apache 2 with Commons Clause.
