FROM node:18-buster-slim as build

ARG branch=main
# install the latest javascript SDK
WORKDIR /jssdk
RUN apt-get update && apt-get install -y git

RUN git clone https://github.com/featurehub-io/featurehub-javascript-sdk.git
# try and checkout this external named branch or leave on main if not
RUN git checkout $branch || true
RUN cd featurehub-javascript-sdk/featurehub-javascript-client-sdk && \
    npm install && npm run compile && \
    cd ../featurehub-javascript-node-sdk && \
    npm install && npm run link && npm run compile

WORKDIR /app

ADD app /app/app/
ADD features /app/features/
COPY package.json package-lock.json tsconfig.json run.sh /app/
RUN npm install && npm run setup && npm run build
