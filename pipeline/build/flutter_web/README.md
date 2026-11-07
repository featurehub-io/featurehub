# FeatureHub flutter_web

`featurehub/flutter_web` is the **build image for the FeatureHub Admin web app**, the Flutter admin console that ships
inside the [`mr`](https://hub.docker.com/r/featurehub/mr),
[`party-server`](https://hub.docker.com/r/featurehub/party-server),
[`party-server-ish`](https://hub.docker.com/r/featurehub/party-server-ish) and
[`maintenance-responder`](https://hub.docker.com/r/featurehub/maintenance-responder) images.

It is a build tool, not a runtime image. It lets you build the Admin app without installing Flutter locally, and it
pins the Flutter version so CI and developer builds produce the same output.

To run FeatureHub, see the documentation at **https://docs.featurehub.io**.

## Contents

- Debian (`trixie`) with `git`, `curl`, `wget`, `unzip` and `lcov`
- the **Flutter SDK** (pinned by the `FLUTTER_VERSION` build argument, currently `3.38.5`) at `/opt/flutter`,
  with web support enabled, artifacts precached, and analytics turned off
- the Dart SDK bundled with Flutter, on the `PATH`

## Usage

The FeatureHub build uses this image automatically when `flutter` is not on your `PATH`:

```sh
cd admin-frontend
sh build-frontend.sh <version>      # for example: sh build-frontend.sh 1.11.0
```

This mounts the unpacked frontend sources at `/opt/app` and runs the repository's
[`admin-frontend/build/build.sh`](https://github.com/featurehub-io/featurehub/blob/main/admin-frontend/build/build.sh).
That script builds both the standard and the CanvasKit-embedded (`intranet`) versions of the Admin app. You can do
the same by hand:

```sh
docker run --rm -e BUILD_VERSION=1.11.0 -v $PWD:/opt/app --entrypoint /bin/bash \
  featurehub/flutter_web:1.14 -c "cd /opt/app && ./build.sh"
```

## Tags

Tags are build image versions (for example `1.14`), not FeatureHub or Flutter versions. The tag the build uses is set
in `admin-frontend/build-frontend.sh`.

## Building

The source is in [`pipeline/build/flutter_web`](https://github.com/featurehub-io/featurehub/tree/main/pipeline/build/flutter_web).

```sh
cd pipeline/build/flutter_web
sh make-image.sh
docker push featurehub/flutter_web:<version>
```

To upgrade Flutter, change `FLUTTER_VERSION` in the `Dockerfile`, bump `VERSION` in `make-image.sh`, and update the
tag in `admin-frontend/build-frontend.sh`.

## More information

- [Contributing to FeatureHub](https://docs.featurehub.io/featurehub/latest/developers.html)
- [Source code](https://github.com/featurehub-io/featurehub)
