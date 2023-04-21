FROM node:18-buster-slim as build

WORKDIR /app
COPY ./app /app/app/
COPY package.json package-lock.json tsconfig.json /app/
RUN npm install && npm run build
