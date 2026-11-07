# FeatureHub base_party

`featurehub/base_party` is the **base image used to build the
[FeatureHub Party Server](https://hub.docker.com/r/featurehub/party-server)**. You don't run it directly. It is the
`FROM` image that the Party Server's Maven build (Google Jib) layers the application onto.

It is the same as [`featurehub/base_jdk`](https://hub.docker.com/r/featurehub/base_jdk) plus a **NATS server**.
The Party Server starts that NATS server internally, so a single container provides the Admin app, the Management
Repository, the Dacha2 cache and Edge with streaming.
See [Option 1a – Low Volume Deployment](https://docs.featurehub.io/featurehub/latest/installation.html#_option_1a_low_volume_deployment_streaming_rest_feature_updates).

To run FeatureHub, see the documentation at **https://docs.featurehub.io**.

## Contents

- **Eclipse Temurin 25 JRE** on Alpine Linux (`eclipse-temurin:25-jre-alpine`)
- the **NATS server** binary at `/target/nats-server` (taken from the official `nats:2.12-alpine` image), with its
  config at `/etc/nats/nats-server.conf`
- a non-root user `k8suser` with UID/GID **999**. The Party Server runs as `--user 999:999`.
- the directories FeatureHub expects, owned by UID 999:

| Directory | Purpose |
|-----------|---------|
| `/etc/common-config` | Default configuration and `log4j2.xml` baked into the Party Server image |
| `/etc/app-config` | Where you mount `application.properties` and `secrets.properties`. See [Run configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_run_configuration). |
| `/db` | Data directory, used for the H2 database in [evaluation](https://docs.featurehub.io/featurehub/latest/evaluation.html) setups |
| `/target` | Bundled binaries (NATS) |

Exposed ports: `8085` (Admin app, MR and Edge) and `8701` (metrics and health).

The image is published for `linux/amd64` and `linux/arm64`.

## Tags

Tags are base image versions (for example `2.2`), not FeatureHub versions. The version the Party Server uses is set
by `app.baseimage` in `backend/party-server/pom.xml`.

## Building

The source is in [`pipeline/build/base_party`](https://github.com/featurehub-io/featurehub/tree/main/pipeline/build/base_party).
To build and push a multi-architecture image (this requires `docker buildx` and a DockerHub login):

```sh
cd pipeline/build/base_party
sh make-dualarch-and-push.sh
```

When you bump the tag, or update NATS, change it in `make-dualarch-and-push.sh` and in `backend/party-server/pom.xml`.

## More information

- [FeatureHub documentation](https://docs.featurehub.io)
- [NATS configuration in FeatureHub](https://docs.featurehub.io/featurehub/latest/configuration.html#_nats)
- [Source code](https://github.com/featurehub-io/featurehub)
