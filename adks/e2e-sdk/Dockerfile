FROM node:18-buster-slim as build

WORKDIR /app
ADD app /app/app/
ADD features /app/features/
COPY package.json package-lock.json tsconfig.json run.sh /app/
RUN npm install && npm run build
