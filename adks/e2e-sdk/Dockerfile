FROM node:18-buster-slim as build

# install the latest javascript SDK
WORKDIR /jssdk
RUN apt-get update && apt-get install -y git

ARG branch=main
RUN git clone https://github.com/featurehub-io/featurehub-javascript-sdk.git && \
     cd featurehub-javascript-sdk && \
     (git checkout $branch || true) && \
    cd featurehub-javascript-client-sdk && \
    npm install && npm run compile && \
    cd ../featurehub-javascript-node-sdk && \
    npm install && npm run setup && npm run compile

WORKDIR /app

ADD app /app/app/
ADD features /app/features/
COPY package.json package-lock.json tsconfig.json run.sh /app/
RUN npm install && npm run setup && npm run build
