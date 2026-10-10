FROM node:24-alpine as build

# install the latest javascript SDK
WORKDIR /jssdk
RUN apk update && apk add --no-cache git wget yq bash
RUN npm install -g pnpm@11

RUN mkdir -p /pnpm
ENV PNPM_HOME=/pnpm
ENV PATH=$PATH:/pnpm:/pnpm/bin
ARG branch=main
# we pull and build the js-sdk on this branch (or main if there isn't one) so we can link it in below
RUN (git clone --depth 1 -b $branch https://github.com/featurehub-io/featurehub-javascript-sdk.git || git clone --depth 1 https://github.com/featurehub-io/featurehub-javascript-sdk.git) && \
     cd featurehub-javascript-sdk && \
     (git checkout $branch || true) && \
    cd packages/core && pnpm install && pnpm run build && \
    cd ../node && pnpm install && pnpm run build

WORKDIR /app

ADD app /app/app/
ADD features /app/features/
# don't include workspace as it will bring in the lock
COPY package.json pnpm-lock.yaml tsconfig.json run.sh /app/
ENV PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/pnpm/bin
# need to fix the setup before doing the install otherwise dev based overrides can pollute this
RUN ( (pnpm remove featurehub-javascript-node-sdk featurehub-javascript-core-sdk && yq -i 'del(.overrides)' pnpm-lock.yaml) || true) && \
    pnpm link /jssdk/featurehub-javascript-sdk/packages/node && \
    pnpm install --frozen-lockfile && pnpm run build
