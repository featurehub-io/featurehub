FROM node:24-bullseye-slim as build

# install the latest javascript SDK
WORKDIR /jssdk
RUN apt-get update && apt-get install -y git wget &&  \
    npm install -g pnpm@latest && \
    wget -qO /usr/local/bin/yq https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 && \
    chmod a+x /usr/local/bin/yq

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
# don't include workspace as it will bring in the lock
COPY package.json pnpm-lock.yaml tsconfig.json run.sh /app/
# need to fix the setup before doing the install otherwise dev based overrides can pollute this
RUN pnpm remove featurehub-javascript-node-sdk featurehub-javascript-core-sdk && yq -i 'del(.overrides)' pnpm-lock.yaml && \
    pnpm add featurehub-javascript-node-sdk && \
    pnpm run setup && pnpm install --frozen-lockfile && pnpm run build
