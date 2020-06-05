FROM nats:2.1.7-alpine AS builder

WORKDIR /workdir
RUN cp /usr/local/bin/nats-server /workdir && cp /etc/nats/nats-server.conf /workdir

FROM adoptopenjdk:11-jre-hotspot-bionic

RUN mkdir /db && mkdir /target && mkdir /etc/app-config && mkdir /etc/common-config
COPY --from=builder /workdir/nats-server /target
COPY --from=builder /workdir/nats-server.conf /etc/nats/nats-server.conf
COPY party.properties /etc/common-config/common.properties
COPY log4j2.xml /etc/common-config/log4j2.xml
RUN apt-get update && apt-get install -y nginx
COPY default_site /etc/nginx/sites-enabled/default





