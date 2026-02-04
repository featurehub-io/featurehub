FROM --platform=$BUILDPLATFORM nats:2.12.3-alpine AS builder

WORKDIR /workdir
RUN cp /usr/local/bin/nats-server /workdir && cp /etc/nats/nats-server.conf /workdir

FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jre-alpine

# make the non-root user & make appropriate directories
RUN  (delgroup ping | true) && adduser -g 999 -u 999 -D k8suser && mkdir -p /db /target /etc/app-config /etc/common-config

# copy in nats
COPY --from=builder /workdir/nats-server /target
COPY --from=builder /workdir/nats-server.conf /etc/nats/nats-server.conf

# now for nginx
RUN chown -R 999:999 /target /etc/nats/nats-server.conf /db /etc/app-config /etc/common-config

EXPOSE 8085
EXPOSE 8701



