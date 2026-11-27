#!/bin/sh
# Runs MuslSmoke.java inside the pinned Alpine (musl) Temurin JDK image against the party-server runtime classpath,
# which is a superset of every other server image's native libraries (NATS, Kafka, Pub/Sub, DB drivers).
#
# Requires backend/party-server to have been built (mvn install). Usage:
#   sh pipeline/build/musl-smoke/run.sh [linux/amd64|linux/arm64]
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../../.." && pwd)
PLATFORM=${1:-}
# keep in step with app.baseimage.name in backend/tile-app/tile.xml (same Temurin release, JDK variant so we can
# launch a .java source file directly)
IMAGE=${MUSL_SMOKE_IMAGE:-eclipse-temurin:25-jdk-alpine@sha256:22edbb1021420ae6e248519d1af07577742314d67d4c60fcbb2e285b7e47fa78}
LIB=$(mktemp -d)
trap 'rm -rf "$LIB"' EXIT

(cd "$ROOT/backend/party-server" && mvn -q -B dependency:copy-dependencies -Dgib.disable=true \
  -DincludeScope=runtime -DoutputDirectory="$LIB")
echo "$(ls "$LIB" | wc -l | tr -d ' ') runtime jars"
ls "$LIB" | grep -i snappy

PLATFORM_ARG=""
if [ -n "$PLATFORM" ]; then PLATFORM_ARG="--platform $PLATFORM"; fi

# same JVM flag the images use, see tile-app/tile.xml
docker run --rm $PLATFORM_ARG -v "$LIB:/lib-smoke:ro" -v "$HERE:/smoke:ro" "$IMAGE" \
  java -Dorg.bouncycastle.native.cpu_variant=java -cp '/lib-smoke/*' /smoke/MuslSmoke.java
