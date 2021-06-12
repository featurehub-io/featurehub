FROM nats:2.1.7-alpine AS builder

WORKDIR /workdir
RUN cp /usr/local/bin/nats-server /workdir && cp /etc/nats/nats-server.conf /workdir

FROM adoptopenjdk:11-jre-hotspot-focal

# make appropriate directories
RUN mkdir -p /db /target /etc/app-config /etc/common-config

# copy in nats
COPY --from=builder /workdir/nats-server /target
COPY --from=builder /workdir/nats-server.conf /etc/nats/nats-server.conf

# now this party server
RUN groupadd -g 999 k8sgroup && useradd -g 999 -u 999 k8suser
COPY party.properties /etc/common-config/common.properties
COPY log4j2.xml /etc/common-config/log4j2.xml

# now for nginx
RUN chown -R 999:999 /target /etc/nats/nats-server.conf /db /etc/app-config /etc/common-config
RUN apt-get update && apt-get install -y nginx && mkdir -p /var/lib/nginx/body /var/lib/nginx/proxy /var/lib/nginx/fastcgi
RUN chown -R www-data:www-data /var/lib/nginx/body /var/lib/nginx/proxy /var/lib/nginx /var/log/nginx /usr/share/nginx  /var/lib/nginx/fastcgi
COPY default_site /etc/nginx/sites-enabled/default
COPY nginx.conf /etc/nginx
RUN chown -R 999:999 /etc/nginx /var/lib/nginx /var/log/nginx /var/www/html

EXPOSE 8085
EXPOSE 8701



