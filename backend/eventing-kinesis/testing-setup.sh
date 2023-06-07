#!/bin/sh
docker run -d --name kinesis-localstack -e SERVICES=kinesis,dynamodb,cloudwatch -p 4566:4566 -e KINESIS_PROVIDER=kinesis-mock -e KINESIS_INITIALIZE_STREAMS=featurehub-mr-edge,featurehub-stats,featurehub-mr-dacha2,featurehub-edge-updates,featurehub-enriched-events,featurehub-messaging-stream localstack/localstack:1.1
