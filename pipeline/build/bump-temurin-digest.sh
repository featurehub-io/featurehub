#!/bin/sh
# Updates the pinned eclipse-temurin multi-arch index digests used by every FeatureHub server image
# (backend/tile-app, backend/tile-app-with-ui) and by the musl smoke test (JDK variant of the same release).
# Requires docker buildx. Prints the new JRE digest, or nothing changes if they are already current.
set -e
ROOT=$(cd "$(dirname "$0")/../.." && pwd)

digest() {
  docker buildx imagetools inspect "$1" --format '{{.Manifest.Digest}}'
}

JRE=$(digest eclipse-temurin:25-jre-alpine)
JDK=$(digest eclipse-temurin:25-jdk-alpine)
[ -n "$JRE" ] && [ -n "$JDK" ] || { echo "could not resolve digests" >&2; exit 1; }

for tile in "$ROOT/backend/tile-app/tile.xml" "$ROOT/backend/tile-app-with-ui/tile.xml"; do
  sed -i.bak "s#eclipse-temurin:25-jre-alpine@sha256:[a-f0-9]*#eclipse-temurin:25-jre-alpine@$JRE#" "$tile" && rm "$tile.bak"
done
SMOKE="$ROOT/pipeline/build/musl-smoke/run.sh"
sed -i.bak "s#eclipse-temurin:25-jdk-alpine@sha256:[a-f0-9]*#eclipse-temurin:25-jdk-alpine@$JDK#" "$SMOKE" && rm "$SMOKE.bak"

echo "$JRE"
