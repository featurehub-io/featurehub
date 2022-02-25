FROM --platform=$BUILDPLATFORM nats:2.6.4-alpine AS builder

WORKDIR /workdir
RUN cp /usr/local/bin/nats-server /workdir && cp /etc/nats/nats-server.conf /workdir

FROM --platform=$BUILDPLATFORM adoptopenjdk:11-jre-hotspot-focal

# make the non-root user & make appropriate directories
RUN groupadd -g 999 k8sgroup && useradd -g 999 -u 999 k8suser && mkdir -p /db /target /etc/app-config /etc/common-config

# copy in nats
COPY --from=builder /workdir/nats-server /target
COPY --from=builder /workdir/nats-server.conf /etc/nats/nats-server.conf

# now for nginx
RUN chown -R 999:999 /target /etc/nats/nats-server.conf /db /etc/app-config /etc/common-config

EXPOSE 8085
EXPOSE 8701



