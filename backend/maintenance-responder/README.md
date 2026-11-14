# FeatureHub Maintenance Responder

The **Maintenance Responder** is a small stand-in for the FeatureHub Management Repository (MR) during planned
maintenance, such as database upgrades or migrations.

- It serves the **Admin web app**, so users see a maintenance notice instead of an error page.
- It answers every admin API request (by default paths starting with `mr-api`, `saml` and `oauth`) with
  **HTTP 503 Service Unavailable**. The response body is the configured maintenance message, and a `Retry-After`
  header is set to the end of the maintenance window.
- Every response carries `X-Maintenance-Start`, `X-Maintenance-End` and `X-Maintenance-Message` headers.
- `/` returns 200, so load balancers that check the root path still route traffic to it.

It never connects to the database or the async layer, and SDK traffic (Edge) is not affected.

See [Maintenance Windows](https://docs.featurehub.io/featurehub/latest/maintenance.html) for the full guide.

## Typical use

In Kubernetes:

1. Deploy the Maintenance Responder alongside MR.
2. When the maintenance window starts, point the ingress for the Admin app at the Maintenance Responder.
3. Stop MR and carry out the database maintenance.
4. Start MR and point the ingress back at it.

The same pattern works on ECS and other platforms. See [Running a maintenance window](https://docs.featurehub.io/featurehub/latest/maintenance.html#_running_maintenance).

## Running

The maintenance window **must** be configured, or the container fails to start.

```sh
docker run -p 8085:8085 --user 999:999 \
  -e server.port=8085 \
  -e maintenance.start=2026-11-08T01:00:00Z \
  -e maintenance.end=2026-11-08T03:00:00Z \
  -e "maintenance.message=FeatureHub is down for scheduled maintenance" \
  featurehub/maintenance-responder:latest
```

## Ports

| Port | Purpose |
|------|---------|
| `8553` | HTTP (default `server.port`; set it to match your MR port, for example `8085`) |
| `8701` | Prometheus metrics and health checks (`monitor.port`) |

## Configuration

You can set any configuration property as an environment variable with the same name and case. You can also mount
`/etc/app-config/application.properties` and `/etc/app-config/secrets.properties`. See
[Run configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_run_configuration).

| Property | Description |
|----------|-------------|
| `maintenance.start` | **Required.** Start of the maintenance window, as an ISO-8601 instant (for example `2026-11-08T01:00:00Z`). |
| `maintenance.end` | **Required.** End of the maintenance window, as an ISO-8601 instant. Used for the `Retry-After` header. |
| `maintenance.message` | **Required.** Message returned in the 503 body and in the `X-Maintenance-Message` header. |
| `interceptPrefixes` | Comma-separated path prefixes that get a 503 (default `mr-api,saml,oauth,dacha2`). |
| `featurehub.url-path` | Context path, if your MR is served under a path other than `/`. |

You can also use the general server options in
[Common to all servers](https://docs.featurehub.io/featurehub/latest/configuration.html#_common_to_all_servers)
(for example `monitor.port` and `jersey.cors.headers`).

## Health and metrics

- `/health/liveness` and `/health/readiness`: see [Health and Liveness checks](https://docs.featurehub.io/featurehub/latest/configuration.html#_health_and_liveness_checks)
- `/metrics`: see [Monitoring](https://docs.featurehub.io/featurehub/latest/metrics.html)

## More information

- [Maintenance Windows](https://docs.featurehub.io/featurehub/latest/maintenance.html)
- Full documentation: **https://docs.featurehub.io**
- [Source code](https://github.com/featurehub-io/featurehub)

License: Apache 2 with Commons Clause.
