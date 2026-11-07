# FeatureHub base_jdk

`featurehub/base_jdk` is the **base image used to build the FeatureHub server containers**. You don't run it
directly. It is the `FROM` image that the FeatureHub Maven build (Google Jib) layers the application onto.

It is used by:

- [`featurehub/mr`](https://hub.docker.com/r/featurehub/mr)
- [`featurehub/party-server-ish`](https://hub.docker.com/r/featurehub/party-server-ish)
- [`featurehub/edge`](https://hub.docker.com/r/featurehub/edge)
- [`featurehub/edge-rest`](https://hub.docker.com/r/featurehub/edge-rest)
- [`featurehub/dacha`](https://hub.docker.com/r/featurehub/dacha)
- [`featurehub/maintenance-responder`](https://hub.docker.com/r/featurehub/maintenance-responder)

[`featurehub/party-server`](https://hub.docker.com/r/featurehub/party-server) uses
[`featurehub/base_party`](https://hub.docker.com/r/featurehub/base_party) instead, which adds NATS.

To run FeatureHub, see the documentation at **https://docs.featurehub.io**, starting with
[Installation](https://docs.featurehub.io/featurehub/latest/installation.html).

## Contents

- **Eclipse Temurin 25 JRE** on Alpine Linux (`eclipse-temurin:25-jre-alpine`)
- a non-root user `k8suser` with UID/GID **999**. FeatureHub containers run as `--user 999:999`.
- the directories FeatureHub expects:

| Directory | Purpose |
|-----------|---------|
| `/etc/common-config` | Default configuration and `log4j2.xml` baked into each application image |
| `/etc/app-config` | Where you mount `application.properties` and `secrets.properties`. See [Run configuration](https://docs.featurehub.io/featurehub/latest/configuration.html#_run_configuration). |
| `/db` | Data directory, used for the H2 database in evaluation setups |
| `/target` | Working directory for bundled binaries |

The image is published for `linux/amd64` and `linux/arm64`.

## Tags

Tags are base image versions (for example `2.2`), not FeatureHub versions. The version each server build uses is
set by `app.baseimage` in `backend/tile-app/tile.xml` and `backend/tile-app-with-ui/tile.xml`.

## Building

The source is in [`pipeline/build/base_jdk`](https://github.com/featurehub-io/featurehub/tree/main/pipeline/build/base_jdk).
To build and push a multi-architecture image (this requires `docker buildx` and a DockerHub login):

```sh
cd pipeline/build/base_jdk
sh make-dualarch-and-push.sh
```

When you bump the tag, update it in `make-dualarch-and-push.sh` and in the tile `app.baseimage` properties.

## More information

- [FeatureHub documentation](https://docs.featurehub.io)
- [Source code](https://github.com/featurehub-io/featurehub)
