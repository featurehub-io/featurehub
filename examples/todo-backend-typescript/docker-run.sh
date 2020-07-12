#!/bin/sh
nginx
echo "{\"baseUrl\": \"http://localhost:5000\", \"sdkUrl\": \"$FEATUREHUB_APP_ENV_URL\", \"gaId\": \"$FEATUREHUB_APP_GA_ID\", \"gaCid\": \"$FEATUREHUB_APP_GA_CID\"}" > /var/www/html/todo-frontend/featurehub-config.json
cd /app && node app
