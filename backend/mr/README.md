# FeatureHub Management Repository (MR)

The **Management Repository** is the FeatureHub admin server and the source of truth for the platform. It serves:

- the **Admin web app** (the Flutter admin console)
- the **MR API**, which manages portfolios, applications, environments, features, rollout strategies, users, groups and service accounts
- user login through local passwords, OAuth2 or SAML

MR stores its data in a relational database. It publishes changes to Dacha and Edge over an async layer (NATS, Kafka
or Google Pub/Sub). SDK clients do not talk to MR, so if MR goes down, feature delivery keeps working. See
[Architecture – Management Repository](https://docs.featurehub.io/featurehub/latest/architecture.html#_the_management_repository_mr_featurehub_server).

Use this image when you split FeatureHub into separate services, either with
[`dacha2`](https://hub.docker.com/r/featurehub/dacha2) and [`edge`](https://hub.docker.com/r/featurehub/edge) for streaming
([Option 2a](https://docs.featurehub.io/featurehub/latest/installation.html#_option_2a_scalable_deployment_streaming_rest_feature_updates)),
or with [`edge-rest`](https://hub.docker.com/r/featurehub/edge-rest) for REST only
([Option 2b](https://docs.featurehub.io/featurehub/latest/installation.html#_option_2b_scalable_deployment_rest_feature_updates_only)).
For a single-container install, use [`party-server`](https://hub.docker.com/r/featurehub/party-server).

Full documentation: **https://docs.featurehub.io**

## Running

Docker Compose examples live in [featurehub-install](https://github.com/featurehub-io/featurehub-install)
(for example `docker-compose-options/all-separate-postgres`). Production Kubernetes deployments use the
[Helm chart](https://github.com/featurehub-io/featurehub-helm).

```sh
docker run -p 8085:8085 --user 999:999 \
  -e db.url=jdbc:postgresql://db:5432/featurehub -e db.username=featurehub -e db.password=secret \
  -e nats.urls=nats://nats:4222 \
  featurehub/mr:latest
```

## Ports

| Port | Purpose |
|------|---------|
| `8085` | Admin web app and MR API (`server.port`) |
| `8701` | Prometheus metrics and health checks (`monitor.port`) |

## Configuration

You can set any configuration property as an environment variable with the same name and case. You can also mount
`/etc/app-config/application.properties` and `/etc/app-config/secrets.properties`. See
[Run configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_run_configuration).

| Property | Description |
|----------|-------------|
| `db.url`, `db.username`, `db.password`, `db.minConnections`, `db.maxConnections` | Database connection. See [Database configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_database_configuration). |
| `dacha1.enabled`, `dacha2.enabled` | Choose which cache MR publishes to. For Dacha2 (recommended), set `dacha1.enabled=false` and `dacha2.enabled=true`. See [Async options](https://docs.featurehub.io/featurehub/latest/configuration.html#_async_options_for_featurehub). |
| `nats.urls` | NATS servers. For Kafka or Google Pub/Sub settings, see [Async options](https://docs.featurehub.io/featurehub/latest/configuration.html#_async_options_for_featurehub). |
| `mr.dacha2.api-keys` | Exposes the Dacha2 cache-fill endpoint on the public interface, protected by these keys. See [Dacha2 Config](https://docs.featurehub.io/featurehub/latest/configuration.html#_dacha2_config). |
| `webhooks.encryption.password`, `webhooks.decryption.enabled` | Encryption of Slack tokens and webhook secrets. See [Encryption](https://docs.featurehub.io/featurehub/latest/configuration.html#_encryption). |
| `webhooks.features.enabled`, `enricher.enabled` | Turn webhooks on or off. See [Webhooks configuration](https://docs.featurehub.io/featurehub/latest/webhooks-configuration.html). |
| `portfolio.admin.group.suffix` | Suffix for auto-created portfolio admin groups (default `Administrators`). |
| `sdk.feature.properties` | Extra templated feature properties sent to SDKs. See [SDK Feature Extension Properties](https://docs.featurehub.io/featurehub/latest/configuration.html#_sdk_feature_extension_properties). |
| `web.asset.location`, `cache-control.web.index`, `cache-control.web.other` | Admin web app asset serving and caching. |
| `featurehub.url-path` | Serve FeatureHub under a context path other than `/`. See [Common to all servers](https://docs.featurehub.io/featurehub/latest/configuration.html#_common_to_all_servers). |

The full MR property list is in
[Management Repository configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_management_repository).
For OAuth2 and SAML setup, see [Identity](https://docs.featurehub.io/featurehub/latest/identity.html).

## Health and metrics

- `/health/liveness` and `/health/readiness`: see [Health and Liveness checks](https://docs.featurehub.io/featurehub/latest/configuration.html#_health_and_liveness_checks)
- `/metrics`: see [MR metrics](https://docs.featurehub.io/featurehub/latest/metrics.html#_management_repository)

## More information

- [Installation](https://docs.featurehub.io/featurehub/latest/installation.html)
- [Configuration reference](https://docs.featurehub.io/featurehub/latest/configuration.html)
- [Source code](https://github.com/featurehub-io/featurehub)

License: Apache 2 with Commons Clause.
