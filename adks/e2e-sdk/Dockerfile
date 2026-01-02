FROM node:24-bullseye-slim as build

# install the latest javascript SDK
WORKDIR /jssdk
RUN apt-get update && apt-get install -y git && npm install -g pnpm@latest
RUN mkdir -p /pnpm
ENV PNPM_HOME=/pnpm

ARG branch=main
RUN (git clone --depth 1 -b $branch https://github.com/featurehub-io/featurehub-javascript-sdk.git || git clone --depth 1 https://github.com/featurehub-io/featurehub-javascript-sdk.git) && \
     cd featurehub-javascript-sdk && \
     (git checkout $branch || true) && \
    export PATH=$PATH:/pnpm && \
    cd packages/core && pnpm install && pnpm build && pnpm link && \
    cd ../node && pnpm install && pnpm build && pnpm link

WORKDIR /app

ADD app /app/app/
ADD features /app/features/
COPY package.json pnpm*.yaml tsconfig.json run.sh /app/
# need to fix the setup before doing the install otherwise dev based overrides can pollute this
RUN pnpm remove featurehub-javascript-node-sdk pnpm run setup && pnpm install && pnpm run build
