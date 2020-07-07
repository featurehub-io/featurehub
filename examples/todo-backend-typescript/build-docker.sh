#/bin/sh
cp docker-run.sh dist
cp Dockerfile dist
cp default_site dist
cd dist
cp -Rv ../../../sdks/client-typescript-core/dist client-typescript-core
cp -Rv ../../../sdks/client-typescript-eventsource/dist client-typescript-eventsource
docker build -t featurehub/example_node:1.1 .
